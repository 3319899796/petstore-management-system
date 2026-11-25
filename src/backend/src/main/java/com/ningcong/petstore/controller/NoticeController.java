package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.PageQuery;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.dto.NoticeDTO;
import com.ningcong.petstore.entity.Notice;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.service.NoticeService;
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

/**
 * 公告接口。
 *
 * <p>查看：三类角色都有 notice:list；发布/修改/删除：按开题报告（二）角色划分，
 * 只有管理员（权限 *）能发布公告，所以权限码用 notice:publish。</p>
 */
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/page")
    @RequirePerm("notice:list")
    public Result<PageResult<Notice>> page(PageQuery query) {
        return Result.ok(noticeService.page(query));
    }

    @GetMapping("/{noticeId}")
    @RequirePerm("notice:list")
    public Result<Notice> detail(@PathVariable Integer noticeId) {
        return Result.ok(noticeService.getOrThrow(noticeId));
    }

    @PostMapping
    @RequirePerm("notice:publish")
    public Result<Integer> create(@Valid @RequestBody NoticeDTO dto) {
        return Result.ok(noticeService.create(dto));
    }

    @PutMapping
    @RequirePerm("notice:publish")
    public Result<Void> update(@Valid @RequestBody NoticeDTO dto) {
        noticeService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{noticeId}")
    @RequirePerm("notice:publish")
    public Result<Void> delete(@PathVariable Integer noticeId) {
        noticeService.delete(noticeId);
        return Result.ok();
    }

    /** 手动触发一次疫苗到期提醒（答辩演示用，不必等到早上 9 点）。 */
    @PostMapping("/remind")
    @RequirePerm({"notice:publish", "vaccine:edit"})
    public Result<Integer> remind() {
        return Result.ok(noticeService.triggerReminder());
    }
}
