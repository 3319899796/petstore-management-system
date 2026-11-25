package com.ningcong.petstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 寄养记录（表 foster）。
 *
 * <p>状态机：PENDING →（审核通过）APPROVED →（到期结束）FINISHED
 * 　　　　　PENDING →（审核驳回）REJECTED</p>
 */
@Data
@TableName("foster")
public class Foster {

    @TableId(type = IdType.AUTO)
    private Integer fosterId;

    private Integer petId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /** PENDING / APPROVED / FINISHED / REJECTED */
    private String status;

    /** 处理人（审核的工作人员，用户主键） */
    private Integer handlerId;

    private LocalDateTime createTime;
}
