package com.ningcong.petstore.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物列表/详情返回对象：把主人姓名和手机号一并带上，前端列表不用二次请求。
 *
 * <p>这是典型的"VO 装配"：数据库里 pet 表没有主人姓名，service 里批量查 owner 再填进来。</p>
 */
@Data
public class PetVO {

    private Integer petId;

    private String petName;

    private String chipNo;

    private String breed;

    private Integer ownerId;

    /** 主人姓名（来自 owner 表） */
    private String ownerName;

    /** 主人手机号（脱敏后） */
    private String ownerPhone;

    private String photoPath;

    private LocalDateTime createTime;
}
