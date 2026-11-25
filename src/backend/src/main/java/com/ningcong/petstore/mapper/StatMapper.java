package com.ningcong.petstore.mapper;

import com.ningcong.petstore.vo.DueDistributionVO;
import com.ningcong.petstore.vo.MonthCountVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 统计专用 Mapper：只有聚合查询，没有实体，所以不继承 BaseMapper。
 *
 * <p>为什么用 SQL 聚合而不是查出来在 Java 里算：数据量一大，
 * 把几万行拉到内存再统计是典型的性能反模式，交给数据库做 GROUP BY 更快。</p>
 */
public interface StatMapper {

    /** 指定日期之后，每个月新增的宠物档案数（近 6 个月用）。 */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS total "
            + "FROM pet WHERE create_time >= #{from} "
            + "GROUP BY DATE_FORMAT(create_time, '%Y-%m') ORDER BY month")
    List<MonthCountVO> countPetByMonth(@Param("from") LocalDate from);

    /** 领养申请按状态分组计数（算完成率用）。 */
    @Select("SELECT status, COUNT(*) AS cnt FROM adoption GROUP BY status")
    List<Map<String, Object>> countAdoptionByStatus();

    /** 免疫到期分布：逾期 / 7 天内 / 30 天内 / 更久。 */
    @Select("SELECT "
            + "COALESCE(SUM(CASE WHEN next_due_date < CURDATE() THEN 1 ELSE 0 END), 0) AS overdue, "
            + "COALESCE(SUM(CASE WHEN next_due_date >= CURDATE() AND next_due_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY) THEN 1 ELSE 0 END), 0) AS in7, "
            + "COALESCE(SUM(CASE WHEN next_due_date > DATE_ADD(CURDATE(), INTERVAL 7 DAY) AND next_due_date <= DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 1 ELSE 0 END), 0) AS in30, "
            + "COALESCE(SUM(CASE WHEN next_due_date > DATE_ADD(CURDATE(), INTERVAL 30 DAY) THEN 1 ELSE 0 END), 0) AS later "
            + "FROM vaccine WHERE next_due_date IS NOT NULL")
    DueDistributionVO dueDistribution();
}
