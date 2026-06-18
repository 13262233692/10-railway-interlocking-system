package com.railway.interlocking.model;

import com.railway.interlocking.model.enums.SignalAspect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 信号机模型
 * Signal Model
 * 表示铁路线路中的信号机设备，用于指挥列车运行
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Signal {

    /**
     * 信号机唯一标识
     */
    private String id;

    /**
     * 信号机编号
     */
    private String number;

    /**
     * 信号机名称
     */
    private String name;

    /**
     * 信号机描述
     */
    private String description;

    /**
     * 所属车站编号
     */
    private String stationId;

    /**
     * 信号机类型：进站、出站、进路、通过、调车、驼峰等
     */
    private String signalType;

    /**
     * 信号机方向：上行、下行
     */
    private String direction;

    /**
     * 信号机当前显示
     */
    private SignalAspect aspect;

    /**
     * 信号机目标显示
     */
    private SignalAspect targetAspect;

    /**
     * 信号机是否开放
     */
    private boolean cleared;

    /**
     * 信号机是否被锁闭（不能开放）
     */
    private boolean locked;

    /**
     * 信号机是否允许显示引导信号
     */
    private boolean guidanceAllowed;

    /**
     * 开放该信号机的进路ID
     */
    private String clearedByRouteId;

    /**
     * 信号机防护的轨道区段ID列表
     */
    private List<String> protectedSectionIds;

    /**
     * 信号机所在的轨道区段ID
     */
    private String locationSectionId;

    /**
     * 信号机显示的灯位配置
     */
    private List<String> lampConfiguration;

    /**
     * 信号机允许的最大显示
     */
    private SignalAspect maximumAspect;

    /**
     * 坐标位置X
     */
    private double positionX;

    /**
     * 坐标位置Y
     */
    private double positionY;

    /**
     * 信号机显示距离（米）
     */
    private int displayDistance;

    /**
     * 最后状态更新时间
     */
    private LocalDateTime lastUpdateTime;

    /**
     * 最后开放时间
     */
    private LocalDateTime lastClearTime;

    /**
     * 信号机是否有电
     */
    private boolean electrified;

    /**
     * 灯丝是否完好
     */
    private boolean lampOk;

    /**
     * 状态变更说明
     */
    private String statusRemark;

    /**
     * 检查信号机是否允许通过
     */
    public boolean allowsPass() {
        return cleared && aspect != null && aspect.isAllowPass();
    }

    /**
     * 检查信号机是否显示禁止信号
     */
    public boolean isStopSignal() {
        return aspect == SignalAspect.RED || aspect == SignalAspect.OFF;
    }

    /**
     * 检查信号机是否可以开放
     */
    public boolean canClear() {
        return !locked && electrified && lampOk && aspect != SignalAspect.OFF;
    }

    /**
     * 关闭信号机（恢复红灯）
     */
    public void close() {
        this.aspect = SignalAspect.RED;
        this.cleared = false;
        this.clearedByRouteId = null;
        this.lastUpdateTime = LocalDateTime.now();
    }
}
