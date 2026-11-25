package com.ningcong.petstore.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Result 单元测试：所有接口都返回这个结构，前端靠 code 判断成功失败，
 * 所以它的默认值（成功 200、失败提示文案）必须固定。
 */
class ResultTest {

    @Test
    @DisplayName("ok()：code=200，message=成功，data 为空")
    void okWithoutData() {
        Result<Void> result = Result.ok();
        assertEquals(200, result.getCode());
        assertEquals("成功", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("ok(data)：把业务数据放进 data")
    void okWithData() {
        Result<String> result = Result.ok("豆豆");
        assertEquals(200, result.getCode());
        assertEquals("豆豆", result.getData());
    }

    @Test
    @DisplayName("fail(ResultCode)：code 取枚举值，message 取枚举自带的中文")
    void failWithCode() {
        Result<Void> result = Result.fail(ResultCode.FORBIDDEN);
        assertEquals(403, result.getCode());
        assertTrue(result.getMessage().contains("权限"));
    }

    @Test
    @DisplayName("fail(code, message)：可以自定义提示，供业务异常使用")
    void failWithMessage() {
        Result<Void> result = Result.fail(400, "芯片号「CHIP-CAT-0007」已存在");
        assertEquals(400, result.getCode());
        assertEquals("芯片号「CHIP-CAT-0007」已存在", result.getMessage());
    }
}
