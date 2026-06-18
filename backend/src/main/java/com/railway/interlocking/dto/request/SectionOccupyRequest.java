package com.railway.interlocking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 轨道区段占用请求DTO
 * Section Occupy Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionOccupyRequest {

    /**
     * 轨道区段ID
     */
    @NotBlank(message = "轨道区段ID不能为空")
    private String sectionId;

    /**
     * 列车ID
     */
    @NotBlank(message = "列车ID不能为空")
    private String trainId;

    /**
     * 办理人
     */
    private String operator;
}
