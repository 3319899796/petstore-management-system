package com.ningcong.petstore.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class AdoptionDTO {

    @NotNull(message = "请选择要领养的宠物")
    private Integer petId;
}
