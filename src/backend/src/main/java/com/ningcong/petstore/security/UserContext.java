package com.ningcong.petstore.security;

/**
 * 线程级登录用户容器。
 *
 * <p>Tomcat 用线程池处理请求，所以用 ThreadLocal 存"当前请求的用户"：
 * 拦截器 preHandle 里 set，afterCompletion 里必须 clear，否则线程复用会串号。</p>
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
