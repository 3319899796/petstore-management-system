package com.ningcong.petstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ningcong.petstore.entity.Role;

/**
 * 角色表 Mapper。当前只需按主键取角色（selectById 由 BaseMapper 提供）。
 */
public interface RoleMapper extends BaseMapper<Role> {
}
