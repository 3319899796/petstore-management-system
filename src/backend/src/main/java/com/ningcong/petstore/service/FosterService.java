package com.ningcong.petstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.common.ResultCode;
import com.ningcong.petstore.dto.FosterDTO;
import com.ningcong.petstore.dto.FosterQuery;
import com.ningcong.petstore.entity.Foster;
import com.ningcong.petstore.entity.Owner;
import com.ningcong.petstore.entity.Pet;
import com.ningcong.petstore.entity.SysUser;
import com.ningcong.petstore.mapper.FosterMapper;
import com.ningcong.petstore.mapper.OwnerMapper;
import com.ningcong.petstore.mapper.PetMapper;
import com.ningcong.petstore.mapper.SysUserMapper;
import com.ningcong.petstore.security.UserContext;
import com.ningcong.petstore.vo.FosterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 寄养业务 + 状态机。
 *
 * <p>状态流转只允许这几种，写死在 Service 里而不是交给前端：
 * PENDING → APPROVED / REJECTED；APPROVED → FINISHED。
 * 这样即使有人直接调接口，也不会出现"已结束的寄养又被驳回"这类脏数据。</p>
 */
@Service
@RequiredArgsConstructor
public class FosterService {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String FINISHED = "FINISHED";
    public static final String REJECTED = "REJECTED";

    private final FosterMapper fosterMapper;
    private final PetMapper petMapper;
    private final OwnerMapper ownerMapper;
    private final SysUserMapper sysUserMapper;
    private final PetService petService;

    public PageResult<FosterVO> page(FosterQuery query) {
        List<Integer> mine = petService.ownPetIdsOrNull();
        if (mine != null && mine.isEmpty()) {
            return PageResult.empty();
        }
        LambdaQueryWrapper<Foster> wrapper = new LambdaQueryWrapper<>();
        if (mine != null) {
            wrapper.in(Foster::getPetId, mine);
        }
        if (query.getPetId() != null) {
            if (mine != null && !mine.contains(query.getPetId())) {
                throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权查看该宠物的寄养记录");
            }
            wrapper.eq(Foster::getPetId, query.getPetId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Foster::getStatus, query.getStatus().trim());
        }
        wrapper.orderByDesc(Foster::getCreateTime).orderByDesc(Foster::getFosterId);

        Page<Foster> page = fosterMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page, toVoList(page.getRecords()));
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer create(FosterDTO dto) {
        Pet pet = petService.getOrThrow(dto.getPetId());
        petService.assertAccessible(pet);
        validateDates(dto);
        checkOverlap(dto.getPetId(), dto.getStartDate(), dto.getEndDate(), null);

        Foster foster = new Foster();
        foster.setPetId(dto.getPetId());
        foster.setStartDate(dto.getStartDate());
        foster.setEndDate(dto.getEndDate());
        foster.setStatus(PENDING);
        foster.setCreateTime(LocalDateTime.now());
        fosterMapper.insert(foster);
        return foster.getFosterId();
    }

    /** 只有待审核的申请可以改日期。 */
    @Transactional(rollbackFor = Exception.class)
    public void update(FosterDTO dto) {
        Foster exist = getOrThrow(dto.getFosterId());
        Pet pet = petService.getOrThrow(exist.getPetId());
        petService.assertAccessible(pet);
        if (!PENDING.equals(exist.getStatus())) {
            throw new BusinessException("只有「待审核」的寄养申请可以修改");
        }
        validateDates(dto);
        checkOverlap(exist.getPetId(), dto.getStartDate(), dto.getEndDate(), exist.getFosterId());

        exist.setStartDate(dto.getStartDate());
        exist.setEndDate(dto.getEndDate());
        fosterMapper.updateById(exist);
    }

