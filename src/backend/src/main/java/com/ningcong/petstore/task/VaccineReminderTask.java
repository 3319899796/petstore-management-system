package com.ningcong.petstore.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ningcong.petstore.entity.Notice;
import com.ningcong.petstore.entity.Owner;
import com.ningcong.petstore.entity.Pet;
import com.ningcong.petstore.entity.Vaccine;
import com.ningcong.petstore.mapper.NoticeMapper;
import com.ningcong.petstore.mapper.OwnerMapper;
import com.ningcong.petstore.mapper.PetMapper;
import com.ningcong.petstore.mapper.VaccineMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 疫苗到期提醒定时任务。
 *
 * <p>为什么用 Spring Task 而不是 Quartz / XXL-Job：需求只有"每天扫一次表发提醒"，
 * 单体应用自带 @Scheduled 足够，引分布式调度框架会把部署复杂度抬上去，
 * 开题报告里也写明"不引入分布式框架"，答辩时这一点是要解释清楚的取舍。</p>
 *
 * <p>幂等做法：提醒成功就把该条记录的 reminded 置 1，
 * 所以即使服务重启或多实例部署，同一条免疫记录也不会重复发公告。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VaccineReminderTask {

    /** 提前多少天提醒 */
    private static final int REMIND_AHEAD_DAYS = 7;

    /** 系统提醒在公告表里的标题前缀，前端据此筛选 */
    public static final String TITLE_PREFIX = "【疫苗到期提醒】";

    private final VaccineMapper vaccineMapper;
    private final PetMapper petMapper;
    private final OwnerMapper ownerMapper;
    private final NoticeMapper noticeMapper;

    /** 每天上午 9 点执行（cron: 秒 分 时 日 月 周） */
    @Scheduled(cron = "0 0 9 * * ?")
    public void scheduled() {
        int count = runOnce();
        log.info("疫苗到期提醒任务完成，本次生成公告 {} 条", count);
    }

    /**
     * 执行一次提醒扫描，返回本次生成的公告条数。
     * 定时器会调用它；也可以由管理员手动触发（演示/测试很方便）。
     */
    public int runOnce() {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(REMIND_AHEAD_DAYS);

        List<Vaccine> dueList = vaccineMapper.selectList(new LambdaQueryWrapper<Vaccine>()
                .eq(Vaccine::getReminded, 0)
                .isNotNull(Vaccine::getNextDueDate)
                .le(Vaccine::getNextDueDate, deadline)
                .orderByAsc(Vaccine::getNextDueDate));
        if (dueList.isEmpty()) {
            return 0;
        }

        List<Integer> petIds = dueList.stream().map(Vaccine::getPetId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Pet> petMap = petIds.isEmpty() ? Collections.emptyMap()
                : petMapper.selectBatchIds(petIds).stream()
                .collect(Collectors.toMap(Pet::getPetId, Function.identity(), (a, b) -> a));
        List<Integer> ownerIds = petMap.values().stream().map(Pet::getOwnerId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Owner> ownerMap = ownerIds.isEmpty() ? Collections.emptyMap()
                : ownerMapper.selectBatchIds(ownerIds).stream()
                .collect(Collectors.toMap(Owner::getOwnerId, Function.identity(), (a, b) -> a));

        int generated = 0;
        for (Vaccine vaccine : dueList) {
            Pet pet = petMap.get(vaccine.getPetId());
            if (pet == null) {
                // 宠物档案被删了，这条记录留着也没意义，标记已处理避免反复扫
                vaccine.setReminded(1);
                vaccineMapper.updateById(vaccine);
                continue;
            }
            Owner owner = ownerMap.get(pet.getOwnerId());

            Notice notice = new Notice();
            notice.setTitle(TITLE_PREFIX + pet.getPetName() + " 该接种" + vaccine.getVaccineType() + "了");
            notice.setContent(buildContent(pet, owner, vaccine, today));
            notice.setPublisherId(null);
            notice.setPublishTime(LocalDateTime.now());
            noticeMapper.insert(notice);

            vaccine.setReminded(1);
            vaccineMapper.updateById(vaccine);
            generated++;
        }
        return generated;
    }

    private String buildContent(Pet pet, Owner owner, Vaccine vaccine, LocalDate today) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(today, vaccine.getNextDueDate());
        String when = days < 0
                ? "已逾期 " + (-days) + " 天"
                : (days == 0 ? "今天到期" : "还有 " + days + " 天到期");
        return "宠物：" + pet.getPetName()
                + (pet.getBreed() == null ? "" : "（" + pet.getBreed() + "）")
                + "\n主人：" + (owner == null ? "未知" : owner.getOwnerName()
                + (owner.getPhone() == null ? "" : " " + owner.getPhone()))
                + "\n疫苗：" + vaccine.getVaccineType()
                + "\n上次接种：" + vaccine.getInoculateDate()
                + "\n下次到期：" + vaccine.getNextDueDate() + "（" + when + "）"
                + "\n请提前联系主人安排接种。";
    }
}
