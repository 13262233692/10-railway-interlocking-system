package com.railway.interlocking.dto.response;

import com.railway.interlocking.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 联锁系统状态响应DTO
 * Interlocking Status Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterlockingStatusResponse {

    /**
     * 车站ID
     */
    private String stationId;

    /**
     * 车站名称
     */
    private String stationName;

    /**
     * 系统状态：正常、降级、故障
     */
    private String systemStatus;

    /**
     * 轨道区段列表
     */
    private List<TrackSection> trackSections;

    /**
     * 道岔列表
     */
    private List<Switch> switches;

    /**
     * 信号机列表
     */
    private List<Signal> signals;

    /**
     * 进路列表
     */
    private List<Route> routes;

    /**
     * 进路锁闭列表
     */
    private List<RouteLock> routeLocks;

    /**
     * 空闲区段数量
     */
    private int idleSectionCount;

    /**
     * 占用区段数量
     */
    private int occupiedSectionCount;

    /**
     * 锁闭区段数量
     */
    private int lockedSectionCount;

    /**
     * 已锁闭进路数量
     */
    private int lockedRouteCount;

    /**
     * 已开放进路数量
     */
    private int clearedRouteCount;

    /**
     * 故障设备数量
     */
    private int faultDeviceCount;

    /**
     * 状态更新时间
     */
    private LocalDateTime updateTime;
}
