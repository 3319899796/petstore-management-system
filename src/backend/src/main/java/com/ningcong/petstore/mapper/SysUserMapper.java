package com.ningcong.petstore.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ningcong.petstore.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户表 Mapper。继承 BaseMapper 即可获得增删改查与条件构造器；
 * 需要"按账号查"这种高频语义时，显式写 @Select，比在 service 里拼 Wrapper 更直观。
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT * FROM sys_user WHERE username = #{username} LIMIT 1")
    SysUser selectByUsername(@Param("username") String username);
}
