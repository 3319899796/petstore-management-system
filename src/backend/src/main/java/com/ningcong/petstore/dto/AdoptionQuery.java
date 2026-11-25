package com.ningcong.petstore.dto;

import com.ningcong.petstore.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdoptionQuery extends PageQuery {

    private Integer petId;

    /** PENDING / APPROVED / REJECTED */
    private String status;
}