    /** 审核：PENDING → APPROVED / REJECTED。 */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Integer fosterId, boolean approved, String reason) {
        Foster foster = getOrThrow(fosterId);
        if (!PENDING.equals(foster.getStatus())) {
            throw new BusinessException("该申请已处理过，不能重复审核");
        }
        if (!approved && !StringUtils.hasText(reason)) {
            throw new BusinessException("驳回时请填写原因");
        }
        foster.setStatus(approved ? APPROVED : REJECTED);
        foster.setHandlerId(currentUserId());
        fosterMapper.updateById(foster);
    }

    /** 结束寄养：APPROVED → FINISHED。 */
    @Transactional(rollbackFor = Exception.class)
    public void finish(Integer fosterId) {
        Foster foster = getOrThrow(fosterId);
        if (!APPROVED.equals(foster.getStatus())) {
            throw new BusinessException("只有「寄养中」的记录可以结束");
        }
        foster.setStatus(FINISHED);
        foster.setHandlerId(currentUserId());
        fosterMapper.updateById(foster);
    }

    /** 删除：已通过/已结束的记录要留档，不允许删。 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer fosterId) {
        Foster foster = getOrThrow(fosterId);
        Pet pet = petService.getOrThrow(foster.getPetId());
        petService.assertAccessible(pet);
        if (APPROVED.equals(foster.getStatus()) || FINISHED.equals(foster.getStatus())) {
            throw new BusinessException("已通过或已结束的寄养记录不能删除");
        }
        fosterMapper.deleteById(fosterId);
    }

    private Foster getOrThrow(Integer fosterId) {
        if (fosterId == null) {
            throw new BusinessException("寄养记录不能为空");
        }
        Foster foster = fosterMapper.selectById(fosterId);
        if (foster == null) {
            throw new BusinessException("寄养记录不存在（可能已被删除）");
        }
        return foster;
    }

    private void validateDates(FosterDTO dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        if (dto.getEndDate().isBefore(java.time.LocalDate.now())) {
            throw new BusinessException("结束日期不能早于今天");
        }
    }

    /** 同一只宠物，时间段不能和还在生效（待审核/寄养中）的记录重叠。 */
    private void checkOverlap(Integer petId, java.time.LocalDate start, java.time.LocalDate end, Integer selfId) {
        LambdaQueryWrapper<Foster> wrapper = new LambdaQueryWrapper<Foster>()
                .eq(Foster::getPetId, petId)
                .in(Foster::getStatus, PENDING, APPROVED)
                .le(Foster::getStartDate, end)
                .ge(Foster::getEndDate, start);
        if (selfId != null) {
            wrapper.ne(Foster::getFosterId, selfId);
        }
        if (fosterMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("该宠物在这段时间已有寄养安排，请换个日期");
        }
    }

    private Integer currentUserId() {
        return UserContext.get() == null ? null : UserContext.get().getUserId();
    }

    private String statusText(String status) {
        if (PENDING.equals(status)) {
            return "待审核";
        }
        if (APPROVED.equals(status)) {
            return "寄养中";
        }
        if (FINISHED.equals(status)) {
            return "已结束";
        }
        if (REJECTED.equals(status)) {
            return "已驳回";
        }
        return status;
    }

    private List<FosterVO> toVoList(List<Foster> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> petIds = records.stream().map(Foster::getPetId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Pet> petMap = petIds.isEmpty() ? Collections.emptyMap()
                : petMapper.selectBatchIds(petIds).stream()
                .collect(Collectors.toMap(Pet::getPetId, Function.identity(), (a, b) -> a));
        List<Integer> ownerIds = petMap.values().stream().map(Pet::getOwnerId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Owner> ownerMap = ownerIds.isEmpty() ? Collections.emptyMap()
                : ownerMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(Owner::getOwnerId, Function.identity(), (a, b) -> a));
        List<Integer> handlerIds = records.stream().map(Foster::getHandlerId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, SysUser> handlerMap = handlerIds.isEmpty() ? Collections.emptyMap()
                : sysUserMapper.selectBatchIds(handlerIds).stream()
                .collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (a, b) -> a));

        return records.stream().map(foster -> {
            FosterVO vo = new FosterVO();
            vo.setFosterId(foster.getFosterId());
            vo.setPetId(foster.getPetId());
            vo.setStartDate(foster.getStartDate());
            vo.setEndDate(foster.getEndDate());
            vo.setStatus(foster.getStatus());
            vo.setStatusText(statusText(foster.getStatus()));
            vo.setCreateTime(foster.getCreateTime());
            if (foster.getStartDate() != null && foster.getEndDate() != null) {
                vo.setDays(ChronoUnit.DAYS.between(foster.getStartDate(), foster.getEndDate()) + 1);
            }
            Pet pet = petMap.get(foster.getPetId());
            if (pet != null) {
                vo.setPetName(pet.getPetName());
                vo.setOwnerId(pet.getOwnerId());
                Owner owner = ownerMap.get(pet.getOwnerId());
                if (owner != null) {
                    vo.setOwnerName(owner.getOwnerName());
                }
            }
            SysUser handler = handlerMap.get(foster.getHandlerId());
            if (handler != null) {
                vo.setHandlerName(handler.getUsername());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
