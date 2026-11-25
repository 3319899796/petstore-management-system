package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.dto.VaccineDTO;
import com.ningcong.petstore.dto.VaccineQuery;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.service.VaccineService;
import com.ningcong.petstore.vo.VaccineVO;
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

import javax.validation.Valid;
import java.util.List;

/**
 * 免疫记录接口。
 */
@RestController
@RequestMapping("/vaccine")
@RequiredArgsConstructor
public class VaccineController {

    private final VaccineService vaccineService;

    @GetMapping("/page")
    @RequirePerm("vaccine:list")
    public Result<PageResult<VaccineVO>> page(VaccineQuery query) {
        return Result.ok(vaccineService.page(query));
    }

    @GetMapping("/pet/{petId}")
    @RequirePerm("vaccine:list")
    public Result<List<VaccineVO>> listByPet(@PathVariable Integer petId) {
        return Result.ok(vaccineService.listByPet(petId));
    }

    /** 到期提醒列表：未来 days 天内（默认 7 天）到期或已过期。 */
    @GetMapping("/upcoming")
    @RequirePerm("vaccine:list")
    public Result<List<VaccineVO>> upcoming(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(vaccineService.upcoming(days));
    }

    @PostMapping
    @RequirePerm("vaccine:add")
    public Result<Integer> create(@Valid @RequestBody VaccineDTO dto) {
        return Result.ok(vaccineService.create(dto));
    }

    @PutMapping
    @RequirePerm("vaccine:edit")
    public Result<Void> update(@Valid @RequestBody VaccineDTO dto) {
        vaccineService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{vaccineId}")
    @RequirePerm("vaccine:edit")
    public Result<Void> delete(@PathVariable Integer vaccineId) {
        vaccineService.delete(vaccineId);
        return Result.ok();
    }
}
