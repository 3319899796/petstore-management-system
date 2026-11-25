package com.ningcong.petstore.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PageQuery 单元测试：分页参数是用户能随意改的（前端可以被手工改 URL），
 * 所以必须在后端把页码和每页条数限制在合理范围内，防止一次拉全表拖垮数据库。
 */
class PageQueryTest {

    @Test
    @DisplayName("默认值：第 1 页、每页 10 条")
    void defaults() {
        PageQuery query = new PageQuery();
        assertEquals(1, query.safePageNum());
        assertEquals(10, query.safePageSize());
    }

    @Test
    @DisplayName("页码 0 或负数都按第 1 页处理")
    void pageNumLowerBound() {
        PageQuery query = new PageQuery();
        query.setPageNum(0);
        assertEquals(1, query.safePageNum());

        query.setPageNum(-5);
        assertEquals(1, query.safePageNum());
    }

    @Test
    @DisplayName("每页条数上限 100：传 9999 也只取 100 条")
    void pageSizeUpperBound() {
        PageQuery query = new PageQuery();
        query.setPageSize(9999);
        assertEquals(100, query.safePageSize());
    }

    @Test
    @DisplayName("每页条数下限 1：传 0 或负数按 1 处理")
    void pageSizeLowerBound() {
        PageQuery query = new PageQuery();
        query.setPageSize(0);
        assertEquals(1, query.safePageSize());

        query.setPageSize(-3);
        assertEquals(1, query.safePageSize());
    }

    @Test
    @DisplayName("正常范围内的值原样保留")
    void normalValues() {
        PageQuery query = new PageQuery();
        query.setPageNum(3);
        query.setPageSize(20);
        assertEquals(3, query.safePageNum());
        assertEquals(20, query.safePageSize());
    }
}
