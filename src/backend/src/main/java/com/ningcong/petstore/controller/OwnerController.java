package com.ningcong.petstore.controller;

import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.PageQuery;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.Result;
import com.ningcong.petstore.dto.OwnerDTO;
import com.ningcong.petstore.entity.Owner;
import com.ningcong.petstore.security.LoginUser;
import com.ningcong.petstore.security.RequirePerm;
import com.ningcong.petstore.security.UserContext;
import com.ningcong.petstore.service.OwnerService;
import com.ningcong.petstore.vo.OwnerVO;
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
 * 主人档案接口。
 */
@RestController
@RequestMapping("/owner")
@RequiredArgsConstructor
public class OwnerController {

    private final OwnerService ownerService;

    /** 分页列表（工作人员/管理员）。 */
    @GetMapping("/page")
    @RequirePerm("owner:list")
    public Result<PageResult<OwnerVO>> page(PageQuery query) {
        return Result.ok(ownerService.page(query));
    }

    /** 下拉选项（建档时选主人）。 */
    @GetMapping("/options")
    @RequirePerm("owner:list")
    public Result<List<OwnerVO>> options() {
        return Result.ok(ownerService.options());
    }

    /**
     * 当前登录账号绑定的主人档案（宠物主人登录后用它拿到自己的 ownerId）。
     * 不加 @RequirePerm：任何登录用户都能看"我是谁"。
     */
    @GetMapping("/me")
    public Result<OwnerVO> me() {
        LoginUser user = UserContext.get();
        Owner owner = ownerService.findByUserId(user == null ? null : user.getUserId());
        if (owner == null) {
            throw new BusinessException("当前账号还没有绑定主人档案，请联系店内工作人员登记");
        }
        OwnerVO vo = new OwnerVO();
        vo.setOwnerId(owner.getOwnerId());
        vo.setOwnerName(owner.getOwnerName());
        vo.setPhone(com.ningcong.petstore.util.MaskUtil.maskPhone(owner.getPhone()));
        vo.setUserId(owner.getUserId());
        return Result.ok(vo);
    }

    @PostMapping
    @RequirePerm("owner:edit")
    public Result<Integer> create(@Valid @RequestBody OwnerDTO dto) {
        return Result.ok(ownerService.create(dto));
    }

    @PutMapping
    @RequirePerm("owner:edit")
    public Result<Void> update(@Valid @RequestBody OwnerDTO dto) {
        ownerService.update(dto);
        return Result.ok();
    }

    @DeleteMapping("/{ownerId}")
    @RequirePerm("owner:edit")
    public Result<Void> delete(@PathVariable Integer ownerId) {
        ownerService.delete(ownerId);
        return Result.ok();
    }
}
