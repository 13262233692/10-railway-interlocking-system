package com.railway.interlocking.model;

import com.railway.interlocking.model.enums.TrackSectionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 轨道区段模型
 * Track Section Model
 * 表示铁路线路中的一个独立轨道区段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackSection {

    /**
     * 轨道区段唯一标识
     */
    private String id;

    /**
     * 轨道区段名称/编号
     */
    private String name;

    /**
     * 轨道区段描述
     */
    private String description;

    /**
     * 所属车站编号
     */
    private String stationId;

    /**
     * 轨道区段长度（米）
     */
    private double length;

    /**
     * 轨道区段类型：正线、到发线、调车线、渡线等
     */
    private String sectionType;

    /**
     * 轨道区段当前状态
     */
    private TrackSectionStatus status;

    /**
     * 占用该轨道区段的列车ID（如果有）
     */
    private String occupiedByTrainId;

    /**
     * 锁闭该轨道区段的进路ID（如果有）
     */
    private String lockedByRouteId;

    /**
     * 轨道区段限速（km/h）
     */
    private int speedLimit;

    /**
     * 坐标位置X
     */
    private double positionX;

    /**
     * 坐标位置Y
     */
    private double positionY;

    /**
     * 轨道区段是否有电
     */
    private boolean electrified;

    /**
     * 最后状态更新时间
     */
    private LocalDateTime lastUpdateTime;

    /**
     * 状态变更说明
     */
    private String statusRemark;

    /**
     * 检查轨道区段是否空闲且可用
     */
    public boolean isAvailable() {
        return status == TrackSectionStatus.IDLE;
    }

    /**
     * 检查轨道区段是否被占用
     */
    public boolean isOccupied() {
        return status == TrackSectionStatus.OCCUPIED;
    }

    /**
     * 检查轨道区段是否被锁闭
     */
    public boolean isLocked() {
        return status == TrackSectionStatus.LOCKED;
    }

    /**
     * 检查轨道区段是否故障
     */
    public boolean isFault() {
        return status == TrackSectionStatus.FAULT;
    }
}
