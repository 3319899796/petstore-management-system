package com.ningcong.petstore.util;

/**
 * 脱敏工具：手机号属于个人信息，列表里只显示 138****8000。
 *
 * <p>开题报告"隐私保护"那条的落地实现。</p>
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    public static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        String value = phone.trim();
        if (value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
