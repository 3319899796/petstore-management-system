package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.dto.AdoptionDTO;
import com.ningcong.petstore.dto.AdoptionQuery;
import com.ningcong.petstore.dto.AuditDTO;
import com.ningcong.petstore.dto.FollowUpDTO;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.service.AdoptionService;
import com.ningcong.petstore.vo.AdoptionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 领养接口：申请 → 审核（通过即转移所有权）→ 回访。
 */
@RestController
@RequestMapping("/adoption")
@RequiredArgsConstructor
public class AdoptionController {

    private final AdoptionService adoptionService;

    @GetMapping("/page")
    @RequirePerm("adoption:list")
    public Result<PageResult<AdoptionVO>> page(AdoptionQuery query) {
        return Result.ok(adoptionService.page(query));
    }

    @PostMapping
    @RequirePerm("adoption:add")
    public Result<Integer> create(@Valid @RequestBody AdoptionDTO dto) {
        return Result.ok(adoptionService.create(dto));
    }

    @PostMapping("/audit")
    @RequirePerm("adoption:audit")
    public Result<Void> audit(@Valid @RequestBody AuditDTO dto) {
        adoptionService.audit(dto.getId(), Boolean.TRUE.equals(dto.getApproved()), dto.getReason());
        return Result.ok();
    }

    /** 回访登记（扩展字段写回 adoption 表，权限码 adoption:followup） */
    @PostMapping("/followup")
    @RequirePerm("adoption:followup")
    public Result<Void> followUp(@Valid @RequestBody FollowUpDTO dto) {
        adoptionService.followUp(dto);
        return Result.ok();
    }

    @DeleteMapping("/{adoptionId}")
    @RequirePerm({"adoption:audit", "adoption:add"})
    public Result<Void> delete(@PathVariable Integer adoptionId) {
        adoptionService.delete(adoptionId);
        return Result.ok();
    }
}
