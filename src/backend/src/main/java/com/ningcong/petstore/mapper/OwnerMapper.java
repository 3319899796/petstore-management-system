package com.ningcong.petstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ningcong.petstore.entity.Owner;

/**
 * 主人表 Mapper。查询条件用 MyBatis-Plus 的 LambdaQueryWrapper 在 service 里拼，
 * 这里保持空接口：没有自定义 SQL 就不写 SQL。
 */
public interface OwnerMapper extends BaseMapper<Owner> {
}
