package com.ningcong.petstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 免疫记录（表 vaccine）。一只宠物可以有多条，按月龄周期重复接种。
 */
@Data
@TableName("vaccine")
public class Vaccine {

    @TableId(type = IdType.AUTO)
    private Integer vaccineId;

    /** 外键：哪只宠物 */
    private Integer petId;

    /** 疫苗种类，如 狂犬疫苗 / 猫三联 / 犬四联 */
    private String vaccineType;

    /** 接种日期 */
    private LocalDate inoculateDate;

    /** 下次到期日（按免疫周期自动推算） */
    private LocalDate nextDueDate;

    /** 提醒去重标记：0 未提醒，1 已提醒（定时任务写 1，避免每天重复发） */
    private Integer reminded;
}
