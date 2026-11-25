package com.ningcong.petstore.dto;

import com.ningcong.petstore.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 宠物列表查询条件：在通用分页参数上再加"按主人""按品种"筛选。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PetQuery extends PageQuery {

    private Integer ownerId;

    private String breed;
}
