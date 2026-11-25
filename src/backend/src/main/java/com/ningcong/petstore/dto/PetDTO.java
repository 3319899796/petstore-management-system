package com.ningcong.petstore.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 宠物新增/编辑入参。
 */
@Data
public class PetDTO {

    private Integer petId;

    @NotBlank(message = "请填写宠物名称")
    @Size(max = 30, message = "宠物名称不超过 30 个字")
    private String petName;

    @NotBlank(message = "请填写芯片号")
    @Pattern(regexp = "^[A-Za-z0-9-]{4,30}$", message = "芯片号只能是 4~30 位字母、数字或短横线")
    private String chipNo;

    @Size(max = 30, message = "品种不超过 30 个字")
    private String breed;

    @NotNull(message = "请选择所属主人")
    private Integer ownerId;

    /** 先调 /pet/upload 上传照片拿到路径，再随表单提交 */
    private String photoPath;
}
