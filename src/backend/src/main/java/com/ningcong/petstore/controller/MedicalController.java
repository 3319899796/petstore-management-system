package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.dto.MedicalDTO;
import com.ningcong.petstore.dto.MedicalQuery;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.service.MedicalService;
import com.ningcong.petstore.vo.MedicalVO;
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
 * 就诊记录接口。
 */
@RestController
@RequestMapping("/medical")
@RequiredArgsConstructor
public class MedicalController {

    private final MedicalService medicalService;

    @GetMapping("/page")
    @RequirePerm("medical:list")
    public Result<PageResult<MedicalVO>> page(MedicalQuery query) {
        return Result.ok(medicalService.page(query));
    }

    @GetMapping("/pet/{petId}")
    @RequirePerm("medical:list")
    public Result<List<MedicalVO>> listByPet(@PathVariable Integer petId) {
        return Result.ok(medicalService.listByPet(petId));
    }

    @PostMapping
    @RequirePerm("medical:add")
    public Result<Integer> create(@Valid @RequestBody MedicalDTO dto) {
        return Result.ok(medicalService.create(dto));
    }

    /** 修改/删除要求"新增或修改"任一权限（工作人员默认只有 medical:add）。 */
    @PutMapping
    @RequirePerm({"medical:add", "medical:edit"})
    public Result<Void> update(@Valid @RequestBody MedicalDTO dto) {
        medicalService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{medicalId}")
    @RequirePerm({"medical:add", "medical:edit"})
    public Result<Void> delete(@PathVariable Integer medicalId) {
        medicalService.delete(medicalId);
        return Result.ok();
    }
}
