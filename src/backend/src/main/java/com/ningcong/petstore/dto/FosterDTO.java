package com.ningcong.petstore.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 寄养申请表单（新增/修改）。
 */
@Data
public class FosterDTO {

    /** 修改时必填 */
    private Integer fosterId;

    @NotNull(message = "请选择寄养宠物")
    private Integer petId;

    @NotNull(message = "请选择寄养开始日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "请选择寄养结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
