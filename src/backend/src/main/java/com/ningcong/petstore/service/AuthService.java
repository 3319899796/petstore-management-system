package com.ningcong.petstore.service;

import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.ResultCode;
import com.ningcong.petstore.dto.LoginDTO;
import com.ningcong.petstore.entity.Role;
import com.ningcong.petstore.entity.SysUser;
import com.ningcong.petstore.mapper.RoleMapper;
import com.ningcong.petstore.mapper.SysUserMapper;
import com.ningcong.petstore.security.JwtUtil;
import com.ningcong.petstore.security.LoginUser;
import com.ningcong.petstore.security.UserContext;
import com.ningcong.petstore.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务：登录、取当前用户信息。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 登录：校验账号密码 → 取角色权限 → 签发 JWT。
     *
     * <p>账号不存在与密码错误返回同一句提示，避免被用来枚举账号。</p>
     */
    public LoginVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectByUsername(dto.getUsername().trim());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码不正确");
        }

        Role role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
        if (role == null) {
            throw new BusinessException("该账号未分配角色，请联系管理员");
        }

        List<String> permissions = splitPermissions(role.getPermission());
        String token = jwtUtil.createToken(user.getUserId(), user.getUsername(), role.getRoleId());
        return new LoginVO(token, user.getUserId(), user.getUsername(),
                role.getRoleId(), role.getRoleName(), permissions, jwtUtil.getExpireSeconds());
    }

    /** 当前登录用户（前端刷新页面后用它恢复身份与菜单）。 */
    public LoginVO current() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return new LoginVO(null, loginUser.getUserId(), loginUser.getUsername(),
                loginUser.getRoleId(), loginUser.getRoleName(),
                new ArrayList<>(loginUser.safePermissions()), jwtUtil.getExpireSeconds());
    }

    private List<String> splitPermissions(String permission) {
        if (!StringUtils.hasText(permission)) {
            return Collections.emptyList();
        }
        return Arrays.stream(permission.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
