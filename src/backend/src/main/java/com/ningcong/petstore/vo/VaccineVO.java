package com.ningcong.petstore.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

/**
 * 免疫记录展示对象：在表字段外补上宠物名、主人名和"还有几天到期"。
 */
@Data
public class VaccineVO {

    private Integer vaccineId;
    private Integer petId;
    private String petName;
    private String ownerName;
    private String vaccineType;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate inoculateDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDueDate;

    private Integer reminded;

    /** 距离下次到期还有几天：0=今天，负数=已过期（前端据此标红） */
    private Long daysLeft;
}
