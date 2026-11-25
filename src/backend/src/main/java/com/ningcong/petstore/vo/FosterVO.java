package com.ningcong.petstore.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class FosterVO {

    private Integer fosterId;
    private Integer petId;
    private String petName;
    private Integer ownerId;
    private String ownerName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** PENDING / APPROVED / FINISHED / REJECTED */
    private String status;

    /** 状态中文名，省得前端再写一遍映射 */
    private String statusText;

    private String handlerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 寄养天数（含首尾），前端展示用 */
    private Long days;
}
