package com.ningcong.petstore.dto;

import com.ningcong.petstore.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FosterQuery extends PageQuery {

    private Integer petId;

    /** 按状态筛选：PENDING / APPROVED / FINISHED / REJECTED */
    private String status;
}
