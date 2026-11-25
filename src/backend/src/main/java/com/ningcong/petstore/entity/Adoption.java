package com.ningcong.petstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 领养申请（表 adoption）。
 *
 * <p>状态机：PENDING →（通过）APPROVED →（回访登记）仍为 APPROVED 但回访字段有值；
 * 　　　　　PENDING →（驳回）REJECTED。</p>
 *
 * <p>回访字段 follow_up_time / follow_up_result 是扩展字段：
 * 开题报告明确规定"回访不单独建表"，所以直接挂在申请单上，一张单子一次回访。</p>
 */
@Data
@TableName("adoption")
public class Adoption {

    @TableId(type = IdType.AUTO)
    private Integer adoptionId;

    private Integer petId;

    /** 申请人（主人主键，不是用户主键） */
    private Integer applicantId;

    /** PENDING / APPROVED / REJECTED */
    private String status;

    private LocalDateTime applyTime;

    private LocalDateTime auditTime;

    /** 审核人（用户主键） */
    private Integer auditorId;

    private String rejectReason;

    private LocalDateTime followUpTime;

    private String followUpResult;
}
