package com.ningcong.petstore.vo;

import lombok.Data;

import java.util.List;

/**
 * 首页看板数据。
 *
 * <p>对应开题报告（三）第 7 条"统计分析：月度新增登记数、领养申请完成率、免疫到期分布"。</p>
 */
@Data
public class DashboardVO {

    /** 宠物档案总数 */
    private long petTotal;

    /** 主人总数 */
    private long ownerTotal;

    /** 待审核寄养 */
    private long fosterPending;

    /** 寄养中 */
    private long fosterActive;

    /** 待审核领养 */
    private long adoptionPending;

    /** 领养申请总数 / 已通过数 */
    private long adoptionTotal;
    private long adoptionApproved;

    /** 领养完成率（百分比，保留 1 位小数） */
    private double adoptionRate;

    /** 7 天内到期 / 已逾期 的免疫记录条数 */
    private long vaccineDue7;
    private long vaccineOverdue;

    /** 近 6 个月每月新增宠物档案数 */
    private List<MonthCountVO> monthlyPets;

    /** 免疫到期分布 */
    private DueDistributionVO dueDistribution;
}
