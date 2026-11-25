package com.ningcong.petstore.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * MaskUtil 单元测试：手机号脱敏是开题报告里"隐私保护"的落地点，
 * 这里用最简单的 assertEquals 把三种边界固定下来，防止以后改代码时把脱敏改坏。
 */
class MaskUtilTest {

    @Test
    @DisplayName("11 位手机号：保留前 3 位和后 4 位，中间用 **** 代替")
    void maskNormalPhone() {
        assertEquals("138****0001", MaskUtil.maskPhone("13800000001"));
    }

    @Test
    @DisplayName("null 安全：不做任何处理，直接返回 null（列表里没填手机号的情况）")
    void maskNull() {
        assertNull(MaskUtil.maskPhone(null));
    }

    @Test
    @DisplayName("过短的号码不脱敏，原样返回（避免出现 ****0001 这种信息更少的怪结果）")
    void maskTooShort() {
        assertEquals("12345", MaskUtil.maskPhone("12345"));
    }

    @Test
    @DisplayName("带空格时先 trim 再脱敏")
    void maskTrim() {
        assertEquals("138****0001", MaskUtil.maskPhone("  13800000001  "));
    }

    @Test
    @DisplayName("空字符串原样返回")
    void maskEmpty() {
        assertEquals("", MaskUtil.maskPhone(""));
    }
}
