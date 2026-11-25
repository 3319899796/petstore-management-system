package com.ningcong.petstore.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicalVO {

    private Integer medicalId;
    private Integer petId;
    private String petName;
    private String ownerName;
    private String symptom;
    private String diagnosis;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;
}
