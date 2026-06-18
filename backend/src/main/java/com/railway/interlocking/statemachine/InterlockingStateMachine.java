package com.railway.interlocking.statemachine;

import com.railway.interlocking.model.Route;
import com.railway.interlocking.model.enums.RouteStatus;
import com.railway.interlocking.model.enums.SignalAspect;
import com.railway.interlocking.model.enums.SwitchPosition;
import com.railway.interlocking.model.enums.TrackSectionStatus;
import com.railway.interlocking.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 联锁状态机
 * Interlocking State Machine
 * 负责管理进路、轨道区段、道岔、信号机的状态转换
 * 实现联锁逻辑的状态管理核心
 */
@Slf4j
@Component
public class InterlockingStateMachine {

    /**
     * 状态转换事件枚举
     */
    public enum StateEvent {
        ESTABLISH,          // 办理进路
        LOCK,               // 锁闭进路
        CLEAR,               // 开放信号
        OCCUPY,               // 进路占用
        START_UNLOCK,         // 开始解锁
        UNLOCK,               // 解锁进路
        CANCEL,               // 取消进路
        FAULT,                // 故障
        RECOVER                // 恢复
    }

    /**
     * 检查进路状态转换是否合法
     * @param route 进路
     * @param event 事件
     * @return 是否允许转换
     */
    public boolean canTransition(Route route, StateEvent event) {
        if (route == null) {
            log.error("进路对象为空，无法进行状态转换检查");
            return false;
        }

        RouteStatus currentStatus = route.getStatus();
        log.debug("检查进路[{}]状态转换检查: 当前状态={}, 事件={}",
                route.getId(), currentStatus, event);

        switch (currentStatus) {
            case NOT_ESTABLISHED:
                return canTransitionFromNotEstablished(event);
            case LOCKING:
                return canTransitionFromLocking(event);
            case LOCKED:
                return canTransitionFromLocked(event);
            case CLEARED:
                return canTransitionFromCleared(event);
            case OCCUPIED:
                return canTransitionFromOccupied(event);
            case UNLOCKING:
                return canTransitionFromUnlocking(event);
            case CANCELLING:
                return canTransitionFromCancelling(event);
            case FAULT:
                return canTransitionFromFault(event);
            default:
                log.warn("进路[{}]处于未知状态[{}]，不允许状态转换", route.getId(), currentStatus);
                return false;
        }
    }

