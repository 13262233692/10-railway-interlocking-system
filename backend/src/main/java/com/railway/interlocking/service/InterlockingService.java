package com.railway.interlocking.service;

import com.railway.interlocking.dto.WebSocketMessage;
import com.railway.interlocking.dto.request.*;
import com.railway.interlocking.dto.response.InterlockingStatusResponse;
import com.railway.interlocking.dto.response.RouteOperationResponse;
import com.railway.interlocking.model.*;
import com.railway.interlocking.model.enums.*;
import com.railway.interlocking.statemachine.InterlockingStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 联锁核心服务
 * Interlocking Core Service
 * 实现铁路联锁系统的核心业务逻辑，包括：
 * 1. 进路锁闭与解锁
 * 2. 道岔控制与联锁
 * 3. 信号机控制
 * 4. 轨道区段状态管理
 * 5. 冲突进路检查与阻断
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterlockingService {

    private final InterlockingStateMachine stateMachine;
    private final WebSocketService webSocketService;

    /**
     * 车站ID
     */
    private static final String STATION_ID = "STATION_001";

    /**
     * 车站名称
     */
    private static final String STATION_NAME = "高铁南站";

    /**
     * 轨道区段存储
     */
    private final Map<String, TrackSection> trackSections = new ConcurrentHashMap<>();

    /**
     * 道岔存储
     */
    private final Map<String, Switch> switches = new ConcurrentHashMap<>();

    /**
     * 信号机存储
     */
    private final Map<String, Signal> signals = new ConcurrentHashMap<>();

    /**
     * 进路存储
     */
    private final Map<String, Route> routes = new ConcurrentHashMap<>();

    /**
     * 进路锁闭存储
     */
    private final Map<String, RouteLock> routeLocks = new ConcurrentHashMap<>();

    /**
     * 初始化站场数据
     */
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

    /**
     * 办理进路
     * 核心联锁逻辑：
     * 1. 检查进路是否可以办理
     * 2. 检查所有轨道区段空闲
     * 3. 检查并转换道岔到正确位置
     * 4. 锁闭轨道区段和道岔
     * 5. 阻断所有冲突进路
     * 6. 锁闭敌对信号机
     * 7. 开放信号机
     */
    public RouteOperationResponse establishRoute(RouteEstablishRequest request) {
        log.info("开始办理进路: routeId={}, operator={}", request.getRouteId(), request.getOperator());

        Route route = routes.get(request.getRouteId());
        if (route == null) {
            return createErrorResponse(request.getRouteId(), "办理", "进路不存在", "ROUTE_NOT_FOUND");
        }

        if (!route.canEstablish()) {
            return createErrorResponse(request.getRouteId(), "办理",
                    "进路当前状态不允许办理: " + route.getStatus(), "INVALID_STATUS");
        }

        try {
            stateMachine.transition(route, InterlockingStateMachine.StateEvent.ESTABLISH);

            List<String> sectionCheckResult = checkSectionsAvailable(route);
            if (!sectionCheckResult.isEmpty()) {
                String errorMsg = "轨道区段不可用: " + String.join(", ", sectionCheckResult);
                return createErrorResponse(request.getRouteId(), "办理", errorMsg, "SECTION_NOT_AVAILABLE");
            }

            List<String> switchCheckResult = checkAndConvertSwitches(route);
            if (!switchCheckResult.isEmpty()) {
                String errorMsg = "道岔位置不正确且无法转换: " + String.join(", ", switchCheckResult);
                return createErrorResponse(request.getRouteId(), "办理", errorMsg, "SWITCH_POSITION_ERROR");
            }

            List<String> conflictCheckResult = checkConflictingRoutes(route);
            if (!conflictCheckResult.isEmpty()) {
                String errorMsg = "冲突进路已办理: " + String.join(", ", conflictCheckResult);
                return createErrorResponse(request.getRouteId(), "办理", errorMsg, "CONFLICTING_ROUTE");
            }

            RouteLock routeLock = createRouteLock(route, request.getOperator());

            lockSections(route, routeLock);
            lockSwitches(route, routeLock);
            blockConflictingRoutes(route, routeLock);
            lockHostileSignals(route, routeLock);

            stateMachine.transition(route, InterlockingStateMachine.StateEvent.LOCK);
            route.setLockedTime(LocalDateTime.now());
            route.setOperator(request.getOperator());
            route.setGuidanceRoute(request.isGuidanceRoute());

            SignalAspect aspect = calculateSignalAspect(route);
            openStartSignal(route, aspect);

            stateMachine.transition(route, InterlockingStateMachine.StateEvent.CLEAR);
            route.setClearedTime(LocalDateTime.now());

            RouteOperationResponse response = createSuccessResponse(route, "办理", routeLock);
            response.setMessage("进路办理成功，信号已开放");

            log.info("进路办理成功: routeId={}, routeName={}", route.getId(), route.getName());

            broadcastRouteUpdate(route, "进路办理成功", "SUCCESS");
            broadcastStatusUpdate();

            return response;

        } catch (Exception e) {
            log.error("办理进路异常: routeId={}", request.getRouteId(), e);
            handleRouteEstablishFailure(route);
            return createErrorResponse(request.getRouteId(), "办理",
                    "办理进路异常: " + e.getMessage(), "SYSTEM_ERROR");
        }
    }

    /**
     * 取消进路
     */
    public RouteOperationResponse cancelRoute(RouteCancelRequest request) {
        log.info("开始取消进路: routeId={}, operator={}", request.getRouteId(), request.getOperator());

        Route route = routes.get(request.getRouteId());
        if (route == null) {
            return createErrorResponse(request.getRouteId(), "取消", "进路不存在", "ROUTE_NOT_FOUND");
        }

        if (!route.canCancel()) {
            return createErrorResponse(request.getRouteId(), "取消",
                    "进路当前状态不允许取消: " + route.getStatus(), "INVALID_STATUS");
        }

        if (route.isApproachLocked() && !request.isForceCancel()) {
            return createErrorResponse(request.getRouteId(), "取消",
                    "进路处于接近锁闭状态，需要强制取消", "APPROACH_LOCKED");
        }

        try {
            stateMachine.transition(route, InterlockingStateMachine.StateEvent.CANCEL);

            closeStartSignal(route);
            unlockRoute(route, request.getReason(), request.getOperator());

            if (route.getStatus() == RouteStatus.CANCELLING) {
                stateMachine.transition(route, InterlockingStateMachine.StateEvent.UNLOCK);
            }
            route.setUnlockedTime(LocalDateTime.now());

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

            log.info("进路取消成功: routeId={}, routeName={}", route.getId(), route.getName());

            broadcastRouteUpdate(route, "进路已取消", "INFO");
            broadcastStatusUpdate();

            return response;

        } catch (Exception e) {
            log.error("取消进路异常: routeId={}", request.getRouteId(), e);
            return createErrorResponse(request.getRouteId(), "取消",
                    "取消进路异常: " + e.getMessage(), "SYSTEM_ERROR");
        }
    }

    /**
     * 操作道岔
     */
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

    /**
     * 控制信号机
     */
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

    /**
     * 占用轨道区段（模拟列车占用）
     */
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

    /**
     * 释放轨道区段（模拟列车出清）
     */
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

    /**
     * 获取联锁系统完整状态
     */
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

    /**
     * 检查轨道区段是否可用（空闲且未锁闭）
     */
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

    /**
     * 检查并转换道岔位置
     */
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

    /**
     * 检查冲突进路
     * 不仅检查冲突进路的状态，还检查区段是否被锁闭
     */
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

    /**
     * 检查道岔是否被进路阻挡
     */
    private List<String> checkSwitchBlockingRoutes(Switch sw) {
        List<String> blockingRoutes = new ArrayList<>();
        for (Route route : routes.values()) {
            if (route.isLocked() && route.getSwitchIds().contains(sw.getId())) {
                blockingRoutes.add(route.getName());
            }
        }
        return blockingRoutes;
    }

    /**
     * 创建进路锁闭记录
     */
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

    /**
     * 锁闭轨道区段
     */
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

    /**
     * 锁闭道岔
     */
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

    /**
     * 阻断冲突进路
     */
    private void blockConflictingRoutes(Route route, RouteLock routeLock) {
        for (String conflictingRouteId : route.getConflictingRouteIds()) {
            Route conflictingRoute = routes.get(conflictingRouteId);
            if (conflictingRoute != null) {
                log.debug("冲突进路已被阻断: {} -> {}", route.getName(), conflictingRoute.getName());
            }
        }
    }

    /**
     * 锁闭敌对信号机
     */
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

    /**
     * 计算信号机显示
     */
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

    /**
     * 计算前方空闲闭塞分区数量
     */
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

    /**
     * 开放起始信号机
     */
    private void openStartSignal(Route route, SignalAspect aspect) {
        Signal startSignal = signals.get(route.getStartSignalId());
        if (startSignal != null) {
            stateMachine.transitionSignal(startSignal, aspect,
                    "进路[" + route.getName() + "]开放信号");
            startSignal.setClearedByRouteId(route.getId());
        }
    }

    /**
     * 关闭起始信号机
     */
    private void closeStartSignal(Route route) {
        Signal startSignal = signals.get(route.getStartSignalId());
        if (startSignal != null) {
            stateMachine.transitionSignal(startSignal, SignalAspect.RED,
                    "进路[" + route.getName() + "]关闭信号");
            startSignal.setClearedByRouteId(null);
        }
    }

    /**
     * 解锁进路
     */
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

    /**
     * 自动解锁进路（列车出清后）
     */
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

            log.info("进路自动解锁成功: routeId={}, trainId={}", route.getId(), trainId);

            broadcastRouteUpdate(route, "进路已自动解锁", "INFO");
        } catch (Exception e) {
            log.error("自动解锁进路异常: routeId={}", route.getId(), e);
        }
    }

    /**
     * 处理进路办理失败
     */
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
                route.setStatusRemark("强制重置为未建立状态");
            } catch (Exception ex) {
                log.error("强制重置进路状态失败: routeId={}", route.getId(), ex);
            }
        }
    }

    /**
     * 查找包含指定区段的进路
     */
    private List<Route> findRoutesContainingSection(String sectionId) {
        return routes.values().stream()
                .filter(route -> route.getSectionIds().contains(sectionId))
                .collect(Collectors.toList());
    }

    /**
     * 检查是否为进路的第一个区段
     */
    private boolean isFirstSectionOfRoute(Route route, String sectionId) {
        return !route.getSectionIds().isEmpty()
                && route.getSectionIds().get(0).equals(sectionId);
    }

    /**
     * 检查是否为进路的最后一个区段
     */
    private boolean isLastSectionOfRoute(Route route, String sectionId) {
        List<String> sections = route.getSectionIds();
        return !sections.isEmpty() && sections.get(sections.size() - 1).equals(sectionId);
    }

    /**
     * 检查进路的所有区段是否都已释放（未占用）
     */
    private boolean areAllSectionsOfRouteReleased(Route route) {
        for (String sectionId : route.getSectionIds()) {
            TrackSection section = trackSections.get(sectionId);
            if (section != null && section.isOccupied()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查并解锁列车已经通过的区段
     */
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

    /**
     * 查找激活的进路锁闭
     */
    private RouteLock findActiveRouteLock(String routeId) {
        return routeLocks.values().stream()
                .filter(lock -> routeId.equals(lock.getRouteId()) && lock.isActive())
                .findFirst()
                .orElse(null);
    }

    /**
     * 创建成功响应
     */
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

    /**
     * 创建错误响应
     */
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

    /**
     * 广播状态更新
     */
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

    /**
     * 广播进路更新
     */
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

    /**
     * 广播道岔更新
     */
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

    /**
     * 广播信号机更新
     */
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

    /**
     * 广播区段更新
     */
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

    /**
     * 获取所有轨道区段
     */
    public List<TrackSection> getAllTrackSections() {
        return new ArrayList<>(trackSections.values());
    }

    /**
     * 获取所有道岔
     */
    public List<Switch> getAllSwitches() {
        return new ArrayList<>(switches.values());
    }

    /**
     * 获取所有信号机
     */
    public List<Signal> getAllSignals() {
        return new ArrayList<>(signals.values());
    }

    /**
     * 获取所有进路
     */
    public List<Route> getAllRoutes() {
        return new ArrayList<>(routes.values());
    }

    /**
     * 获取单个轨道区段
     */
    public TrackSection getTrackSection(String id) {
        return trackSections.get(id);
    }

    /**
     * 获取单个道岔
     */
    public Switch getSwitch(String id) {
        return switches.get(id);
    }

    /**
     * 获取单个信号机
     */
    public Signal getSignal(String id) {
        return signals.get(id);
    }

    /**
     * 获取单个进路
     */
    public Route getRoute(String id) {
        return routes.get(id);
    }
}
