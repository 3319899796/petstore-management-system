package com.ningcong.petstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.ResultCode;
import com.ningcong.petstore.dto.MedicalDTO;
import com.ningcong.petstore.dto.MedicalQuery;
import com.ningcong.petstore.entity.Medical;
import com.ningcong.petstore.entity.Owner;
import com.ningcong.petstore.entity.Pet;
import com.ningcong.petstore.mapper.MedicalMapper;
import com.ningcong.petstore.mapper.OwnerMapper;
import com.ningcong.petstore.mapper.PetMapper;
import com.ningcong.petstore.vo.MedicalVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 就诊记录业务：工作人员录入，本人宠物可查看。
 */
@Service
@RequiredArgsConstructor
public class MedicalService {

    private final MedicalMapper medicalMapper;
    private final PetMapper petMapper;
    private final OwnerMapper ownerMapper;
    private final PetService petService;

    public PageResult<MedicalVO> page(MedicalQuery query) {
        List<Integer> mine = petService.ownPetIdsOrNull();
        if (mine != null && mine.isEmpty()) {
            return PageResult.empty();
        }

        LambdaQueryWrapper<Medical> wrapper = new LambdaQueryWrapper<>();
        if (mine != null) {
            wrapper.in(Medical::getPetId, mine);
        }
        if (query.getPetId() != null) {
            if (mine != null && !mine.contains(query.getPetId())) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权查看该宠物的就诊记录");
            }
            wrapper.eq(Medical::getPetId, query.getPetId());
        }
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        if (keyword != null) {
            wrapper.and(w -> w.like(Medical::getSymptom, keyword).or().like(Medical::getDiagnosis, keyword));
        }
        wrapper.orderByDesc(Medical::getVisitDate).orderByDesc(Medical::getMedicalId);

        Page<Medical> page = medicalMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page, toVoList(page.getRecords()));
    }

    public List<MedicalVO> listByPet(Integer petId) {
        Pet pet = petService.getOrThrow(petId);
        petService.assertAccessible(pet);
        return toVoList(medicalMapper.selectList(new LambdaQueryWrapper<Medical>()
                .eq(Medical::getPetId, petId)
                .orderByDesc(Medical::getVisitDate)));
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer create(MedicalDTO dto) {
        Pet pet = petService.getOrThrow(dto.getPetId());
        petService.assertAccessible(pet);

        Medical medical = new Medical();
        medical.setPetId(dto.getPetId());
        medical.setSymptom(dto.getSymptom().trim());
        medical.setDiagnosis(StringUtils.hasText(dto.getDiagnosis()) ? dto.getDiagnosis().trim() : null);
        medical.setVisitDate(dto.getVisitDate());
        medicalMapper.insert(medical);
        return medical.getMedicalId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(MedicalDTO dto) {
        Medical exist = getOrThrow(dto.getMedicalId());
        Pet pet = petService.getOrThrow(exist.getPetId());
        petService.assertAccessible(pet);

        exist.setSymptom(dto.getSymptom().trim());
        exist.setDiagnosis(StringUtils.hasText(dto.getDiagnosis()) ? dto.getDiagnosis().trim() : null);
        exist.setVisitDate(dto.getVisitDate());
        medicalMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer medicalId) {
        Medical exist = getOrThrow(medicalId);
        Pet pet = petService.getOrThrow(exist.getPetId());
        petService.assertAccessible(pet);
        medicalMapper.deleteById(medicalId);
    }

    public Medical getOrThrow(Integer medicalId) {
        if (medicalId == null) {
            throw new BusinessException("就诊记录不能为空");
        }
        Medical medical = medicalMapper.selectById(medicalId);
        if (medical == null) {
            throw new BusinessException("就诊记录不存在（可能已被删除）");
        }
        return medical;
    }

    private List<MedicalVO> toVoList(List<Medical> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> petIds = records.stream().map(Medical::getPetId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Pet> petMap = petIds.isEmpty() ? Collections.emptyMap()
                : petMapper.selectBatchIds(petIds).stream()
                .collect(Collectors.toMap(Pet::getPetId, Function.identity(), (a, b) -> a));
        List<Integer> ownerIds = petMap.values().stream().map(Pet::getOwnerId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Owner> ownerMap = ownerIds.isEmpty() ? Collections.emptyMap()
                : ownerMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(Owner::getOwnerId, Function.identity(), (a, b) -> a));

        return records.stream().map(medical -> {
            MedicalVO vo = new MedicalVO();
            vo.setMedicalId(medical.getMedicalId());
            vo.setPetId(medical.getPetId());
            vo.setSymptom(medical.getSymptom());
            vo.setDiagnosis(medical.getDiagnosis());
            vo.setVisitDate(medical.getVisitDate());
            Pet pet = petMap.get(medical.getPetId());
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
