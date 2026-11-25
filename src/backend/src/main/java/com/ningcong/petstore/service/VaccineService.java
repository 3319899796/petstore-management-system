package com.ningcong.petstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.ResultCode;
import com.ningcong.petstore.dto.VaccineDTO;
import com.ningcong.petstore.dto.VaccineQuery;
import com.ningcong.petstore.entity.Owner;
import com.ningcong.petstore.entity.Pet;
import com.ningcong.petstore.entity.Vaccine;
import com.ningcong.petstore.mapper.OwnerMapper;
import com.ningcong.petstore.mapper.PetMapper;
import com.ningcong.petstore.mapper.VaccineMapper;
import com.ningcong.petstore.vo.VaccineVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 免疫记录业务。
 *
 * <p>核心规则：
 * 1. 下次到期日 = 接种日期 + 免疫周期（默认 365 天），手动填了就用手填的；
 * 2. 到期日发生变化时把 reminded 重置为 0，让提醒任务重新提醒一次；
 * 3. 主人只能看/改自己宠物的记录。</p>
 */
@Service
@RequiredArgsConstructor
public class VaccineService {

    /** 默认免疫周期：一年 */
    private static final int DEFAULT_CYCLE_DAYS = 365;

    private final VaccineMapper vaccineMapper;
    private final PetMapper petMapper;
    private final OwnerMapper ownerMapper;
    private final PetService petService;

    public PageResult<VaccineVO> page(VaccineQuery query) {
        List<Integer> mine = petService.ownPetIdsOrNull();
        if (mine != null && mine.isEmpty()) {
            return PageResult.empty();
        }

        LambdaQueryWrapper<Vaccine> wrapper = new LambdaQueryWrapper<>();
        if (mine != null) {
            wrapper.in(Vaccine::getPetId, mine);
        }
        if (query.getPetId() != null) {
            if (mine != null && !mine.contains(query.getPetId())) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权查看该宠物的免疫记录");
            }
            wrapper.eq(Vaccine::getPetId, query.getPetId());
        }

        if (Boolean.TRUE.equals(query.getDueSoon())) {
            // 提醒视图：7 天内到期 + 已过期的，按到期日升序，最紧急的排最前
            wrapper.isNotNull(Vaccine::getNextDueDate)
                    .le(Vaccine::getNextDueDate, LocalDate.now().plusDays(7))
                    .orderByAsc(Vaccine::getNextDueDate);
        } else {
            wrapper.orderByDesc(Vaccine::getInoculateDate).orderByDesc(Vaccine::getVaccineId);
        }

        Page<Vaccine> page = vaccineMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page, toVoList(page.getRecords()));
    }

    /** 某只宠物的全部免疫记录（宠物详情页用）。 */
    public List<VaccineVO> listByPet(Integer petId) {
        Pet pet = petService.getOrThrow(petId);
        petService.assertAccessible(pet);
        List<Vaccine> list = vaccineMapper.selectList(new LambdaQueryWrapper<Vaccine>()
                .eq(Vaccine::getPetId, petId)
                .orderByDesc(Vaccine::getInoculateDate));
        return toVoList(list);
    }

    /** 看板用：未来 days 天内到期（含已过期）的记录。 */
    public List<VaccineVO> upcoming(int days) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<Vaccine> wrapper = new LambdaQueryWrapper<Vaccine>()
                .isNotNull(Vaccine::getNextDueDate)
                .le(Vaccine::getNextDueDate, today.plusDays(Math.max(days, 1)))
                .orderByAsc(Vaccine::getNextDueDate);
        List<Integer> mine = petService.ownPetIdsOrNull();
        if (mine != null) {
            if (mine.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(Vaccine::getPetId, mine);
        }
        return toVoList(vaccineMapper.selectList(wrapper));
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer create(VaccineDTO dto) {
        Pet pet = petService.getOrThrow(dto.getPetId());
        petService.assertAccessible(pet);

        Vaccine vaccine = new Vaccine();
        vaccine.setPetId(dto.getPetId());
        vaccine.setVaccineType(dto.getVaccineType().trim());
        vaccine.setInoculateDate(dto.getInoculateDate());
        vaccine.setNextDueDate(resolveNextDueDate(dto));
        vaccine.setReminded(0);
        vaccineMapper.insert(vaccine);
        return vaccine.getVaccineId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(VaccineDTO dto) {
        Vaccine exist = getOrThrow(dto.getVaccineId());
        Pet pet = petService.getOrThrow(exist.getPetId());
        petService.assertAccessible(pet);

        LocalDate newDue = resolveNextDueDate(dto);
        boolean dueChanged = !Objects.equals(exist.getNextDueDate(), newDue);

        exist.setVaccineType(dto.getVaccineType().trim());
        exist.setInoculateDate(dto.getInoculateDate());
        exist.setNextDueDate(newDue);
        if (dueChanged) {
            // 到期日改了就重新提醒，否则用户改了日期也收不到新提醒
            exist.setReminded(0);
        }
        vaccineMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer vaccineId) {
        Vaccine exist = getOrThrow(vaccineId);
        Pet pet = petService.getOrThrow(exist.getPetId());
        petService.assertAccessible(pet);
        vaccineMapper.deleteById(vaccineId);
    }

    public Vaccine getOrThrow(Integer vaccineId) {
        if (vaccineId == null) {
            throw new BusinessException("免疫记录不能为空");
        }
        Vaccine vaccine = vaccineMapper.selectById(vaccineId);
        if (vaccine == null) {
            throw new BusinessException("免疫记录不存在（可能已被删除）");
        }
        return vaccine;
    }

    private LocalDate resolveNextDueDate(VaccineDTO dto) {
        if (dto.getNextDueDate() != null) {
            return dto.getNextDueDate();
        }
        int cycle = dto.getCycleDays() == null ? DEFAULT_CYCLE_DAYS : dto.getCycleDays();
        return dto.getInoculateDate().plusDays(cycle);
    }

    private List<VaccineVO> toVoList(List<Vaccine> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> petIds = records.stream().map(Vaccine::getPetId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Pet> petMap = petIds.isEmpty() ? Collections.emptyMap()
                : petMapper.selectBatchIds(petIds).stream()
                .collect(Collectors.toMap(Pet::getPetId, Function.identity(), (a, b) -> a));
        List<Integer> ownerIds = petMap.values().stream().map(Pet::getOwnerId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Owner> ownerMap = ownerIds.isEmpty() ? Collections.emptyMap()
                : ownerMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(Owner::getOwnerId, Function.identity(), (a, b) -> a));

        LocalDate today = LocalDate.now();
        return records.stream().map(vaccine -> {
            VaccineVO vo = new VaccineVO();
            vo.setVaccineId(vaccine.getVaccineId());
            vo.setPetId(vaccine.getPetId());
            vo.setVaccineType(vaccine.getVaccineType());
            vo.setInoculateDate(vaccine.getInoculateDate());
            vo.setNextDueDate(vaccine.getNextDueDate());
            vo.setReminded(vaccine.getReminded());
            if (vaccine.getNextDueDate() != null) {
                vo.setDaysLeft(ChronoUnit.DAYS.between(today, vaccine.getNextDueDate()));
            }
            Pet pet = petMap.get(vaccine.getPetId());
            if (pet != null) {
                vo.setPetName(pet.getPetName());
                Owner owner = ownerMap.get(pet.getOwnerId());
                if (owner != null) {
                    vo.setOwnerName(owner.getOwnerName());
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
