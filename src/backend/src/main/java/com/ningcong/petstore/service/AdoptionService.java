package com.ningcong.petstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.ResultCode;
import com.ningcong.petstore.dto.AdoptionDTO;
import com.ningcong.petstore.dto.AdoptionQuery;
import com.ningcong.petstore.dto.FollowUpDTO;
import com.ningcong.petstore.entity.Adoption;
import com.ningcong.petstore.entity.Owner;
import com.ningcong.petstore.entity.Pet;
import com.ningcong.petstore.entity.SysUser;
import com.ningcong.petstore.mapper.AdoptionMapper;
import com.ningcong.petstore.mapper.OwnerMapper;
import com.ningcong.petstore.mapper.PetMapper;
import com.ningcong.petstore.mapper.SysUserMapper;
import com.ningcong.petstore.security.UserContext;
import com.ningcong.petstore.vo.AdoptionVO;
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
 * 领养业务：申请 → 审核 → 回访。
 *
 * <p>关键设计（答辩可讲）：领养通过时把 pet.owner_id 改成申请人，
 * 即"所有权转移"。这样 pet 表不用加"待领养"标志位，
 * 也和"一户多宠"的外键关系保持一致。</p>
 */
@Service
@RequiredArgsConstructor
public class AdoptionService {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    private final AdoptionMapper adoptionMapper;
    private final PetMapper petMapper;
    private final OwnerMapper ownerMapper;
    private final SysUserMapper sysUserMapper;
    private final PetService petService;
    private final OwnerService ownerService;

