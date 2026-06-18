package com.railway.interlocking.dto.request;

import com.railway.interlocking.model.enums.SwitchPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 道岔操作请求DTO
 * Switch Operate Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchOperateRequest {

    /**
     * 道岔ID
     */
    @NotBlank(message = "道岔ID不能为空")
    private String switchId;

    /**
     * 目标位置
     */
    @NotNull(message = "目标位置不能为空")
    private SwitchPosition targetPosition;

    /**
     * 办理人
     */
    private String operator;

    /**
     * 是否强制操作
     */
    private boolean forceOperate;
}
