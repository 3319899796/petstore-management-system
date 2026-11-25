package com.ningcong.petstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 就诊记录（表 medical）。与免疫记录分开存，符合第三范式：
 * 一次就诊可能同时包含检查和用药，写在一行里更贴近实际业务。
 */
@Data
@TableName("medical")
public class Medical {

    @TableId(type = IdType.AUTO)
    private Integer medicalId;

    private Integer petId;

    /** 症状描述 */
    private String symptom;

    /** 诊断结果 */
    private String diagnosis;

    /** 就诊日期 */
    private LocalDate visitDate;
}
