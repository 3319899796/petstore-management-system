package com.ningcong.petstore.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宠物档案。chip_no 唯一：一只宠物只能建一次档。
 */
@Data
@TableName("pet")
public class Pet {

    @TableId(type = IdType.AUTO)
    private Integer petId;

    private String petName;

    /** 芯片号，唯一索引 */
    private String chipNo;

    private String breed;

    /** 所属主人 */
    private Integer ownerId;

    /** 照片相对路径（如 /files/2026/11/uuid.jpg），不存二进制 */
    private String photoPath;

    private LocalDateTime createTime;
}
