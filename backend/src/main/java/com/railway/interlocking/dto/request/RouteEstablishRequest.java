package com.railway.interlocking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 进路办理请求DTO
 * Route Establish Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteEstablishRequest {

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
     * 是否为引导进路
     */
    private boolean guidanceRoute;

    /**
     * 备注信息
     */
    private String remark;
}
