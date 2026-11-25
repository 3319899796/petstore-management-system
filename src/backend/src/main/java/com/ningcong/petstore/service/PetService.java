package com.ningcong.petstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.ResultCode;
import com.ningcong.petstore.dto.PetDTO;
import com.ningcong.petstore.dto.PetQuery;
import com.ningcong.petstore.entity.Owner;
import com.ningcong.petstore.entity.Pet;
import com.ningcong.petstore.mapper.OwnerMapper;
import com.ningcong.petstore.mapper.PetMapper;
import com.ningcong.petstore.security.LoginUser;
import com.ningcong.petstore.security.UserContext;
import com.ningcong.petstore.util.MaskUtil;
import com.ningcong.petstore.vo.PetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 宠物档案业务。
 *
 * <p>本模块是"数据隔离"最典型的地方：工作人员看全店，宠物主人只能看自己的。</p>
 */
@Service
@RequiredArgsConstructor
public class PetService {

    /** 与 schema.sql 中 role_id=3「宠物主人」对应 */
    private static final int ROLE_OWNER = 3;

    private final PetMapper petMapper;
    private final OwnerMapper ownerMapper;
    private final OwnerService ownerService;

    /** 分页查询：主人角色自动只查自己的宠物。 */
    public PageResult<PetVO> page(PetQuery query) {
        LoginUser user = UserContext.get();
        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<>();

        if (isOwner(user)) {
            Owner me = ownerService.findByUserId(user.getUserId());
            if (me == null) {
                return PageResult.empty();
            }
            wrapper.eq(Pet::getOwnerId, me.getOwnerId());
        } else if (query.getOwnerId() != null) {
            wrapper.eq(Pet::getOwnerId, query.getOwnerId());
        }

        if (StringUtils.hasText(query.getBreed())) {
            wrapper.eq(Pet::getBreed, query.getBreed().trim());
        }
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        if (keyword != null) {
            wrapper.and(w -> w.like(Pet::getPetName, keyword).or().like(Pet::getChipNo, keyword));
        }
        wrapper.orderByDesc(Pet::getCreateTime).orderByDesc(Pet::getPetId);

        Page<Pet> page = petMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page, toVoList(page.getRecords()));
    }

    public PetVO detail(Integer petId) {
        Pet pet = getOrThrow(petId);
        assertAccessible(pet);
        return toVoList(Collections.singletonList(pet)).get(0);
    }

    /** 下拉框用：主人只返回自己的宠物，工作人员返回全部。 */
    public List<PetVO> options() {
        LoginUser user = UserContext.get();
        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<>();
        if (isOwner(user)) {
            Owner me = ownerService.findByUserId(user.getUserId());
            if (me == null) {
                return Collections.emptyList();
            }
            wrapper.eq(Pet::getOwnerId, me.getOwnerId());
        }
        wrapper.orderByDesc(Pet::getPetId);
        return toVoList(petMapper.selectList(wrapper));
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer create(PetDTO dto) {
        ownerService.getOrThrow(dto.getOwnerId());
        if (isOwner(UserContext.get())) {
            Owner me = ownerService.findByUserId(UserContext.get().getUserId());
            if (me == null || !me.getOwnerId().equals(dto.getOwnerId())) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "只能给自己名下的宠物建档");
            }
        }
        checkChipUnique(dto.getChipNo(), null);

        Pet pet = new Pet();
        pet.setPetName(dto.getPetName().trim());
        pet.setChipNo(dto.getChipNo().trim());
        pet.setBreed(trimToNull(dto.getBreed()));
        pet.setOwnerId(dto.getOwnerId());
        pet.setPhotoPath(trimToNull(dto.getPhotoPath()));
        pet.setCreateTime(LocalDateTime.now());
        petMapper.insert(pet);
        return pet.getPetId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(PetDTO dto) {
        Pet pet = getOrThrow(dto.getPetId());
        assertAccessible(pet);
        if (isOwner(UserContext.get()) && !Objects.equals(pet.getOwnerId(), dto.getOwnerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "不能把宠物转到其他人名下");
        }
        checkChipUnique(dto.getChipNo(), pet.getPetId());

        pet.setPetName(dto.getPetName().trim());
        pet.setChipNo(dto.getChipNo().trim());
        pet.setBreed(trimToNull(dto.getBreed()));
        pet.setOwnerId(dto.getOwnerId());
        if (dto.getPhotoPath() != null) {
            pet.setPhotoPath(trimToNull(dto.getPhotoPath()));
        }
        petMapper.updateById(pet);
    }

    /**
     * 删除宠物档案。若已有免疫/就诊/寄养/领养记录，数据库外键会拦住，
     * 由 GlobalExceptionHandler 翻译成"数据被引用，无法删除"的提示。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer petId) {
        Pet pet = getOrThrow(petId);
        assertAccessible(pet);
        petMapper.deleteById(petId);
    }

    /**
     * 数据隔离工具：主人角色返回自己名下宠物 id 列表（可能为空），
     * 工作人员/管理员返回 null 表示"不做限制"。
     *
     * <p>免疫、就诊、寄养、领养等子模块都用它来做同一套隔离，避免每个模块各写一遍。</p>
     */
    public List<Integer> ownPetIdsOrNull() {
        LoginUser user = UserContext.get();
        if (!isOwner(user)) {
            return null;
        }
        Owner me = ownerService.findByUserId(user.getUserId());
        if (me == null) {
            return Collections.emptyList();
        }
        return petMapper.selectList(new LambdaQueryWrapper<Pet>()
                        .select(Pet::getPetId)
                        .eq(Pet::getOwnerId, me.getOwnerId()))
                .stream().map(Pet::getPetId).collect(Collectors.toList());
    }

    public Pet getOrThrow(Integer petId) {
        if (petId == null) {
            throw new BusinessException("宠物不能为空");
        }
        Pet pet = petMapper.selectById(petId);
        if (pet == null) {
            throw new BusinessException("宠物不存在（可能已被删除）");
        }
        return pet;
    }

    /** 主人角色访问别人的宠物 → 403。 */
    public void assertAccessible(Pet pet) {
        LoginUser user = UserContext.get();
        if (!isOwner(user)) {
            return;
        }
        Owner me = ownerService.findByUserId(user.getUserId());
        if (me == null || !Objects.equals(me.getOwnerId(), pet.getOwnerId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权查看该宠物档案");
        }
    }

    private boolean isOwner(LoginUser user) {
        return user != null && Objects.equals(user.getRoleId(), ROLE_OWNER);
    }

    private void checkChipUnique(String chipNo, Integer selfPetId) {
        Pet other = petMapper.selectOne(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getChipNo, chipNo.trim()).last("LIMIT 1"));
        if (other != null && !other.getPetId().equals(selfPetId)) {
            throw new BusinessException("芯片号「" + chipNo + "」已存在，不能重复建档");
        }
    }

    /** 批量补主人信息，避免 N+1 查询。 */
    private List<PetVO> toVoList(List<Pet> pets) {
        if (pets == null || pets.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ownerIds = pets.stream().map(Pet::getOwnerId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Owner> ownerMap = ownerIds.isEmpty() ? Collections.emptyMap()
                : ownerMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(Owner::getOwnerId, Function.identity(), (a, b) -> a));

        return pets.stream().map(pet -> {
            PetVO vo = new PetVO();
            vo.setPetId(pet.getPetId());
            vo.setPetName(pet.getPetName());
            vo.setChipNo(pet.getChipNo());
            vo.setBreed(pet.getBreed());
            vo.setOwnerId(pet.getOwnerId());
            vo.setPhotoPath(pet.getPhotoPath());
            vo.setCreateTime(pet.getCreateTime());
            Owner owner = ownerMap.get(pet.getOwnerId());
            if (owner != null) {
                vo.setOwnerName(owner.getOwnerName());
                vo.setOwnerPhone(MaskUtil.maskPhone(owner.getPhone()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
