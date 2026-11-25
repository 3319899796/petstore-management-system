package com.ningcong.petstore.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

/**
 * 主人新增/编辑入参。
 *
 * <p>编辑时带 ownerId，新增时为空——用同一个对象省掉两套接口。</p>
 */
@Data
public class OwnerDTO {

    private Integer ownerId;

    @NotBlank(message = "请填写主人姓名")
    @Size(max = 30, message = "主人姓名不超过 30 个字")
    private String ownerName;

    /** 允许为空（前台登记可能只有名字），填了就必须是 11 位手机号 */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号应为 11 位数字，且以 1 开头")
    private String phone;

    /** 可选：绑定一个登录账号（宠物主人角色） */
    private Integer userId;
}
