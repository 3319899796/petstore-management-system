package com.ningcong.petstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.PageQuery;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.dto.NoticeDTO;
import com.ningcong.petstore.entity.Notice;
import com.ningcong.petstore.mapper.NoticeMapper;
import com.ningcong.petstore.security.UserContext;
import com.ningcong.petstore.task.VaccineReminderTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 公告业务。
 *
 * <p>注意：公告表同时承载"疫苗到期自动提醒"（标题带【疫苗到期提醒】前缀），
 * 这是受"只用 9 张表"的约束所做的取舍，文档与论文里都要说明。</p>
 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final VaccineReminderTask vaccineReminderTask;

    public PageResult<Notice> page(PageQuery query) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        if (keyword != null) {
            wrapper.and(w -> w.like(Notice::getTitle, keyword).or().like(Notice::getContent, keyword));
        }
        wrapper.orderByDesc(Notice::getPublishTime).orderByDesc(Notice::getNoticeId);
        Page<Notice> page = noticeMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page);
    }

    public Notice getOrThrow(Integer noticeId) {
        Notice notice = noticeMapper.selectById(noticeId);
        if (notice == null) {
            throw new BusinessException("公告不存在（可能已被删除）");
        }
        return notice;
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer create(NoticeDTO dto) {
        Notice notice = new Notice();
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(StringUtils.hasText(dto.getContent()) ? dto.getContent().trim() : null);
        notice.setPublisherId(UserContext.get() == null ? null : UserContext.get().getUserId());
        notice.setPublishTime(LocalDateTime.now());
        noticeMapper.insert(notice);
        return notice.getNoticeId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(NoticeDTO dto) {
        Notice notice = getOrThrow(dto.getNoticeId());
        if (notice.getTitle().startsWith(VaccineReminderTask.TITLE_PREFIX)) {
            throw new BusinessException("系统自动生成的提醒不允许手工修改");
        }
        notice.setTitle(dto.getTitle().trim());
        notice.setContent(StringUtils.hasText(dto.getContent()) ? dto.getContent().trim() : null);
        noticeMapper.updateById(notice);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer noticeId) {
        getOrThrow(noticeId);
        noticeMapper.deleteById(noticeId);
    }

    /** 手动触发一次疫苗到期提醒（演示 / 答辩现场用，不用等到早上 9 点）。 */
    public int triggerReminder() {
        return vaccineReminderTask.runOnce();
    }
}
