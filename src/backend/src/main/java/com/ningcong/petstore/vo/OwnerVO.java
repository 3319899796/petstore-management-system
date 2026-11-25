package com.ningcong.petstore.vo;

import lombok.Data;

/**
 * 主人列表/详情返回对象。手机号已脱敏，原始号码不出后端。
 */
@Data
public class OwnerVO {

    private Integer ownerId;

    private String ownerName;

    /** 形如 138****8000 */
    private String phone;

    /** 关联登录账号，可空 */
    private Integer userId;
}
