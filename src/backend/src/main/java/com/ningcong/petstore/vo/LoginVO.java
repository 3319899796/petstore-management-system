package com.ningcong.petstore.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 登录成功后返回给前端的信息。
 *
 * <p>permissions 同时驱动前端菜单显示（菜单级）与按钮级控制，
 * 后端接口再由 @RequirePerm 校验（接口级）。</p>
 */
@Data
@AllArgsConstructor
public class LoginVO {

    /** JWT，前端存 localStorage，后续请求放在 Authorization: Bearer xxx */
    private String token;

    private Integer userId;

    private String username;

    private Integer roleId;

    private String roleName;

    /** 权限码列表，管理员为 ["*"] */
    private List<String> permissions;

    /** 令牌有效期（秒） */
    private long expiresIn;
}
