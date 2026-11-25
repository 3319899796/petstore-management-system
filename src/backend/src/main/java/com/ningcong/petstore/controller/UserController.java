package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.PageQuery;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.dto.UserDTO;
import com.ningcong.petstore.entity.Role;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.service.UserService;
import com.ningcong.petstore.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 用户与角色管理接口（管理员专属，权限码 user:list / user:edit，管理员持有 * 全部权限）。
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/page")
    @RequirePerm("user:list")
    public Result<PageResult<UserVO>> page(PageQuery query) {
        return Result.ok(userService.page(query));
    }

    @GetMapping("/roles")
    @RequirePerm("user:list")
    public Result<List<Role>> roles() {
        return Result.ok(userService.roles());
    }

    @PostMapping
    @RequirePerm("user:edit")
    public Result<Integer> create(@Valid @RequestBody UserDTO dto) {
        return Result.ok(userService.create(dto));
    }

    @PutMapping
    @RequirePerm("user:edit")
    public Result<Void> update(@Valid @RequestBody UserDTO dto) {
        userService.update(dto);
        return Result.ok();
    }

    @PostMapping("/{userId}/reset-password")
    @RequirePerm("user:edit")
    public Result<Void> resetPassword(@PathVariable Integer userId) {
        userService.resetPassword(userId);
        return Result.ok();
    }

    @DeleteMapping("/{userId}")
    @RequirePerm("user:edit")
    public Result<Void> delete(@PathVariable Integer userId) {
        userService.delete(userId);
        return Result.ok();
    }
}
