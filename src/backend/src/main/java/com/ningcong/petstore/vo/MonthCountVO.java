package com.ningcong.petstore.vo;

import lombok.Data;

/**
 * 月度统计项：某个月新增了多少条记录。
 */
@Data
public class MonthCountVO {

    /** 形如 2026-11 */
    private String month;

    private Integer total;
}
