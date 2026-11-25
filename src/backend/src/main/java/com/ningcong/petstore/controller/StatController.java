package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.service.StatService;
import com.ningcong.petstore.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 统计看板接口。
 */
@RestController
@RequestMapping("/stat")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/overview")
    @RequirePerm("stat:view")
    public Result<DashboardVO> overview() {
        return Result.ok(statService.overview());
    }
}
