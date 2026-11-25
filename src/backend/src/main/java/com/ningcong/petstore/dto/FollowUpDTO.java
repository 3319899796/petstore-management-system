package com.ningcong.petstore.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 领养回访登记表单（扩展字段，落在 adoption 表上，不建第 10 张表）。
 */
@Data
public class FollowUpDTO {

    @NotNull(message = "请指定领养申请单")
    private Integer adoptionId;

    @NotBlank(message = "请填写回访结果")
    private String followUpResult;
}
