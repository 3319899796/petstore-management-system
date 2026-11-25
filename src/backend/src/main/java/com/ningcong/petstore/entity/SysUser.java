package com.ningcong.petstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表实体（表名 sys_user：user 是 MySQL 保留关键字）。
 *
 * <p>字段与 src/db/schema.sql 一一对应；下划线列名靠 map-underscore-to-camel-case 自动映射。</p>
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Integer userId;

    /** 登录账号，唯一 */
    private String username;

    /** BCrypt 哈希，绝不出现在任何响应里 */
    @JsonIgnore
    private String password;

    /** 角色主键 */
    private Integer roleId;

    /** 手机号（展示时脱敏） */
    private String phone;

    private LocalDateTime createTime;
}
