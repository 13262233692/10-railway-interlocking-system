package com.railway.interlocking.service;

import com.railway.interlocking.dto.WebSocketMessage;
import com.railway.interlocking.dto.request.*;
import com.railway.interlocking.dto.response.InterlockingStatusResponse;
import com.railway.interlocking.dto.response.RouteOperationResponse;
import com.railway.interlocking.model.*;
import com.railway.interlocking.model.enums.*;
import com.railway.interlocking.statemachine.InterlockingStateMachine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InterlockingService {

    private final InterlockingStateMachine stateMachine;
    private final WebSocketService webSocketService;
    private final ResourceLockManager resourceLockManager;
    private final TransactionCoordinator transactionCoordinator;

    private static final String STATION_ID = "STATION_001";
    private static final String STATION_NAME = "高铁南站";

    private final Map<String, TrackSection> trackSections = new ConcurrentHashMap<>();
    private final Map<String, Switch> switches = new ConcurrentHashMap<>();
    private final Map<String, Signal> signals = new ConcurrentHashMap<>();
    private final Map<String, Route> routes = new ConcurrentHashMap<>();
    private final Map<String, RouteLock> routeLocks = new ConcurrentHashMap<>();

    private final Map<String, String> routeResourceReservations = new ConcurrentHashMap<>();

    public InterlockingService(InterlockingStateMachine stateMachine,
                                WebSocketService webSocketService,
                                ResourceLockManager resourceLockManager,
                                TransactionCoordinator transactionCoordinator) {
        this.stateMachine = stateMachine;
        this.webSocketService = webSocketService;
        this.resourceLockManager = resourceLockManager;
        this.transactionCoordinator = transactionCoordinator;
    }

    public void initializeStationData(List<TrackSection> sections,
                                       List<Switch> switchesList,
                                       List<Signal> signalsList,
                                       List<Route> routesList) {
        log.info("开始初始化站场数据...");

        sections.forEach(section -> trackSections.put(section.getId(), section));
        switchesList.forEach(sw -> switches.put(sw.getId(), sw));
        signalsList.forEach(signal -> signals.put(signal.getId(), signal));
        routesList.forEach(route -> routes.put(route.getId(), route));

        log.info("站场数据初始化完成: 轨道区段={}, 道岔={}, 信号机={}, 进路={}",
                trackSections.size(), switches.size(), signals.size(), routes.size());

        broadcastStatusUpdate();
    }

    public RouteOperationResponse establishRoute(RouteEstablishRequest request) {
        String routeId = request.getRouteId();
        String operator = request.getOperator();
        log.info("开始办理进路(事务模式): routeId={}, operator={}, thread={}", routeId, operator, Thread.currentThread().getName());

        Route route = routes.get(routeId);
        if (route == null) {
            return createErrorResponse(routeId, "办理", "进路不存在", "ROUTE_NOT_FOUND");
        }

        if (!route.canEstablish()) {
            return createErrorResponse(routeId, "办理",
                    "进路当前状态不允许办理: " + route.getStatus(), "INVALID_STATUS");
        }

        TransactionCoordinator.TransactionContext txCtx = null;

        try {
            txCtx = transactionCoordinator.beginTransaction(routeId, route.getName(), operator);

            stateMachine.transition(route, InterlockingStateMachine.StateEvent.ESTABLISH);
            transactionCoordinator.pushCompensation(txCtx, () -> {
                if (route.getStatus() == RouteStatus.LOCKING) {
                    try {
                        stateMachine.transition(route, InterlockingStateMachine.StateEvent.UNLOCK);
                    } catch (Exception e) {
                        route.setStatus(RouteStatus.NOT_ESTABLISHED);
                    }
                }
                log.info("补偿: 回退进路状态, routeId={}", routeId);
            });

            List<String> allResourceIds = collectAllResourceIds(route);
            boolean prepared = transactionCoordinator.prepareResources(txCtx, route, trackSections, switches, signals);
            if (!prepared) {
                transactionCoordinator.rollback(txCtx);
                return createErrorResponse(routeId, "办理",
                        "资源锁定失败，存在并发冲突，请稍后重试", "RESOURCE_LOCK_CONFLICT");
            }

            List<String> sectionCheckResult = checkSectionsAvailable(route);
            if (!sectionCheckResult.isEmpty()) {
                String errorMsg = "轨道区段不可用: " + String.join(", ", sectionCheckResult);
                transactionCoordinator.rollback(txCtx);
                handleRouteEstablishFailure(route);
                return createErrorResponse(routeId, "办理", errorMsg, "SECTION_NOT_AVAILABLE");
            }

            List<String> switchCheckResult = checkAndConvertSwitches(route);
            if (!switchCheckResult.isEmpty()) {
                String errorMsg = "道岔位置不正确且无法转换: " + String.join(", ", switchCheckResult);
                transactionCoordinator.rollback(txCtx);
                handleRouteEstablishFailure(route);
                return createErrorResponse(routeId, "办理", errorMsg, "SWITCH_POSITION_ERROR");
            }

            List<String> conflictCheckResult = checkConflictingRoutes(route);
            if (!conflictCheckResult.isEmpty()) {
                String errorMsg = "冲突进路已办理: " + String.join(", ", conflictCheckResult);
                transactionCoordinator.rollback(txCtx);
                handleRouteEstablishFailure(route);
                return createErrorResponse(routeId, "办理", errorMsg, "CONFLICTING_ROUTE");
            }

            routeResourceReservations.put(routeId, String.join(",", allResourceIds));

            RouteLock routeLock = createRouteLock(route, operator);
            transactionCoordinator.pushCompensation(txCtx, () -> {
                routeLock.setActive(false);
                routeLock.setLockStatus("已回滚");
                routeLocks.remove(routeLock.getId());
                log.info("补偿: 移除进路锁闭, routeId={}", routeId);
            });

            lockSections(route, routeLock);
            transactionCoordinator.pushCompensation(txCtx, () -> {
                for (String sectionId : route.getSectionIds()) {
                    TrackSection section = trackSections.get(sectionId);
                    if (section != null && routeId.equals(section.getLockedByRouteId())) {
                        section.setStatus(TrackSectionStatus.IDLE);
                        section.setLockedByRouteId(null);
                    }
                }
                log.info("补偿: 解锁轨道区段, routeId={}", routeId);
            });

            lockSwitches(route, routeLock);
            transactionCoordinator.pushCompensation(txCtx, () -> {
                for (String switchId : route.getSwitchIds()) {
                    Switch sw = switches.get(switchId);
                    if (sw != null && routeId.equals(sw.getLockedByRouteId())) {
                        sw.setLocked(false);
                        sw.setLockedByRouteId(null);
                    }
                }
                log.info("补偿: 解锁道岔, routeId={}", routeId);
            });

            lockHostileSignals(route, routeLock);
            transactionCoordinator.pushCompensation(txCtx, () -> {
                for (String signalId : route.getHostileSignalIds()) {
                    Signal signal = signals.get(signalId);
                    if (signal != null) {
                        signal.setLocked(false);
                    }
                }
                log.info("补偿: 解锁敌对信号机, routeId={}", routeId);
            });

            stateMachine.transition(route, InterlockingStateMachine.StateEvent.LOCK);
            route.setLockedTime(LocalDateTime.now());
            route.setOperator(operator);
            route.setGuidanceRoute(request.isGuidanceRoute());

            SignalAspect aspect = calculateSignalAspect(route);
            openStartSignal(route, aspect);
            transactionCoordinator.pushCompensation(txCtx, () -> {
                closeStartSignal(route);
                log.info("补偿: 关闭起始信号机, routeId={}", routeId);
            });

            stateMachine.transition(route, InterlockingStateMachine.StateEvent.CLEAR);
            route.setClearedTime(LocalDateTime.now());

            transactionCoordinator.commit(txCtx);

            RouteOperationResponse response = createSuccessResponse(route, "办理", routeLock);
            response.setMessage("进路办理成功，信号已开放");

            log.info("进路办理成功(事务已提交): routeId={}, routeName={}", route.getId(), route.getName());

            broadcastRouteUpdate(route, "进路办理成功", "SUCCESS");
            broadcastStatusUpdate();

            return response;

        } catch (Exception e) {
            log.error("办理进路异常(事务将回滚): routeId={}, error={}", routeId, e.getMessage(), e);
            if (txCtx != null) {
                transactionCoordinator.rollback(txCtx);
            }
            handleRouteEstablishFailure(route);
            return createErrorResponse(routeId, "办理",
                    "办理进路异常: " + e.getMessage(), "SYSTEM_ERROR");
        } finally {
            releaseAllResourceLocks(route);
        }
    }

    public RouteOperationResponse cancelRoute(RouteCancelRequest request) {
        String routeId = request.getRouteId();
        String operator = request.getOperator();
        log.info("开始取消进路(事务模式): routeId={}, operator={}", routeId, operator);

        Route route = routes.get(routeId);
        if (route == null) {
            return createErrorResponse(routeId, "取消", "进路不存在", "ROUTE_NOT_FOUND");
        }

        if (!route.canCancel()) {
            return createErrorResponse(routeId, "取消",
                    "进路当前状态不允许取消: " + route.getStatus(), "INVALID_STATUS");
        }

        if (route.isApproachLocked() && !request.isForceCancel()) {
            return createErrorResponse(routeId, "取消",
                    "进路处于接近锁闭状态，需要强制取消", "APPROACH_LOCKED");
        }

        TransactionCoordinator.TransactionContext txCtx = null;

        try {
            txCtx = transactionCoordinator.beginTransaction(routeId, route.getName(), operator);

            List<String> allResourceIds = collectAllResourceIds(route);
            boolean prepared = transactionCoordinator.prepareResources(txCtx, route, trackSections, switches, signals);
            if (!prepared) {
                transactionCoordinator.rollback(txCtx);
                return createErrorResponse(routeId, "取消",
                        "资源锁定失败，存在并发操作，请稍后重试", "RESOURCE_LOCK_CONFLICT");
            }

            stateMachine.transition(route, InterlockingStateMachine.StateEvent.CANCEL);

            closeStartSignal(route);
            unlockRoute(route, request.getReason(), request.getOperator());

            if (route.getStatus() == RouteStatus.CANCELLING) {
                stateMachine.transition(route, InterlockingStateMachine.StateEvent.UNLOCK);
            }
            route.setUnlockedTime(LocalDateTime.now());

            routeResourceReservations.remove(routeId);

            transactionCoordinator.commit(txCtx);

            RouteOperationResponse response = RouteOperationResponse.builder()
                    .routeId(route.getId())
                    .routeNumber(route.getNumber())
                    .routeName(route.getName())
                    .operationType("取消")
                    .success(true)
                    .message("进路取消成功")
                    .currentStatus(route.getStatus())
                    .operationTime(LocalDateTime.now())
                    .build();

            log.info("进路取消成功(事务已提交): routeId={}, routeName={}", route.getId(), route.getName());

            broadcastRouteUpdate(route, "进路已取消", "INFO");
            broadcastStatusUpdate();

            return response;

        } catch (Exception e) {
            log.error("取消进路异常(事务将回滚): routeId={}", routeId, e);
            if (txCtx != null) {
                transactionCoordinator.rollback(txCtx);
            }
            return createErrorResponse(routeId, "取消",
                    "取消进路异常: " + e.getMessage(), "SYSTEM_ERROR");
        } finally {
            releaseAllResourceLocks(route);
        }
    }

    public RouteOperationResponse operateSwitch(SwitchOperateRequest request) {
        log.info("开始操作道岔: switchId={}, targetPosition={}, operator={}",
                request.getSwitchId(), request.getTargetPosition(), request.getOperator());

        Switch sw = switches.get(request.getSwitchId());
        if (sw == null) {
            return createErrorResponse(request.getSwitchId(), "道岔操作", "道岔不存在", "SWITCH_NOT_FOUND");
        }

        if (sw.isLocked() && !request.isForceOperate()) {
            return createErrorResponse(request.getSwitchId(), "道岔操作",
                    "道岔已被进路锁闭，无法操作", "SWITCH_LOCKED");
        }

        if (sw.getPosition() == request.getTargetPosition()) {
            return RouteOperationResponse.builder()
                    .routeId(sw.getId())
                    .routeName(sw.getName())
                    .operationType("道岔操作")
                    .success(true)
                    .message("道岔已在目标位置")
                    .operationTime(LocalDateTime.now())
                    .build();
        }

        try {
            if (!request.isForceOperate()) {
                List<String> blockingRoutes = checkSwitchBlockingRoutes(sw);
                if (!blockingRoutes.isEmpty()) {
                    return createErrorResponse(request.getSwitchId(), "道岔操作",
                            "存在经过该道岔的已办理进路: " + String.join(", ", blockingRoutes),
                            "BLOCKED_BY_ROUTE");
                }
            }

            stateMachine.transitionSwitch(sw, request.getTargetPosition(),
                    "人工操作: " + request.getOperator());

            RouteOperationResponse response = RouteOperationResponse.builder()
                    .routeId(sw.getId())
                    .routeName(sw.getName())
                    .operationType("道岔操作")
                    .success(true)
                    .message("道岔操作成功，当前位置: " + sw.getPosition().getDescription())
                    .operationTime(LocalDateTime.now())
                    .build();

            log.info("道岔操作成功: switchId={}, position={}", sw.getId(), sw.getPosition());

            broadcastSwitchUpdate(sw);
            broadcastStatusUpdate();

            return response;

        } catch (Exception e) {
            log.error("操作道岔异常: switchId={}", request.getSwitchId(), e);
            return createErrorResponse(request.getSwitchId(), "道岔操作",
                    "道岔操作异常: " + e.getMessage(), "SYSTEM_ERROR");
        }
    }

    public RouteOperationResponse controlSignal(SignalControlRequest request) {
        log.info("开始控制信号机: signalId={}, targetAspect={}, operator={}",
                request.getSignalId(), request.getTargetAspect(), request.getOperator());

        Signal signal = signals.get(request.getSignalId());
        if (signal == null) {
            return createErrorResponse(request.getSignalId(), "信号控制", "信号机不存在", "SIGNAL_NOT_FOUND");
        }

        if (signal.isLocked() && !request.isForceControl()
                && request.getTargetAspect() != SignalAspect.RED) {
            return createErrorResponse(request.getSignalId(), "信号控制",
                    "信号机已被锁闭，无法开放", "SIGNAL_LOCKED");
        }

        try {
            stateMachine.transitionSignal(signal, request.getTargetAspect(),
                    "人工控制: " + request.getOperator());

            RouteOperationResponse response = RouteOperationResponse.builder()
                    .routeId(signal.getId())
                    .routeName(signal.getName())
                    .operationType("信号控制")
                    .success(true)
                    .message("信号机控制成功，当前显示: " + signal.getAspect().getDescription())
                    .operationTime(LocalDateTime.now())
                    .build();

            log.info("信号机控制成功: signalId={}, aspect={}", signal.getId(), signal.getAspect());

            broadcastSignalUpdate(signal);
            broadcastStatusUpdate();

            return response;

        } catch (Exception e) {
            log.error("控制信号机异常: signalId={}", request.getSignalId(), e);
            return createErrorResponse(request.getSignalId(), "信号控制",
                    "信号控制异常: " + e.getMessage(), "SYSTEM_ERROR");
        }
    }

    public RouteOperationResponse occupySection(SectionOccupyRequest request) {
        log.info("开始占用轨道区段: sectionId={}, trainId={}", request.getSectionId(), request.getTrainId());

        TrackSection section = trackSections.get(request.getSectionId());
        if (section == null) {
            return createErrorResponse(request.getSectionId(), "区段占用", "轨道区段不存在", "SECTION_NOT_FOUND");
        }

        if (section.isOccupied()) {
            return createErrorResponse(request.getSectionId(), "区段占用",
                    "轨道区段已被占用: " + section.getOccupiedByTrainId(), "SECTION_OCCUPIED");
        }

        if (section.isFault()) {
            return createErrorResponse(request.getSectionId(), "区段占用",
                    "轨道区段故障，无法占用", "SECTION_FAULT");
        }

        try {
            stateMachine.transitionSection(section, TrackSectionStatus.OCCUPIED,
                    "列车占用: " + request.getTrainId());
            section.setOccupiedByTrainId(request.getTrainId());

            List<Route> affectedRoutes = findRoutesContainingSection(section.getId());
            for (Route route : affectedRoutes) {
                if (route.isCleared() || route.isLocked()) {
                    if (isFirstSectionOfRoute(route, section.getId())) {
                        stateMachine.transition(route, InterlockingStateMachine.StateEvent.OCCUPY);
                        route.setOccupiedByTrainId(request.getTrainId());
                        route.setOccupiedTime(LocalDateTime.now());
                        closeStartSignal(route);
                        log.info("列车进入进路: routeId={}, trainId={}", route.getId(), request.getTrainId());
                    }
                }
            }

            checkAndUnlockPassedSections(request.getTrainId());

            RouteOperationResponse response = RouteOperationResponse.builder()
                    .routeId(section.getId())
                    .routeName(section.getName())
                    .operationType("区段占用")
                    .success(true)
                    .message("轨道区段占用成功，列车ID: " + request.getTrainId())
                    .operationTime(LocalDateTime.now())
                    .build();

            log.info("轨道区段占用成功: sectionId={}, trainId={}", section.getId(), request.getTrainId());

            broadcastSectionUpdate(section);
            broadcastStatusUpdate();

            return response;

        } catch (Exception e) {
            log.error("占用轨道区段异常: sectionId={}", request.getSectionId(), e);
            return createErrorResponse(request.getSectionId(), "区段占用",
                    "区段占用异常: " + e.getMessage(), "SYSTEM_ERROR");
        }
    }

    public RouteOperationResponse releaseSection(String sectionId, String operator) {
        log.info("开始释放轨道区段: sectionId={}, operator={}", sectionId, operator);

        TrackSection section = trackSections.get(sectionId);
        if (section == null) {
            return createErrorResponse(sectionId, "区段释放", "轨道区段不存在", "SECTION_NOT_FOUND");
        }

        if (!section.isOccupied()) {
            return RouteOperationResponse.builder()
                    .routeId(section.getId())
                    .routeName(section.getName())
                    .operationType("区段释放")
                    .success(true)
                    .message("轨道区段未被占用")
                    .operationTime(LocalDateTime.now())
                    .build();
        }

        try {
            String trainId = section.getOccupiedByTrainId();

            if (section.isLocked()) {
                RouteLock lock = routeLocks.get(section.getLockedByRouteId());
                if (lock != null && lock.isActive()) {
                    stateMachine.transitionSection(section, TrackSectionStatus.LOCKED,
                            "列车出清，区段仍锁闭");
                }
            } else {
                stateMachine.transitionSection(section, TrackSectionStatus.IDLE,
                        "列车出清: " + trainId);
            }

            section.setOccupiedByTrainId(null);

            List<Route> affectedRoutes = findRoutesContainingSection(sectionId);
            for (Route route : affectedRoutes) {
                if (route.isOccupied() || route.isLocked() || route.isCleared()) {
                    boolean allSectionsReleased = areAllSectionsOfRouteReleased(route);
                    if (allSectionsReleased) {
                        autoUnlockRoute(route, trainId);
                    } else if (isLastSectionOfRoute(route, sectionId)) {
                        autoUnlockRoute(route, trainId);
                    }
                }
            }

            RouteOperationResponse response = RouteOperationResponse.builder()
                    .routeId(section.getId())
                    .routeName(section.getName())
                    .operationType("区段释放")
                    .success(true)
                    .message("轨道区段释放成功")
                    .operationTime(LocalDateTime.now())
                    .build();

            log.info("轨道区段释放成功: sectionId={}", sectionId);

            broadcastSectionUpdate(section);
            broadcastStatusUpdate();

            return response;

        } catch (Exception e) {
            log.error("释放轨道区段异常: sectionId={}", sectionId, e);
            return createErrorResponse(sectionId, "区段释放",
                    "区段释放异常: " + e.getMessage(), "SYSTEM_ERROR");
        }
    }

    public InterlockingStatusResponse getInterlockingStatus() {
        List<TrackSection> sectionList = new ArrayList<>(trackSections.values());
        List<Switch> switchList = new ArrayList<>(switches.values());
        List<Signal> signalList = new ArrayList<>(signals.values());
        List<Route> routeList = new ArrayList<>(routes.values());
        List<RouteLock> lockList = routeLocks.values().stream()
                .filter(RouteLock::isActive)
                .collect(Collectors.toList());

        int idleCount = (int) sectionList.stream().filter(TrackSection::isAvailable).count();
        int occupiedCount = (int) sectionList.stream().filter(TrackSection::isOccupied).count();
        int lockedCount = (int) sectionList.stream().filter(TrackSection::isLocked).count();
        int lockedRouteCount = (int) routeList.stream().filter(Route::isLocked).count();
        int clearedRouteCount = (int) routeList.stream().filter(Route::isCleared).count();
        int faultCount = (int) sectionList.stream().filter(TrackSection::isFault).count()
                + (int) switchList.stream().filter(s -> s.getPosition() == SwitchPosition.FAULT).count()
                + (int) signalList.stream().filter(s -> s.getAspect() == SignalAspect.OFF).count();

        String systemStatus = "正常";
        if (faultCount > 0) {
            systemStatus = faultCount > 5 ? "故障" : "降级";
        }

        return InterlockingStatusResponse.builder()
                .stationId(STATION_ID)
                .stationName(STATION_NAME)
                .systemStatus(systemStatus)
                .trackSections(sectionList)
                .switches(switchList)
                .signals(signalList)
                .routes(routeList)
                .routeLocks(lockList)
                .idleSectionCount(idleCount)
                .occupiedSectionCount(occupiedCount)
                .lockedSectionCount(lockedCount)
                .lockedRouteCount(lockedRouteCount)
                .clearedRouteCount(clearedRouteCount)
                .faultDeviceCount(faultCount)
                .updateTime(LocalDateTime.now())
                .build();
    }

    public List<Map<String, Object>> getTransactionStatus() {
        return transactionCoordinator.getActiveTransactionStatus();
    }

    public Map<String, Object> getResourceLockStatus() {
        return resourceLockManager.getLockStatus();
    }

    private List<String> collectAllResourceIds(Route route) {
        List<String> allResourceIds = new ArrayList<>();
        allResourceIds.addAll(route.getSectionIds());
        allResourceIds.addAll(route.getSwitchIds());
        allResourceIds.add(route.getStartSignalId());
        if (route.getHostileSignalIds() != null) {
            allResourceIds.addAll(route.getHostileSignalIds());
        }
        return allResourceIds;
    }

    private void releaseAllResourceLocks(Route route) {
        try {
            List<String> allResourceIds = collectAllResourceIds(route);
            resourceLockManager.unlockAll(allResourceIds, route.getId());
        } catch (Exception e) {
            log.error("释放资源锁异常: routeId={}", route.getId(), e);
            resourceLockManager.unlockHeldLocks(route.getId());
        }
    }

    private List<String> checkSectionsAvailable(Route route) {
        List<String> unavailableSections = new ArrayList<>();
        for (String sectionId : route.getSectionIds()) {
            TrackSection section = trackSections.get(sectionId);
            if (section == null) {
                unavailableSections.add(sectionId + "(不存在)");
            } else if (!section.isAvailable()) {
                unavailableSections.add(section.getName() + "(" + section.getStatus().getDescription() + ")");
            }
        }
        return unavailableSections;
    }

    private List<String> checkAndConvertSwitches(Route route) {
        List<String> failedSwitches = new ArrayList<>();

        for (Map.Entry<String, SwitchPosition> entry : route.getSwitchPositions().entrySet()) {
            String switchId = entry.getKey();
            SwitchPosition requiredPosition = entry.getValue();

            Switch sw = switches.get(switchId);
            if (sw == null) {
                failedSwitches.add(switchId + "(不存在)");
                continue;
            }

            if (sw.isPositionCorrect(requiredPosition)) {
                continue;
            }

            if (sw.canOperate()) {
                try {
                    stateMachine.transitionSwitch(sw, requiredPosition,
                            "进路[" + route.getName() + "]办理自动转换");
                } catch (Exception e) {
                    failedSwitches.add(sw.getName() + "(转换失败: " + e.getMessage() + ")");
                }
            } else {
                failedSwitches.add(sw.getName() + "(无法操作: " + sw.getPosition() + ")");
            }
        }

        return failedSwitches;
    }

    private List<String> checkConflictingRoutes(Route route) {
        List<String> conflictingRoutes = new ArrayList<>();

        for (String conflictingRouteId : route.getConflictingRouteIds()) {
            Route conflictingRoute = routes.get(conflictingRouteId);
            if (conflictingRoute != null && conflictingRoute.isLocked()) {
                conflictingRoutes.add(conflictingRoute.getName() + "(" + conflictingRoute.getStatus() + ")");
            }
        }

        for (String sectionId : route.getSectionIds()) {
            TrackSection section = trackSections.get(sectionId);
            if (section != null && section.isLocked()) {
                String lockedByRouteId = section.getLockedByRouteId();
                if (lockedByRouteId != null && !route.getId().equals(lockedByRouteId)) {
                    Route lockingRoute = routes.get(lockedByRouteId);
                    if (lockingRoute != null) {
                        String conflictInfo = lockingRoute.getName() + "(区段锁闭: " + section.getName() + ")";
                        if (!conflictingRoutes.contains(conflictInfo)) {
                            conflictingRoutes.add(conflictInfo);
                        }
                    }
                }
            }
        }

        return conflictingRoutes;
    }

    private List<String> checkSwitchBlockingRoutes(Switch sw) {
        List<String> blockingRoutes = new ArrayList<>();
        for (Route route : routes.values()) {
            if (route.isLocked() && route.getSwitchIds().contains(sw.getId())) {
                blockingRoutes.add(route.getName());
            }
        }
        return blockingRoutes;
    }

    private RouteLock createRouteLock(Route route, String operator) {
        RouteLock routeLock = RouteLock.builder()
                .id("LOCK_" + route.getId() + "_" + System.currentTimeMillis())
                .routeId(route.getId())
                .routeNumber(route.getNumber())
                .routeName(route.getName())
                .stationId(STATION_ID)
                .lockType("进路锁闭")
                .lockLevel(route.isGuidanceRoute() ? "引导锁闭" : "正常锁闭")
                .lockStatus("已锁闭")
                .lockedSectionIds(new ArrayList<>(route.getSectionIds()))
                .lockedSwitchPositions(new HashMap<>(route.getSwitchPositions()))
                .lockedSwitchIds(new ArrayList<>(route.getSwitchIds()))
                .lockedSignalIds(new ArrayList<>(route.getHostileSignalIds()))
                .blockedConflictingRouteIds(new ArrayList<>(route.getConflictingRouteIds()))
                .lockTime(LocalDateTime.now())
                .lastUpdateTime(LocalDateTime.now())
                .active(true)
                .temporary(false)
                .lockReason("进路办理")
                .operator(operator)
                .lockProgress(100)
                .build();

        routeLocks.put(routeLock.getId(), routeLock);
        return routeLock;
    }

    private void lockSections(Route route, RouteLock routeLock) {
        for (String sectionId : route.getSectionIds()) {
            TrackSection section = trackSections.get(sectionId);
            if (section != null) {
                stateMachine.transitionSection(section, TrackSectionStatus.LOCKED,
                        "进路[" + route.getName() + "]锁闭");
                section.setLockedByRouteId(route.getId());
            }
        }
    }

    private void lockSwitches(Route route, RouteLock routeLock) {
        for (String switchId : route.getSwitchIds()) {
            Switch sw = switches.get(switchId);
            if (sw != null) {
                sw.setLocked(true);
                sw.setLockedByRouteId(route.getId());
                sw.setLastUpdateTime(LocalDateTime.now());
            }
        }
    }

    private void lockHostileSignals(Route route, RouteLock routeLock) {
        for (String signalId : route.getHostileSignalIds()) {
            Signal signal = signals.get(signalId);
            if (signal != null) {
                signal.setLocked(true);
                signal.setLastUpdateTime(LocalDateTime.now());
                if (signal.getAspect() != SignalAspect.RED) {
                    stateMachine.transitionSignal(signal, SignalAspect.RED,
                            "进路[" + route.getName() + "]锁闭敌对信号");
                }
            }
        }
    }

    private SignalAspect calculateSignalAspect(Route route) {
        if (route.isGuidanceRoute()) {
            return SignalAspect.DOUBLE_YELLOW;
        }

        if (route.getRouteType() == RouteType.SHUNTING) {
            return SignalAspect.YELLOW;
        }

        int freeBlocks = countFreeBlocksAhead(route);
        if (freeBlocks >= 2) {
            return SignalAspect.GREEN;
        } else if (freeBlocks >= 1) {
            return SignalAspect.YELLOW;
        } else {
            return SignalAspect.YELLOW;
        }
    }

    private int countFreeBlocksAhead(Route route) {
        int freeCount = 0;
        for (String sectionId : route.getSectionIds()) {
            TrackSection section = trackSections.get(sectionId);
            if (section != null && section.isAvailable()) {
                freeCount++;
            }
        }
        return freeCount;
    }

    private void openStartSignal(Route route, SignalAspect aspect) {
        Signal startSignal = signals.get(route.getStartSignalId());
        if (startSignal != null) {
            stateMachine.transitionSignal(startSignal, aspect,
                    "进路[" + route.getName() + "]开放信号");
            startSignal.setClearedByRouteId(route.getId());
        }
    }

    private void closeStartSignal(Route route) {
        Signal startSignal = signals.get(route.getStartSignalId());
        if (startSignal != null) {
            stateMachine.transitionSignal(startSignal, SignalAspect.RED,
                    "进路[" + route.getName() + "]关闭信号");
            startSignal.setClearedByRouteId(null);
        }
    }

    private void unlockRoute(Route route, String reason, String operator) {
        RouteLock routeLock = findActiveRouteLock(route.getId());
        if (routeLock != null) {
            routeLock.setActive(false);
            routeLock.setLockStatus("已解锁");
            routeLock.setUnlockTime(LocalDateTime.now());
            routeLock.setUnlockReason(reason);
            routeLock.setLastUpdateTime(LocalDateTime.now());
        }

        for (String sectionId : route.getSectionIds()) {
            TrackSection section = trackSections.get(sectionId);
            if (section != null && section.isLocked() && route.getId().equals(section.getLockedByRouteId())) {
                if (section.isOccupied()) {
                    stateMachine.transitionSection(section, TrackSectionStatus.OCCUPIED,
                            "进路解锁，区段仍占用");
                } else {
                    stateMachine.transitionSection(section, TrackSectionStatus.IDLE,
                            "进路解锁");
                }
                section.setLockedByRouteId(null);
            }
        }

        for (String switchId : route.getSwitchIds()) {
            Switch sw = switches.get(switchId);
            if (sw != null && sw.isLocked() && route.getId().equals(sw.getLockedByRouteId())) {
                sw.setLocked(false);
                sw.setLockedByRouteId(null);
                sw.setLastUpdateTime(LocalDateTime.now());
            }
        }

        for (String signalId : route.getHostileSignalIds()) {
            Signal signal = signals.get(signalId);
            if (signal != null && signal.isLocked()) {
                signal.setLocked(false);
                signal.setLastUpdateTime(LocalDateTime.now());
            }
        }

        route.setOccupiedByTrainId(null);
    }

    private void autoUnlockRoute(Route route, String trainId) {
        if (!route.isAutoUnlock()) {
            log.info("进路[{}]不支持自动解锁，需人工操作", route.getName());
            return;
        }

        try {
            stateMachine.transition(route, InterlockingStateMachine.StateEvent.START_UNLOCK);
            unlockRoute(route, "自动解锁（列车出清）", "SYSTEM");
            stateMachine.transition(route, InterlockingStateMachine.StateEvent.UNLOCK);
            route.setUnlockedTime(LocalDateTime.now());

            routeResourceReservations.remove(route.getId());

            log.info("进路自动解锁成功: routeId={}, trainId={}", route.getId(), trainId);

            broadcastRouteUpdate(route, "进路已自动解锁", "INFO");
        } catch (Exception e) {
            log.error("自动解锁进路异常: routeId={}", route.getId(), e);
        }
    }

    private void handleRouteEstablishFailure(Route route) {
        try {
            if (route.getStatus() == RouteStatus.LOCKING
                    || route.getStatus() == RouteStatus.LOCKED
                    || route.getStatus() == RouteStatus.CLEARED) {
                unlockRoute(route, "办理失败回滚", "SYSTEM");
                if (route.getStatus() == RouteStatus.LOCKING) {
                    stateMachine.transition(route, InterlockingStateMachine.StateEvent.UNLOCK);
                } else if (route.getStatus() == RouteStatus.LOCKED
                        || route.getStatus() == RouteStatus.CLEARED) {
                    stateMachine.transition(route, InterlockingStateMachine.StateEvent.CANCEL);
                    stateMachine.transition(route, InterlockingStateMachine.StateEvent.UNLOCK);
                }
            }
        } catch (Exception e) {
            log.error("回滚进路状态异常: routeId={}", route.getId(), e);
            try {
                route.setStatus(RouteStatus.NOT_ESTABLISHED);
                route.setStatusRemark("强制重置为未建立状态(事务回滚兜底)");
            } catch (Exception ex) {
                log.error("强制重置进路状态失败: routeId={}", route.getId(), ex);
            }
        }
    }

    private List<Route> findRoutesContainingSection(String sectionId) {
        return routes.values().stream()
                .filter(route -> route.getSectionIds().contains(sectionId))
                .collect(Collectors.toList());
    }

    private boolean isFirstSectionOfRoute(Route route, String sectionId) {
        return !route.getSectionIds().isEmpty()
                && route.getSectionIds().get(0).equals(sectionId);
    }

    private boolean isLastSectionOfRoute(Route route, String sectionId) {
        List<String> sections = route.getSectionIds();
        return !sections.isEmpty() && sections.get(sections.size() - 1).equals(sectionId);
    }

    private boolean areAllSectionsOfRouteReleased(Route route) {
        for (String sectionId : route.getSectionIds()) {
            TrackSection section = trackSections.get(sectionId);
            if (section != null && section.isOccupied()) {
                return false;
            }
        }
        return true;
    }

    private void checkAndUnlockPassedSections(String trainId) {
        for (Route route : routes.values()) {
            if (!route.isOccupied() || !trainId.equals(route.getOccupiedByTrainId())) {
                continue;
            }

            List<String> sectionIds = route.getSectionIds();
            for (int i = 0; i < sectionIds.size() - 1; i++) {
                String currentSectionId = sectionIds.get(i);
                String nextSectionId = sectionIds.get(i + 1);

                TrackSection currentSection = trackSections.get(currentSectionId);
                TrackSection nextSection = trackSections.get(nextSectionId);

                if (currentSection != null && nextSection != null
                        && !currentSection.isOccupied()
                        && nextSection.isOccupied()
                        && currentSection.isLocked()
                        && route.getId().equals(currentSection.getLockedByRouteId())) {

                    stateMachine.transitionSection(currentSection, TrackSectionStatus.IDLE,
                            "列车通过，区段解锁");
                    currentSection.setLockedByRouteId(null);

                    log.debug("列车通过区段解锁: sectionId={}, trainId={}", currentSectionId, trainId);
                }
            }
        }
    }

    private RouteLock findActiveRouteLock(String routeId) {
        return routeLocks.values().stream()
                .filter(lock -> routeId.equals(lock.getRouteId()) && lock.isActive())
                .findFirst()
                .orElse(null);
    }

    private RouteOperationResponse createSuccessResponse(Route route, String operationType, RouteLock routeLock) {
        return RouteOperationResponse.builder()
                .routeId(route.getId())
                .routeNumber(route.getNumber())
                .routeName(route.getName())
                .operationType(operationType)
                .success(true)
                .currentStatus(route.getStatus())
                .lockedSectionIds(routeLock != null ? routeLock.getLockedSectionIds() : new ArrayList<>())
                .lockedSwitchIds(routeLock != null ? routeLock.getLockedSwitchIds() : new ArrayList<>())
                .blockedRouteIds(routeLock != null ? routeLock.getBlockedConflictingRouteIds() : new ArrayList<>())
                .clearedSignalId(route.getStartSignalId())
                .operationTime(LocalDateTime.now())
                .build();
    }

    private RouteOperationResponse createErrorResponse(String id, String operationType,
                                                       String message, String errorCode) {
        return RouteOperationResponse.builder()
                .routeId(id)
                .operationType(operationType)
                .success(false)
                .message(message)
                .operationTime(LocalDateTime.now())
                .errorCode(errorCode)
                .errorDetail(message)
                .build();
    }

    private void broadcastStatusUpdate() {
        try {
            InterlockingStatusResponse status = getInterlockingStatus();
            WebSocketMessage<InterlockingStatusResponse> message = WebSocketMessage.statusUpdate(status);
            message.setStationId(STATION_ID);
            webSocketService.broadcastMessage(message);
        } catch (Exception e) {
            log.error("广播状态更新异常", e);
        }
    }

    private void broadcastRouteUpdate(Route route, String content, String level) {
        try {
            WebSocketMessage<Route> message = WebSocketMessage.routeUpdate(
                    "进路状态变更", content, route, level);
            message.setStationId(STATION_ID);
            webSocketService.broadcastMessage(message);
        } catch (Exception e) {
            log.error("广播进路更新异常", e);
        }
    }

    private void broadcastSwitchUpdate(Switch sw) {
        try {
            WebSocketMessage<Switch> message = WebSocketMessage.<Switch>builder()
                    .type("SWITCH_UPDATE")
                    .title("道岔状态变更")
                    .content("道岔[" + sw.getName() + "]位置变更为: " + sw.getPosition().getDescription())
                    .data(sw)
                    .level("INFO")
                    .timestamp(LocalDateTime.now())
                    .stationId(STATION_ID)
                    .build();
            webSocketService.broadcastMessage(message);
        } catch (Exception e) {
            log.error("广播道岔更新异常", e);
        }
    }

    private void broadcastSignalUpdate(Signal signal) {
        try {
            WebSocketMessage<Signal> message = WebSocketMessage.<Signal>builder()
                    .type("SIGNAL_UPDATE")
                    .title("信号机状态变更")
                    .content("信号机[" + signal.getName() + "]显示变更为: " + signal.getAspect().getDescription())
                    .data(signal)
                    .level("INFO")
                    .timestamp(LocalDateTime.now())
                    .stationId(STATION_ID)
                    .build();
            webSocketService.broadcastMessage(message);
        } catch (Exception e) {
            log.error("广播信号机更新异常", e);
        }
    }

    private void broadcastSectionUpdate(TrackSection section) {
        try {
            WebSocketMessage<TrackSection> message = WebSocketMessage.<TrackSection>builder()
                    .type("SECTION_UPDATE")
                    .title("轨道区段状态变更")
                    .content("区段[" + section.getName() + "]状态变更为: " + section.getStatus().getDescription())
                    .data(section)
                    .level("INFO")
                    .timestamp(LocalDateTime.now())
                    .stationId(STATION_ID)
                    .build();
            webSocketService.broadcastMessage(message);
        } catch (Exception e) {
            log.error("广播区段更新异常", e);
        }
    }

    public List<TrackSection> getAllTrackSections() {
        return new ArrayList<>(trackSections.values());
    }

    public List<Switch> getAllSwitches() {
        return new ArrayList<>(switches.values());
    }

    public List<Signal> getAllSignals() {
        return new ArrayList<>(signals.values());
    }

    public List<Route> getAllRoutes() {
        return new ArrayList<>(routes.values());
    }

    public TrackSection getTrackSection(String id) {
        return trackSections.get(id);
    }

    public Switch getSwitch(String id) {
        return switches.get(id);
    }

    public Signal getSignal(String id) {
        return signals.get(id);
    }

    public Route getRoute(String id) {
        return routes.get(id);
    }
}
