package com.ningcong.petstore.common;

import lombok.Data;

/**
 * 分页查询基类：pageNum / pageSize / keyword。
 *
 * <p>具体模块的查询条件（如宠物按品种、寄养按状态）继承它再加字段。</p>
 */
@Data
public class PageQuery {

    /** 页码，从 1 开始 */
    private long pageNum = 1;

    /** 每页条数，默认 10 */
    private long pageSize = 10;

    /** 模糊搜索关键字（各模块自己决定搜哪些列） */
    private String keyword;

    /** 防越界：页码最小 1。 */
    public long safePageNum() {
        return Math.max(pageNum, 1);
    }

    /** 防越界：每页 1~100 条，避免一次拉全表。 */
    public long safePageSize() {
        return Math.min(Math.max(pageSize, 1), 100);
    }
}
