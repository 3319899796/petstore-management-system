package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.security.LoginUser;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.security.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 两级鉴权的最小验证接口（论文"系统测试"一节的用例来源）。
 *
 * <ul>
 *   <li>GET /api/demo/ping        —— 只要带合法令牌就能访问（验证第一级）</li>
 *   <li>GET /api/demo/admin-only  —— 需要 user:list 权限，工作人员/主人访问应得 403（验证第二级）</li>
 * </ul>
 */
@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping("/ping")
    public Result<String> ping() {
        LoginUser user = UserContext.get();
        String who = user == null ? "匿名" : user.getUsername() + "（" + user.getRoleName() + "）";
        return Result.ok("你好，" + who + "，令牌有效。");
    }

    @GetMapping("/admin-only")
    @RequirePerm("user:list")
    public Result<String> adminOnly() {
        return Result.ok("只有拥有 user:list 权限的角色（管理员）能看到这句话。");
    }
}
