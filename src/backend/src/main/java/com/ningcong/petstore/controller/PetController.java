package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.dto.PetDTO;
import com.ningcong.petstore.dto.PetQuery;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.service.FileStorageService;
import com.ningcong.petstore.service.PetService;
import com.ningcong.petstore.vo.PetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

/**
 * 宠物档案接口。
 *
 * <p>照片是"先上传拿路径、再随表单提交"：POST /pet/upload 返回 /files/2026/11/xxx.jpg，
 * 前端把该字符串放进表单的 photoPath 字段。</p>
 */
@RestController
@RequestMapping("/pet")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final FileStorageService fileStorageService;

    @GetMapping("/page")
    @RequirePerm("pet:list")
    public Result<PageResult<PetVO>> page(PetQuery query) {
        return Result.ok(petService.page(query));
    }

    /** 下拉框用（登记寄养/领养时选宠物）。 */
    @GetMapping("/options")
    @RequirePerm("pet:list")
    public Result<List<PetVO>> options() {
        return Result.ok(petService.options());
    }

    @GetMapping("/{petId}")
    @RequirePerm("pet:view")
    public Result<PetVO> detail(@PathVariable Integer petId) {
        return Result.ok(petService.detail(petId));
    }

    @PostMapping
    @RequirePerm("pet:add")
    public Result<Integer> create(@Valid @RequestBody PetDTO dto) {
        return Result.ok(petService.create(dto));
    }

    @PutMapping
    @RequirePerm("pet:edit")
    public Result<Void> update(@Valid @RequestBody PetDTO dto) {
        petService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{petId}")
    @RequirePerm("pet:delete")
    public Result<Void> delete(@PathVariable Integer petId) {
        petService.delete(petId);
        return Result.ok();
    }

    /** 上传宠物照片，返回可访问路径。 */
    @PostMapping("/upload")
    @RequirePerm({"pet:add", "pet:edit"})
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileStorageService.store(file));
    }
}
