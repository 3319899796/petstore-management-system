package com.ningcong.petstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色表实体。permission 存逗号分隔的权限码，* 表示全部权限。
 */
@Data
@TableName("role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Integer roleId;

    /** 宠物店管理员 / 工作人员 / 宠物主人 */
    private String roleName;

    /** 权限码串，如 "pet:list,pet:add" */
    private String permission;
}
