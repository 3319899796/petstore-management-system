package com.ningcong.petstore.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.common.ResultCode;
import com.ningcong.petstore.entity.Role;
import com.ningcong.petstore.mapper.RoleMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录校验拦截器（两级鉴权的第一级）：没有合法令牌的请求一律 401，进不到 Controller。
 *
 * <p>校验通过后顺手把用户与权限放进 {@link UserContext}，供业务层和 {@link PermAspect} 使用。</p>
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final RoleMapper roleMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 浏览器跨域预检不带令牌，直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(PREFIX)) {
            writeUnauthorized(response, "缺少登录令牌");
            return false;
        }

        try {
            Claims claims = jwtUtil.parse(header.substring(PREFIX.length()).trim());
            Integer userId = claims.get("uid", Integer.class);
            Integer roleId = claims.get("rid", Integer.class);
            Role role = roleId == null ? null : roleMapper.selectById(roleId);
            UserContext.set(new LoginUser(
                    userId,
                    claims.getSubject(),
                    roleId,
                    role == null ? null : role.getRoleName(),
                    parsePermissions(role)));
            return true;
        } catch (Exception e) {
            // 签名错、格式错、过期都走这里
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 线程会被复用，必须清掉，否则下一个请求可能读到上一个用户的身份
        UserContext.clear();
    }

    /** 角色 permission 字段是逗号分隔的权限码，* 表示全部。 */
    private Set<String> parsePermissions(Role role) {
        if (role == null || !StringUtils.hasText(role.getPermission())) {
            return Collections.emptySet();
        }
        return Arrays.stream(role.getPermission().split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(ResultCode.UNAUTHORIZED.getCode());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                Result.fail(ResultCode.UNAUTHORIZED.getCode(), message)));
    }
}
