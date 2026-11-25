package com.ningcong.petstore.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

@Data
public class MedicalDTO {

    /** 修改时必填 */
    private Integer medicalId;

    @NotNull(message = "请选择宠物")
    private Integer petId;

    @NotBlank(message = "请填写症状")
    @Size(max = 255, message = "症状描述不能超过 255 个字")
    private String symptom;

    @Size(max = 255, message = "诊断结果不能超过 255 个字")
    private String diagnosis;

    @NotNull(message = "请选择就诊日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;
}
