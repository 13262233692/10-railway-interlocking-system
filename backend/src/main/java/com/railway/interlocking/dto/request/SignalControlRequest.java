package com.railway.interlocking.dto.request;

import com.railway.interlocking.model.enums.SignalAspect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 信号机控制请求DTO
 * Signal Control Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalControlRequest {

    /**
     * 信号机ID
     */
    @NotBlank(message = "信号机ID不能为空")
    private String signalId;

    /**
     * 目标显示
     */
    @NotNull(message = "目标显示不能为空")
    private SignalAspect targetAspect;

    /**
     * 办理人
     */
    private String operator;

    /**
     * 是否强制操作
     */
    private boolean forceControl;
}
