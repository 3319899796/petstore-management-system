package com.ningcong.petstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.PageQuery;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.dto.OwnerDTO;
import com.ningcong.petstore.entity.Owner;
import com.ningcong.petstore.entity.Pet;
import com.ningcong.petstore.mapper.OwnerMapper;
import com.ningcong.petstore.mapper.PetMapper;
import com.ningcong.petstore.util.MaskUtil;
import com.ningcong.petstore.vo.OwnerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 主人档案业务。
 */
@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerMapper ownerMapper;
    private final PetMapper petMapper;

    /** 分页 + 按姓名/手机号模糊搜索。 */
    public PageResult<OwnerVO> page(PageQuery query) {
        LambdaQueryWrapper<Owner> wrapper = new LambdaQueryWrapper<>();
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        if (keyword != null) {
            wrapper.and(w -> w.like(Owner::getOwnerName, keyword).or().like(Owner::getPhone, keyword));
        }
        wrapper.orderByDesc(Owner::getOwnerId);

        Page<Owner> page = ownerMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        List<OwnerVO> records = page.getRecords().stream().map(this::toVo).collect(Collectors.toList());
        return PageResult.of(page, records);
    }

    /** 下拉框用：全部主人（不脱敏手机号，只给姓名用）。 */
    public List<OwnerVO> options() {
        return ownerMapper.selectList(new LambdaQueryWrapper<Owner>().orderByDesc(Owner::getOwnerId))
                .stream().map(this::toVo).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer create(OwnerDTO dto) {
        checkUserFree(dto.getUserId(), null);
        Owner owner = new Owner();
        owner.setOwnerName(dto.getOwnerName().trim());
        owner.setPhone(trimToNull(dto.getPhone()));
        owner.setUserId(dto.getUserId());
        ownerMapper.insert(owner);
        return owner.getOwnerId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(OwnerDTO dto) {
        Owner owner = getOrThrow(dto.getOwnerId());
        checkUserFree(dto.getUserId(), owner.getOwnerId());
        owner.setOwnerName(dto.getOwnerName().trim());
        owner.setPhone(trimToNull(dto.getPhone()));
        owner.setUserId(dto.getUserId());
        ownerMapper.updateById(owner);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer ownerId) {
        getOrThrow(ownerId);
        Long pets = petMapper.selectCount(new LambdaQueryWrapper<Pet>().eq(Pet::getOwnerId, ownerId));
        if (pets != null && pets > 0) {
            throw new BusinessException("该主人名下还有 " + pets + " 只宠物，请先移交给其他主人或删除宠物档案");
        }
        ownerMapper.deleteById(ownerId);
    }

    /** 宠物主人登录后，靠它反查"我"是谁。 */
    public Owner findByUserId(Integer userId) {
        if (userId == null) {
            return null;
        }
        return ownerMapper.selectOne(new LambdaQueryWrapper<Owner>().eq(Owner::getUserId, userId).last("LIMIT 1"));
    }

    public Owner getOrThrow(Integer ownerId) {
        if (ownerId == null) {
            throw new BusinessException("主人不能为空");
        }
        Owner owner = ownerMapper.selectById(ownerId);
        if (owner == null) {
            throw new BusinessException("主人不存在（可能已被删除）");
        }
        return owner;
    }

    /** 一个登录账号只能绑定一个主人档案。 */
    private void checkUserFree(Integer userId, Integer selfOwnerId) {
        if (userId == null) {
            return;
        }
        Owner other = ownerMapper.selectOne(
                new LambdaQueryWrapper<Owner>().eq(Owner::getUserId, userId).last("LIMIT 1"));
        if (other != null && !other.getOwnerId().equals(selfOwnerId)) {
            throw new BusinessException("该登录账号已绑定其他主人档案");
        }
    }

    private OwnerVO toVo(Owner owner) {
        OwnerVO vo = new OwnerVO();
        vo.setOwnerId(owner.getOwnerId());
        vo.setOwnerName(owner.getOwnerName());
        vo.setPhone(MaskUtil.maskPhone(owner.getPhone()));
        vo.setUserId(owner.getUserId());
        return vo;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
