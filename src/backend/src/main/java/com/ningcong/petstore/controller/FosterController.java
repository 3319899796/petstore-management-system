package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.dto.AuditDTO;
import com.ningcong.petstore.dto.FosterDTO;
import com.ningcong.petstore.dto.FosterQuery;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.service.FosterService;
import com.ningcong.petstore.vo.FosterVO;
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
 * 寄养接口：申请 → 审核 → 结束，状态由后端状态机控制。
 */
@RestController
@RequestMapping("/foster")
@RequiredArgsConstructor
public class FosterController {

    private final FosterService fosterService;

    @GetMapping("/page")
    @RequirePerm("foster:list")
    public Result<PageResult<FosterVO>> page(FosterQuery query) {
        return Result.ok(fosterService.page(query));
    }

    /** 主人和工作人员都能登记寄养（工作人员代客下单时用）。 */
    @PostMapping
    @RequirePerm({"foster:add", "foster:audit"})
    public Result<Integer> create(@Valid @RequestBody FosterDTO dto) {
        return Result.ok(fosterService.create(dto));
    }

    @PutMapping
    @RequirePerm({"foster:add", "foster:audit"})
    public Result<Void> update(@Valid @RequestBody FosterDTO dto) {
        fosterService.update(dto);
        return Result.ok();
    }

    @PostMapping("/audit")
    @RequirePerm("foster:audit")
    public Result<Void> audit(@Valid @RequestBody AuditDTO dto) {
        fosterService.audit(dto.getId(), Boolean.TRUE.equals(dto.getApproved()), dto.getReason());
        return Result.ok();
    }

    @PostMapping("/{fosterId}/finish")
    @RequirePerm("foster:audit")
    public Result<Void> finish(@PathVariable Integer fosterId) {
        fosterService.finish(fosterId);
        return Result.ok();
    }

    @DeleteMapping("/{fosterId}")
    @RequirePerm({"foster:add", "foster:audit"})
    public Result<Void> delete(@PathVariable Integer fosterId) {
        fosterService.delete(fosterId);
        return Result.ok();
    }
}
