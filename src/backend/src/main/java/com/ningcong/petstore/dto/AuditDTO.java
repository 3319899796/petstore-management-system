package com.ningcong.petstore.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 审核通用表单：寄养与领养都用"通过/驳回 + 原因"。
 */
@Data
public class AuditDTO {

    @NotNull(message = "请指定要审核的记录")
    private Integer id;

    @NotNull(message = "请选择通过或驳回")
    private Boolean approved;

    /** 驳回时必填 */
    private String reason;
}
