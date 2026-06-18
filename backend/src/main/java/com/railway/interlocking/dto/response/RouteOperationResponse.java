package com.railway.interlocking.dto.response;

import com.railway.interlocking.model.enums.RouteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 进路操作响应DTO
 * Route Operation Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteOperationResponse {

    /**
     * 进路ID
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
     * 操作类型：办理、取消、锁闭、解锁等
     */
    private String operationType;

    /**
     * 操作是否成功
     */
    private boolean success;

    /**
     * 操作结果消息
     */
    private String message;

    /**
     * 进路当前状态
     */
    private RouteStatus currentStatus;

    /**
     * 锁闭的轨道区段ID列表
     */
    private List<String> lockedSectionIds;

    /**
     * 锁闭的道岔ID列表
     */
    private List<String> lockedSwitchIds;

    /**
     * 阻断的冲突进路ID列表
     */
    private List<String> blockedRouteIds;

    /**
     * 开放的信号机ID
     */
    private String clearedSignalId;

    /**
     * 操作时间
     */
    private LocalDateTime operationTime;

    /**
     * 错误代码（如果失败）
     */
    private String errorCode;

    /**
     * 错误详情
     */
    private String errorDetail;
}
