package com.ningcong.petstore.dto;

import com.ningcong.petstore.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 免疫记录查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VaccineQuery extends PageQuery {

    /** 只看某只宠物 */
    private Integer petId;

    /** true = 只看 7 天内到期或已过期的（提醒页用） */
    private Boolean dueSoon;
}
