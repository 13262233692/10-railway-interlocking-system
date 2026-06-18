package com.railway.interlocking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 进路取消请求DTO
 * Route Cancel Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteCancelRequest {

    /**
     * 进路ID
     */
    @NotBlank(message = "进路ID不能为空")
    private String routeId;

    /**
     * 办理人
     */
    private String operator;

    /**
     * 取消原因
     */
    private String reason;

    /**
     * 是否强制取消（解锁接近锁闭的进路）
     */
    private boolean forceCancel;
}
