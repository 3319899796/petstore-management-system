package com.ningcong.petstore.dto;

import com.ningcong.petstore.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MedicalQuery extends PageQuery {

    private Integer petId;
}
