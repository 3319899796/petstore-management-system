package com.ningcong.petstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 宠物主人。user_id 可空：不是每个主人都要开登录账号（前台登记的主人可能只有手机号）。
 */
@Data
@TableName("owner")
public class Owner {

    @TableId(type = IdType.AUTO)
    private Integer ownerId;

    private String ownerName;

    /** 手机号，列表展示时脱敏 */
    private String phone;

    /** 关联的登录账号（sys_user.user_id），可空 */
    private Integer userId;
}