    public PageResult<AdoptionVO> page(AdoptionQuery query) {
        LambdaQueryWrapper<Adoption> wrapper = new LambdaQueryWrapper<>();
        Owner me = currentOwner();
        if (isOwnerRole()) {
            if (me == null) {
                return PageResult.empty();
            }
            // 主人只看自己提交的申请
            wrapper.eq(Adoption::getApplicantId, me.getOwnerId());
        }
        if (query.getPetId() != null) {
            wrapper.eq(Adoption::getPetId, query.getPetId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Adoption::getStatus, query.getStatus().trim());
        }
        wrapper.orderByDesc(Adoption::getApplyTime).orderByDesc(Adoption::getAdoptionId);

        Page<Adoption> page = adoptionMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page, toVoList(page.getRecords()));
    }

    /** 提交领养申请。 */
    @Transactional(rollbackFor = Exception.class)
    public Integer create(AdoptionDTO dto) {
        Pet pet = petService.getOrThrow(dto.getPetId());
        Owner me = currentOwner();
        if (me == null) {
            throw new BusinessException("当前账号还没有主人档案，请联系店内工作人员登记后再申请");
        }
        if (Objects.equals(pet.getOwnerId(), me.getOwnerId())) {
            throw new BusinessException("这只宠物已经在您名下，不需要再申请领养");
        }
        long pending = adoptionMapper.selectCount(new LambdaQueryWrapper<Adoption>()
                .eq(Adoption::getPetId, pet.getPetId())
                .in(Adoption::getStatus, PENDING, APPROVED));
        if (pending > 0) {
            throw new BusinessException("这只宠物已有待处理或已通过的领养申请");
        }

        Adoption adoption = new Adoption();
        adoption.setPetId(pet.getPetId());
        adoption.setApplicantId(me.getOwnerId());
        adoption.setStatus(PENDING);
        adoption.setApplyTime(LocalDateTime.now());
        adoptionMapper.insert(adoption);
        return adoption.getAdoptionId();
    }

    /**
     * 审核：通过时把宠物所有权转给申请人；驳回必须写原因。
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Integer adoptionId, boolean approved, String reason) {
        Adoption adoption = getOrThrow(adoptionId);
        if (!PENDING.equals(adoption.getStatus())) {
            throw new BusinessException("该申请已处理过，不能重复审核");
        }
        if (!approved && !StringUtils.hasText(reason)) {
            throw new BusinessException("驳回时请填写原因");
        }

        adoption.setStatus(approved ? APPROVED : REJECTED);
        adoption.setAuditTime(LocalDateTime.now());
        adoption.setAuditorId(currentUserId());
        if (!approved) {
            adoption.setRejectReason(reason.trim());
        }
        adoptionMapper.updateById(adoption);

        if (approved) {
            Pet pet = petService.getOrThrow(adoption.getPetId());
            // 所有权转移：宠物改挂到申请人名下（原有免疫/就诊记录仍跟着 pet_id 走）
            pet.setOwnerId(adoption.getApplicantId());
            petMapper.updateById(pet);
        }
    }

    /** 回访登记：只有审核通过的申请才能回访。 */
    @Transactional(rollbackFor = Exception.class)
    public void followUp(FollowUpDTO dto) {
        Adoption adoption = getOrThrow(dto.getAdoptionId());
        if (!APPROVED.equals(adoption.getStatus())) {
            throw new BusinessException("只有已通过审核的领养申请可以登记回访");
        }
        adoption.setFollowUpTime(LocalDateTime.now());
        adoption.setFollowUpResult(dto.getFollowUpResult().trim());
        adoptionMapper.updateById(adoption);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer adoptionId) {
        Adoption adoption = getOrThrow(adoptionId);
        if (!REJECTED.equals(adoption.getStatus())) {
            throw new BusinessException("只有已驳回的申请可以删除");
        }
        adoptionMapper.deleteById(adoptionId);
    }

    private Adoption getOrThrow(Integer adoptionId) {
        if (adoptionId == null) {
            throw new BusinessException("领养申请不能为空");
        }
        Adoption adoption = adoptionMapper.selectById(adoptionId);
        if (adoption == null) {
            throw new BusinessException("领养申请不存在（可能已被删除）");
        }
        return adoption;
    }

    private boolean isOwnerRole() {
        return UserContext.get() != null && Objects.equals(UserContext.get().getRoleId(), 3);
    }

    private Owner currentOwner() {
        if (UserContext.get() == null) {
            return null;
        }
        return ownerService.findByUserId(UserContext.get().getUserId());
    }

    private Integer currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }

    private String statusText(String status) {
        if (PENDING.equals(status)) {
            return "待审核";
        }
        if (APPROVED.equals(status)) {
            return "已通过";
        }
        if (REJECTED.equals(status)) {
            return "已驳回";
        }
        return status;
    }

    private List<AdoptionVO> toVoList(List<Adoption> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> petIds = records.stream().map(Adoption::getPetId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Pet> petMap = petIds.isEmpty() ? Collections.emptyMap()
                : petMapper.selectBatchIds(petIds).stream()
                .collect(Collectors.toMap(Pet::getPetId, Function.identity(), (a, b) -> a));
        // 注意：这里必须用可变的 ArrayList 并且只声明一次（不重新赋值），
        // 否则后面的 lambda 捕获不到"事实上不可变"的变量，编译会直接报错。
        List<Integer> ownerIds = petMap.values().stream().map(Pet::getOwnerId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toCollection(java.util.ArrayList::new));
        records.stream().map(Adoption::getApplicantId).filter(Objects::nonNull)
                .distinct().forEach(id -> {
                    if (!ownerIds.contains(id)) {
                        ownerIds.add(id);
                    }
                });
        Map<Integer, Owner> ownerMap = ownerIds.isEmpty() ? Collections.emptyMap()
                : ownerMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(Owner::getOwnerId, Function.identity(), (a, b) -> a));
        List<Integer> auditorIds = records.stream().map(Adoption::getAuditorId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, SysUser> auditorMap = auditorIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(auditorIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (a, b) -> a));

        return records.stream().map(adoption -> {
            AdoptionVO vo = new AdoptionVO();
            vo.setAdoptionId(adoption.getAdoptionId());
            vo.setPetId(adoption.getPetId());
            vo.setApplicantId(adoption.getApplicantId());
            vo.setStatus(adoption.getStatus());
            vo.setStatusText(statusText(adoption.getStatus()));
            vo.setApplyTime(adoption.getApplyTime());
            vo.setAuditTime(adoption.getAuditTime());
            vo.setRejectReason(adoption.getRejectReason());
            vo.setFollowUpTime(adoption.getFollowUpTime());
            vo.setFollowUpResult(adoption.getFollowUpResult());
            Pet pet = petMap.get(adoption.getPetId());
            if (pet != null) {
                vo.setPetName(pet.getPetName());
                Owner currentOwnerOfPet = ownerMap.get(pet.getOwnerId());
                if (currentOwnerOfPet != null) {
                    vo.setOwnerName(currentOwnerOfPet.getOwnerName());
                }
            }
            Owner applicant = ownerMap.get(adoption.getApplicantId());
            if (applicant != null) {
                vo.setApplicantName(applicant.getOwnerName());
            }
            SysUser auditor = auditorMap.get(adoption.getAuditorId());
            if (auditor != null) {
                vo.setAuditorName(auditor.getUsername());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