    /**
     * 从【未建立】状态的转换规则
     */
    private boolean canTransitionFromNotEstablished(StateEvent event) {
        switch (event) {
            case ESTABLISH:
                return true;
            case FAULT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 从【锁闭中】状态的转换规则
     */
    private boolean canTransitionFromLocking(StateEvent event) {
        switch (event) {
            case LOCK:
                return true;
            case CANCEL:
                return true;
            case UNLOCK:
                return true;
            case FAULT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 从【已锁闭】状态的转换规则
     */
    private boolean canTransitionFromLocked(StateEvent event) {
        switch (event) {
            case CLEAR:
                return true;
            case CANCEL:
                return true;
            case OCCUPY:
                return true;
            case FAULT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 从【开放】状态的转换规则
     */
    private boolean canTransitionFromCleared(StateEvent event) {
        switch (event) {
            case OCCUPY:
                return true;
            case CANCEL:
                return true;
            case START_UNLOCK:
                return true;
            case FAULT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 从【占用中】状态的转换规则
     */
    private boolean canTransitionFromOccupied(StateEvent event) {
        switch (event) {
            case START_UNLOCK:
                return true;
            case UNLOCK:
                return true;
            case CANCEL:
                return true;
            case FAULT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 从【解锁中】状态的转换规则
     */
    private boolean canTransitionFromUnlocking(StateEvent event) {
        switch (event) {
            case UNLOCK:
                return true;
            case FAULT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 从【取消中】状态的转换规则
     */
    private boolean canTransitionFromCancelling(StateEvent event) {
        switch (event) {
            case UNLOCK:
                return true;
            case FAULT:
                return true;
            default:
                return false;
        }
    }

    /**
     * 从【故障】状态的转换规则
     */
    private boolean canTransitionFromFault(StateEvent event) {
        switch (event) {
            case RECOVER:
                return true;
            case CANCEL:
                return true;
            default:
                return false;
        }
    }

    /**
     * 执行进路状态转换
     * @param route 进路
     * @param event 事件
     * @return 转换后的状态
     */
    public RouteStatus transition(Route route, StateEvent event) {
        if (!canTransition(route, event)) {
            log.error("进路[{}]不允许状态转换: 当前状态={}, 事件={}",
                    route.getId(), route.getStatus(), event);
            throw new IllegalStateException(
                    String.format("进路[%s]状态转换不允许: 当前状态=%s, 事件=%s",
                            route.getId(), route.getStatus(), event));
        }

        RouteStatus currentStatus = route.getStatus();
        RouteStatus newStatus = getTargetStatus(currentStatus, event);

        log.info("进路[{}]状态转换: {} -> {} (事件: {})",
                route.getId(), currentStatus, newStatus, event);

        route.setStatus(newStatus);
        route.setLastUpdateTime(LocalDateTime.now());
        route.setStatusRemark("状态转换: " + currentStatus + " -> " + newStatus);

        return newStatus;
    }

    /**
     * 获取目标状态
     */
    private RouteStatus getTargetStatus(RouteStatus currentStatus, StateEvent event) {
        switch (currentStatus) {
            case NOT_ESTABLISHED:
                if (event == StateEvent.ESTABLISH) return RouteStatus.LOCKING;
                if (event == StateEvent.FAULT) return RouteStatus.FAULT;
                break;
            case LOCKING:
                if (event == StateEvent.LOCK) return RouteStatus.LOCKED;
                if (event == StateEvent.CANCEL) return RouteStatus.CANCELLING;
                if (event == StateEvent.UNLOCK) return RouteStatus.NOT_ESTABLISHED;
                if (event == StateEvent.FAULT) return RouteStatus.FAULT;
                break;
            case LOCKED:
                if (event == StateEvent.CLEAR) return RouteStatus.CLEARED;
                if (event == StateEvent.CANCEL) return RouteStatus.CANCELLING;
                if (event == StateEvent.OCCUPY) return RouteStatus.OCCUPIED;
                if (event == StateEvent.FAULT) return RouteStatus.FAULT;
                break;
            case CLEARED:
                if (event == StateEvent.OCCUPY) return RouteStatus.OCCUPIED;
                if (event == StateEvent.CANCEL) return RouteStatus.CANCELLING;
                if (event == StateEvent.START_UNLOCK) return RouteStatus.UNLOCKING;
                if (event == StateEvent.FAULT) return RouteStatus.FAULT;
                break;
            case OCCUPIED:
                if (event == StateEvent.START_UNLOCK) return RouteStatus.UNLOCKING;
                if (event == StateEvent.UNLOCK) return RouteStatus.NOT_ESTABLISHED;
                if (event == StateEvent.CANCEL) return RouteStatus.CANCELLING;
                if (event == StateEvent.FAULT) return RouteStatus.FAULT;
                break;
            case UNLOCKING:
                if (event == StateEvent.UNLOCK) return RouteStatus.NOT_ESTABLISHED;
                if (event == StateEvent.FAULT) return RouteStatus.FAULT;
                break;
            case CANCELLING:
                if (event == StateEvent.UNLOCK) return RouteStatus.NOT_ESTABLISHED;
                if (event == StateEvent.FAULT) return RouteStatus.FAULT;
                break;
            case FAULT:
                if (event == StateEvent.RECOVER) return RouteStatus.NOT_ESTABLISHED;
                if (event == StateEvent.CANCEL) return RouteStatus.CANCELLING;
                break;
        }
        return currentStatus;
    }

    /**
     * 检查轨道区段状态转换是否合法
     * @param section 轨道区段
     * @param targetStatus 目标状态
     * @return 是否允许转换
     */
    public boolean canTransitionSection(TrackSection section, TrackSectionStatus targetStatus) {
        if (section == null) {
            return false;
        }
        TrackSectionStatus currentStatus = section.getStatus();
        log.debug("检查轨道区段[{}]状态转换检查: 当前状态={}, 目标状态={}",
                section.getId(), currentStatus, targetStatus);

        if (currentStatus == targetStatus) {
            return true;
        }

        switch (currentStatus) {
            case IDLE:
                return targetStatus == TrackSectionStatus.OCCUPIED
                        || targetStatus == TrackSectionStatus.LOCKED
                        || targetStatus == TrackSectionStatus.FAULT;
            case OCCUPIED:
                return targetStatus == TrackSectionStatus.IDLE
                        || targetStatus == TrackSectionStatus.FAULT;
            case LOCKED:
                return targetStatus == TrackSectionStatus.IDLE
                        || targetStatus == TrackSectionStatus.OCCUPIED
                        || targetStatus == TrackSectionStatus.FAULT;
            case FAULT:
                return targetStatus == TrackSectionStatus.IDLE;
            default:
                return false;
        }
    }

    /**
     * 执行轨道区段状态转换
     */
    public TrackSectionStatus transitionSection(TrackSection section, TrackSectionStatus targetStatus, String remark) {
        if (!canTransitionSection(section, targetStatus)) {
            log.error("轨道区段[{}]不允许状态转换: 当前状态={}, 目标状态={}",
                    section.getId(), section.getStatus(), targetStatus);
            throw new IllegalStateException(
                    String.format("轨道区段[%s]状态转换不允许: 当前状态=%s, 目标状态=%s",
                            section.getId(), section.getStatus(), targetStatus));
        }

        if (section.getStatus() != targetStatus) {
            log.info("轨道区段[{}]状态转换: {} -> {}",
                    section.getId(), section.getStatus(), targetStatus);
            section.setStatus(targetStatus);
            section.setLastUpdateTime(LocalDateTime.now());
            section.setStatusRemark(remark);
        }

        return targetStatus;
    }

    /**
     * 检查道岔状态转换是否合法
     */
    public boolean canTransitionSwitch(Switch sw, SwitchPosition targetPosition) {
        if (sw == null) {
            return false;
        }

        if (sw.isLocked()) {
            log.warn("道岔[{}]已被锁闭，不允许转换", sw.getId());
            return false;
        }

        SwitchPosition currentPosition = sw.getPosition();

        if (currentPosition == targetPosition) {
            return true;
        }

        if (currentPosition == SwitchPosition.FAULT) {
            return false;
        }

        if (currentPosition == SwitchPosition.MOVING) {
            log.warn("道岔[{}]正在转换中，不允许再次转换", sw.getId());
            return false;
        }

        return targetPosition == SwitchPosition.NORMAL
                || targetPosition == SwitchPosition.REVERSE
                || targetPosition == SwitchPosition.FAULT;
    }

    /**
     * 执行道岔状态转换
     */
    public SwitchPosition transitionSwitch(Switch sw, SwitchPosition targetPosition, String remark) {
        if (!canTransitionSwitch(sw, targetPosition)) {
            log.error("道岔[{}]不允许状态转换: 当前位置={}, 目标位置={}",
                    sw.getId(), sw.getPosition(), targetPosition);
            throw new IllegalStateException(
                    String.format("道岔[%s]状态转换不允许: 当前位置=%s, 目标位置=%s",
                            sw.getId(), sw.getPosition(), targetPosition));
        }

        if (sw.getPosition() != targetPosition) {
            log.info("道岔[{}]状态转换: {} -> {}",
                    sw.getId(), sw.getPosition(), targetPosition);
            sw.setPosition(targetPosition);
            sw.setIndication(targetPosition);
            sw.setLastUpdateTime(LocalDateTime.now());
            sw.setStatusRemark(remark);
            sw.setLastOperationTime(LocalDateTime.now());
        }

        return targetPosition;
    }

    /**
     * 检查信号机状态转换是否合法
     */
    public boolean canTransitionSignal(Signal signal, SignalAspect targetAspect) {
        if (signal == null) {
            return false;
        }

        if (signal.isLocked() && targetAspect != SignalAspect.RED && targetAspect != SignalAspect.OFF) {
            log.warn("信号机[{}]已被锁闭，不允许开放", signal.getId());
            return false;
        }

        SignalAspect currentAspect = signal.getAspect();

        if (currentAspect == targetAspect) {
            return true;
        }

        if (!signal.isElectrified() || !signal.isLampOk()) {
            if (targetAspect != SignalAspect.OFF && targetAspect != SignalAspect.RED) {
                log.warn("信号机[{}]设备异常，只能显示红灯或灭灯", signal.getId());
                return false;
            }
        }

        return true;
    }

    /**
     * 执行信号机状态转换
     */
    public SignalAspect transitionSignal(Signal signal, SignalAspect targetAspect, String remark) {
        if (!canTransitionSignal(signal, targetAspect)) {
            log.error("信号机[{}]不允许状态转换: 当前显示={}, 目标显示={}",
                    signal.getId(), signal.getAspect(), targetAspect);
            throw new IllegalStateException(
                    String.format("信号机[%s]状态转换不允许: 当前显示=%s, 目标显示=%s",
                            signal.getId(), signal.getAspect(), targetAspect));
        }

        if (signal.getAspect() != targetAspect) {
            log.info("信号机[{}]状态转换: {} -> {}",
                    signal.getId(), signal.getAspect(), targetAspect);
            signal.setAspect(targetAspect);
            signal.setCleared(targetAspect.isAllowPass());
            signal.setLastUpdateTime(LocalDateTime.now());
            signal.setStatusRemark(remark);

            if (targetAspect.isAllowPass()) {
                signal.setLastClearTime(LocalDateTime.now());
            }
        }

        return targetAspect;
    }

    /**
     * 获取状态转换说明
     */
    public String getTransitionDescription(RouteStatus from, RouteStatus to) {
        Map<String, String> descriptions = new ConcurrentHashMap<>();
        descriptions.put("NOT_ESTABLISHED->LOCKING", "开始办理进路");
        descriptions.put("LOCKING->LOCKED", "进路已锁闭");
        descriptions.put("LOCKED->CLEARED", "信号已开放");
        descriptions.put("LOCKED->OCCUPIED", "进路被占用");
        descriptions.put("CLEARED->OCCUPIED", "列车进入进路");
        descriptions.put("OCCUPIED->UNLOCKING", "开始解锁进路");
        descriptions.put("UNLOCKING->NOT_ESTABLISHED", "进路已解锁");
        descriptions.put("LOCKED->CANCELLING", "开始取消进路");
        descriptions.put("CLEARED->CANCELLING", "开始取消进路");
        descriptions.put("CANCELLING->NOT_ESTABLISHED", "进路已取消");

        String key = from + "->" + to;
        return descriptions.getOrDefault(key, "状态转换: " + from + " -> " + to);
    }

    /**
     * 验证进路状态链是否完整
     * 检查进路从建立到解锁的完整生命周期是否合法
     */
    public boolean validateRouteLifeCycle(List<RouteStatus> statusHistory) {
        if (statusHistory == null || statusHistory.isEmpty()) {
            return false;
        }

        RouteStatus first = statusHistory.get(0);
        RouteStatus last = statusHistory.get(statusHistory.size() - 1);

        if (first != RouteStatus.NOT_ESTABLISHED) {
            log.error("进路生命周期必须从未建立状态开始");
            return false;
        }

        for (int i = 1; i < statusHistory.size(); i++) {
            RouteStatus prev = statusHistory.get(i - 1);
            RouteStatus curr = statusHistory.get(i);
            if (!isValidNextStatus(prev, curr)) {
                log.error("进路生命周期状态转换不合法: {} -> {}", prev, curr);
                return false;
            }
        }

        return true;
    }

    /**
     * 检查是否为合法的后续状态
     */
    private boolean isValidNextStatus(RouteStatus current, RouteStatus next) {
        switch (current) {
            case NOT_ESTABLISHED:
                return next == RouteStatus.LOCKING || next == RouteStatus.FAULT;
            case LOCKING:
                return next == RouteStatus.LOCKED || next == RouteStatus.CANCELLING || next == RouteStatus.FAULT;
            case LOCKED:
                return next == RouteStatus.CLEARED || next == RouteStatus.OCCUPIED
                        || next == RouteStatus.CANCELLING || next == RouteStatus.FAULT;
            case CLEARED:
                return next == RouteStatus.OCCUPIED || next == RouteStatus.CANCELLING
                        || next == RouteStatus.UNLOCKING || next == RouteStatus.FAULT;
            case OCCUPIED:
                return next == RouteStatus.UNLOCKING || next == RouteStatus.NOT_ESTABLISHED
                        || next == RouteStatus.CANCELLING || next == RouteStatus.FAULT;
            case UNLOCKING:
            case CANCELLING:
                return next == RouteStatus.NOT_ESTABLISHED || next == RouteStatus.FAULT;
            case FAULT:
                return next == RouteStatus.NOT_ESTABLISHED || next == RouteStatus.CANCELLING;
            default:
                return false;
        }
    }
}
