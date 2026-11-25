package com.ningcong.petstore.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 用户管理表单（仅管理员可用）。
 */
@Data
public class UserDTO {

    /** 修改时必填 */
    private Integer userId;

    @NotBlank(message = "请填写登录账号")
    @Size(min = 3, max = 30, message = "账号长度 3—30 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "账号只能包含字母、数字和下划线")
    private String username;

    /** 新增必填；修改时留空表示不改密码 */
    @Size(min = 6, max = 30, message = "密码长度 6—30 位")
    private String password;

    @NotNull(message = "请选择角色")
    private Integer roleId;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String phone;
}
