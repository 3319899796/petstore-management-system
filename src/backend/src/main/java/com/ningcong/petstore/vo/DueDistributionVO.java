package com.ningcong.petstore.vo;

import lombok.Data;

/**
 * 免疫到期分布：逾期 / 7 天内 / 30 天内 / 更久，用于统计图表。
 */
@Data
public class DueDistributionVO {

    private Long overdue;
    private Long in7;
    private Long in30;
    private Long later;
}
