package com.ningcong.petstore.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdoptionVO {

    private Integer adoptionId;
    private Integer petId;
    private String petName;

    /** 宠物当前主人 */
    private String ownerName;

    /** 申请人 */
    private Integer applicantId;
    private String applicantName;

    /** PENDING / APPROVED / REJECTED */
    private String status;
    private String statusText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    private String auditorName;
    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followUpTime;

    private String followUpResult;
}
