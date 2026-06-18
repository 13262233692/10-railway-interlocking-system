package com.railway.interlocking.model;

import com.railway.interlocking.model.enums.SwitchPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 进路锁闭模型
 * Route Lock Model
 * 记录进路锁闭的详细信息，包括锁闭的区段、道岔和信号机
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteLock {

    /**
     * 锁闭唯一标识
     */
    private String id;

    /**
     * 所属进路ID
     */
    private String routeId;

    /**
     * 进路编号
     */
    private String routeNumber;

    /**
     * 进路名称
     */
    private String routeName;

    /**
     * 所属车站编号
     */
    private String stationId;

    /**
     * 锁闭类型：区段锁闭、道岔锁闭、信号机锁闭
     */
    private String lockType;

    /**
     * 锁闭级别：正常锁闭、引导锁闭、人工锁闭
     */
    private String lockLevel;

    /**
     * 锁闭状态：已锁闭、解锁中、已解锁
     */
    private String lockStatus;

    /**
     * 被锁闭的轨道区段ID列表
     */
    private List<String> lockedSectionIds;

    /**
     * 被锁闭的道岔ID及位置映射
     * key: 道岔ID, value: 锁闭位置
     */
    private Map<String, SwitchPosition> lockedSwitchPositions;

    /**
     * 被锁闭的道岔ID列表
     */
    private List<String> lockedSwitchIds;

    /**
     * 被锁闭的信号机ID列表（禁止开放的敌对信号）
     */
    private List<String> lockedSignalIds;

    /**
     * 被阻断的冲突进路ID列表
     */
    private List<String> blockedConflictingRouteIds;

    /**
     * 锁闭建立时间
     */
    private LocalDateTime lockTime;

    /**
     * 锁闭预计解锁时间
     */
    private LocalDateTime expectedUnlockTime;

    /**
     * 锁闭实际解锁时间
     */
    private LocalDateTime unlockTime;

    /**
     * 最后状态更新时间
     */
    private LocalDateTime lastUpdateTime;

    /**
     * 锁闭是否有效
     */
    private boolean active;

    /**
     * 锁闭是否为临时锁闭
     */
    private boolean temporary;

    /**
     * 锁闭原因
     */
    private String lockReason;

    /**
     * 解锁原因
     */
    private String unlockReason;

    /**
     * 办理人
     */
    private String operator;

    /**
     * 占用该锁闭的列车ID
     */
    private String trainId;

    /**
     * 锁闭进度百分比（0-100）
     */
    private int lockProgress;

    /**
     * 解锁进度百分比（0-100）
     */
    private int unlockProgress;

    /**
     * 备注信息
     */
    private String remark;

    /**
     * 检查锁闭是否处于激活状态
     */
    public boolean isActive() {
        return active && "已锁闭".equals(lockStatus);
    }

    /**
     * 检查指定区段是否被该锁闭锁闭
     */
    public boolean isSectionLocked(String sectionId) {
        return lockedSectionIds != null && lockedSectionIds.contains(sectionId);
    }

    /**
     * 检查指定道岔是否被该锁闭锁闭
     */
    public boolean isSwitchLocked(String switchId) {
        return lockedSwitchIds != null && lockedSwitchIds.contains(switchId);
    }

    /**
     * 检查指定信号机是否被该锁闭锁闭
     */
    public boolean isSignalLocked(String signalId) {
        return lockedSignalIds != null && lockedSignalIds.contains(signalId);
    }

    /**
     * 检查指定进路是否被该锁闭阻断
     */
    public boolean isRouteBlocked(String routeId) {
        return blockedConflictingRouteIds != null && blockedConflictingRouteIds.contains(routeId);
    }

    /**
     * 获取道岔的锁闭位置
     */
    public SwitchPosition getSwitchLockPosition(String switchId) {
        if (lockedSwitchPositions != null) {
            return lockedSwitchPositions.get(switchId);
        }
        return null;
    }
}
