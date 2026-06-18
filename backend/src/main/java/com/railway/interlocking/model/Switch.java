package com.railway.interlocking.model;

import com.railway.interlocking.model.enums.SwitchPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 道岔模型
 * Switch Model
 * 表示铁路线路中的道岔设备，用于转换线路方向
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Switch {

    /**
     * 道岔唯一标识
     */
    private String id;

    /**
     * 道岔编号
     */
    private String number;

    /**
     * 道岔名称
     */
    private String name;

    /**
     * 道岔描述
     */
    private String description;

    /**
     * 所属车站编号
     */
    private String stationId;

    /**
     * 道岔型号
     */
    private String switchType;

    /**
     * 道岔辙叉号数
     */
    private int frogNumber;

    /**
     * 道岔当前位置
     */
    private SwitchPosition position;

    /**
     * 道岔目标位置（转换中时使用）
     */
    private SwitchPosition targetPosition;

    /**
     * 道岔表示状态
     */
    private SwitchPosition indication;

    /**
     * 道岔是否被锁闭
     */
    private boolean locked;

    /**
     * 锁闭该道岔的进路ID
     */
    private String lockedByRouteId;

    /**
     * 道岔定位时连接的轨道区段ID
     */
    private String normalSectionId;

    /**
     * 道岔反位时连接的轨道区段ID
     */
    private String reverseSectionId;

    /**
     * 道岔尖端轨区段ID
     */
    private String toeSectionId;

    /**
     * 道岔允许通过速度-定位（km/h）
     */
    private int normalSpeedLimit;

    /**
     * 道岔允许通过速度-反位（km/h）
     */
    private int reverseSpeedLimit;

    /**
     * 坐标位置X
     */
    private double positionX;

    /**
     * 坐标位置Y
     */
    private double positionY;

    /**
     * 道岔转换时间（秒）
     */
    private int operationTime;

    /**
     * 最后转换时间
     */
    private LocalDateTime lastOperationTime;

    /**
     * 最后状态更新时间
     */
    private LocalDateTime lastUpdateTime;

    /**
     * 道岔是否有电
     */
    private boolean electrified;

    /**
     * 状态变更说明
     */
    private String statusRemark;

    /**
     * 检查道岔位置是否与要求位置一致
     */
    public boolean isPositionCorrect(SwitchPosition requiredPosition) {
        return position == requiredPosition && indication == requiredPosition;
    }

    /**
     * 检查道岔是否可以转换
     */
    public boolean canOperate() {
        return !locked && position != SwitchPosition.FAULT && position != SwitchPosition.MOVING;
    }

    /**
     * 获取道岔当前允许的通过速度
     */
    public int getCurrentSpeedLimit() {
        if (position == SwitchPosition.NORMAL) {
            return normalSpeedLimit;
        } else if (position == SwitchPosition.REVERSE) {
            return reverseSpeedLimit;
        }
        return 0;
    }
}
