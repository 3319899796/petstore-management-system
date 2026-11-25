package com.ningcong.petstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告（表 notice）。
 *
 * <p>项目只允许 9 张表（见开题报告（五）数据库设计），所以"站内提醒"不另建消息表，
 * 而是复用公告表：定时任务生成的提醒以【疫苗到期提醒】作为标题前缀，
 * 前端按前缀区分"系统提醒"和"人工公告"。</p>
 */
@Data
@TableName("notice")
public class Notice {

    @TableId(type = IdType.AUTO)
    private Integer noticeId;

    private String title;

    private String content;

    /** 发布人（用户主键）；系统自动生成时为 null */
    private Integer publisherId;

    private LocalDateTime publishTime;
}
