package com.ningcong.petstore.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果（只把前端需要的三个数给前端：总数、当前页数据、以及是否还有下一页由前端算）。
 *
 * <p>不直接把 MyBatis-Plus 的 Page 对象暴露出去，避免前端被迫理解后端的内部结构。</p>
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    /** 总记录数（用于前端分页器） */
    private long total;

    /** 当前页数据 */
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getRecords());
    }

    /** 实体分页 + 手工装配 VO 时用这个 */
    public static <T> PageResult<T> of(IPage<?> page, List<T> records) {
        return new PageResult<>(page.getTotal(), records);
    }

    /** 不分页的列表（下拉框等） */
    public static <T> PageResult<T> of(List<T> list) {
        return new PageResult<>(list == null ? 0 : list.size(),
                list == null ? Collections.emptyList() : list);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(0L, Collections.emptyList());
    }
}
