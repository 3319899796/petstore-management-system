package com.ningcong.petstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ningcong.petstore.entity.Vaccine;

/**
 * 免疫记录 Mapper。通用 CRUD 由 MyBatis-Plus 的 BaseMapper 提供，
 * 这里不需要写任何 SQL；复杂查询用 LambdaQueryWrapper 在 Service 里拼。
 */
public interface VaccineMapper extends BaseMapper<Vaccine> {
}
