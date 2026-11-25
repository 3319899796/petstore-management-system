package com.ningcong.petstore.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 新增/修改免疫记录的表单。
 *
 * <p>nextDueDate 可以手填，也可以留空——留空时后端按 cycleDays（默认 365 天）
 * 从接种日期自动推算，这就是开题报告里"免疫到期日自动计算"的实现。</p>
 */
@Data
public class VaccineDTO {

    /** 修改时必填 */
    private Integer vaccineId;

    @NotNull(message = "请选择宠物")
    private Integer petId;

    @NotBlank(message = "请填写疫苗种类")
    @Size(max = 30, message = "疫苗种类不能超过 30 个字")
    private String vaccineType;

    @NotNull(message = "请选择接种日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate inoculateDate;

    /** 可选：直接指定下次到期日 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDueDate;

    /** 可选：免疫周期天数，默认 365（狂犬/犬四联多为 1 年） */
    @Min(value = 1, message = "免疫周期至少 1 天")
    private Integer cycleDays;
}
