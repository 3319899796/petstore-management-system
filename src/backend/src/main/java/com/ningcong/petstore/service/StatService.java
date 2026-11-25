package com.ningcong.petstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ningcong.petstore.entity.Adoption;
import com.ningcong.petstore.entity.Foster;
import com.ningcong.petstore.entity.Vaccine;
import com.ningcong.petstore.mapper.AdoptionMapper;
import com.ningcong.petstore.mapper.FosterMapper;
import com.ningcong.petstore.mapper.OwnerMapper;
import com.ningcong.petstore.mapper.PetMapper;
import com.ningcong.petstore.mapper.StatMapper;
import com.ningcong.petstore.mapper.VaccineMapper;
import com.ningcong.petstore.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计分析业务：给首页看板提供聚合数据。
 */
@Service
@RequiredArgsConstructor
public class StatService {

    private final PetMapper petMapper;
    private final OwnerMapper ownerMapper;
    private final FosterMapper fosterMapper;
    private final AdoptionMapper adoptionMapper;
    private final VaccineMapper vaccineMapper;
    private final StatMapper statMapper;

    public DashboardVO overview() {
        DashboardVO vo = new DashboardVO();
        vo.setPetTotal(count(petMapper.selectCount(null)));
        vo.setOwnerTotal(count(ownerMapper.selectCount(null)));
        vo.setFosterPending(count(fosterMapper.selectCount(new LambdaQueryWrapper<Foster>()
                .eq(Foster::getStatus, FosterService.PENDING))));
        vo.setFosterActive(count(fosterMapper.selectCount(new LambdaQueryWrapper<Foster>()
                .eq(Foster::getStatus, FosterService.APPROVED))));
        vo.setAdoptionPending(count(adoptionMapper.selectCount(new LambdaQueryWrapper<Adoption>()
                .eq(Adoption::getStatus, AdoptionService.PENDING))));

        // 领养完成率 = 已通过 / 全部申请
        List<Map<String, Object>> statusRows = statMapper.countAdoptionByStatus();
        long total = 0;
        long approved = 0;
        for (Map<String, Object> row : statusRows) {
            long value = ((Number) row.get("cnt")).longValue();
            total += value;
            if (AdoptionService.APPROVED.equals(String.valueOf(row.get("status")))) {
                approved = value;
            }
        }
        vo.setAdoptionTotal(total);
        vo.setAdoptionApproved(approved);
        vo.setAdoptionRate(total == 0 ? 0.0 : Math.round(approved * 1000.0 / total) / 10.0);

        LocalDate today = LocalDate.now();
        vo.setVaccineDue7(count(vaccineMapper.selectCount(new LambdaQueryWrapper<Vaccine>()
                .isNotNull(Vaccine::getNextDueDate)
                .le(Vaccine::getNextDueDate, today.plusDays(7)))));
        vo.setVaccineOverdue(count(vaccineMapper.selectCount(new LambdaQueryWrapper<Vaccine>()
                .isNotNull(Vaccine::getNextDueDate)
                .lt(Vaccine::getNextDueDate, today))));

        // 近 6 个月（含本月）
        vo.setMonthlyPets(statMapper.countPetByMonth(today.minusMonths(5).withDayOfMonth(1)));
        vo.setDueDistribution(statMapper.dueDistribution());
        return vo;
    }

    private long count(Long value) {
        return value == null ? 0L : value;
    }
}
