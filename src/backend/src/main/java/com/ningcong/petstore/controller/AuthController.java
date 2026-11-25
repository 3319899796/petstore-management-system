package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.dto.LoginDTO;
import com.ningcong.petstore.service.AuthService;
import com.ningcong.petstore.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证接口。
 *
 * <p>POST /api/auth/login 是唯一免登录接口；浏览器里返回的 token 由前端保存。</p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(authService.login(dto));
    }

    @GetMapping("/me")
    public Result<LoginVO> me() {
        return Result.ok(authService.current());
    }

    /**
     * 注销：JWT 无状态，服务端不保存会话，前端删掉 token 即可；
     * 保留这个接口是为了语义完整（将来要做黑名单再在这里落地）。
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
