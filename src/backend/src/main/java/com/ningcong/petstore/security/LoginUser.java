package com.ningcong.petstore.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.Set;

/**
 * 当前请求的登录用户（从 JWT 解析后暂存在线程上下文里）。
 */
@Data
@AllArgsConstructor
public class LoginUser {

    /** 用户主键 */
    private Integer userId;
    /** 登录账号 */
    private String username;
    /** 角色主键 */
    private Integer roleId;
    /** 角色名称，仅用于展示 */
    private String roleName;
    /** 权限码集合，如 pet:add；管理员为 {"*"} */
    private Set<String> permissions;

    public boolean hasPermission(String permission) {
        return permissions != null
                && (permissions.contains("*") || permissions.contains(permission));
    }

    public Set<String> safePermissions() {
        return permissions == null ? Collections.emptySet() : permissions;
    }
}
