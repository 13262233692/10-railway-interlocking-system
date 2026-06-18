package com.railway.interlocking.model;

import com.railway.interlocking.model.enums.RouteStatus;
import com.railway.interlocking.model.enums.RouteType;
import com.railway.interlocking.model.enums.SwitchPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 进路模型
 * Route Model
 * 表示列车在车站内的运行路径，包括轨道区段、道岔和信号机
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Route {

    /**
     * 进路唯一标识
     */
    private String id;

    /**
     * 进路编号
     */
    private String number;

    /**
     * 进路名称
     */
    private String name;

    /**
     * 进路描述
     */
    private String description;

    /**
     * 所属车站编号
     */
    private String stationId;

    /**
     * 进路类型
     */
    private RouteType routeType;

    /**
     * 进路方向：上行、下行
     */
    private String direction;

    /**
     * 进路当前状态
     */
    private RouteStatus status;

    /**
     * 进路起始信号机ID
     */
    private String startSignalId;

    /**
     * 进路终止信号机ID
     */
    private String endSignalId;

    /**
     * 进路经过的轨道区段ID列表（按顺序）
     */
    private List<String> sectionIds;

    /**
     * 进路经过的道岔ID及要求位置映射
     * key: 道岔ID, value: 要求位置
     */
    private Map<String, SwitchPosition> switchPositions;

    /**
     * 进路经过的道岔ID列表（按顺序）
     */
    private List<String> switchIds;

    /**
     * 进路防护的信号机ID列表
     */
    private List<String> signalIds;

    /**
     * 与本进路冲突的进路ID列表
     */
    private List<String> conflictingRouteIds;

    /**
     * 敌对信号机ID列表
     */
    private List<String> hostileSignalIds;

    /**
     * 进路长度（米）
     */
    private double length;

    /**
     * 进路允许速度（km/h）
     */
    private int speedLimit;

    /**
     * 进路占用的列车ID
     */
    private String occupiedByTrainId;

    /**
     * 进路办理人
     */
    private String operator;

    /**
     * 进路建立时间
     */
    private LocalDateTime establishedTime;

    /**
     * 进路锁闭时间
     */
    private LocalDateTime lockedTime;

    /**
     * 进路开放时间
     */
    private LocalDateTime clearedTime;

    /**
     * 进路占用时间
     */
    private LocalDateTime occupiedTime;

    /**
     * 进路解锁时间
     */
    private LocalDateTime unlockedTime;

    /**
     * 最后状态更新时间
     */
    private LocalDateTime lastUpdateTime;

    /**
     * 进路是否自动解锁
     */
    private boolean autoUnlock;

    /**
     * 进路是否接近锁闭
     */
    private boolean approachLocked;

    /**
     * 接近锁闭剩余时间（秒）
     */
    private int approachLockRemainingTime;

    /**
     * 进路是否为引导进路
     */
    private boolean guidanceRoute;

    /**
     * 状态变更说明
     */
    private String statusRemark;

    /**
     * 检查进路是否已锁闭
     */
    public boolean isLocked() {
        return status == RouteStatus.LOCKED || status == RouteStatus.CLEARED || status == RouteStatus.OCCUPIED;
    }

    /**
     * 检查进路是否可以办理
     */
    public boolean canEstablish() {
        return status == RouteStatus.NOT_ESTABLISHED || status == RouteStatus.FAULT;
    }

    /**
     * 检查进路是否可以取消
     */
    public boolean canCancel() {
        return status == RouteStatus.LOCKED 
                || status == RouteStatus.CLEARED 
                || status == RouteStatus.OCCUPIED
                || status == RouteStatus.LOCKING;
    }

    /**
     * 检查进路是否被占用
     */
    public boolean isOccupied() {
        return status == RouteStatus.OCCUPIED;
    }

    /**
     * 检查进路是否已开放
     */
    public boolean isCleared() {
        return status == RouteStatus.CLEARED;
    }

    /**
     * 获取进路的完整名称
     */
    public String getFullName() {
        return name + "(" + routeType.getDescription() + ")";
    }
}
