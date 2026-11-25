package com.ningcong.petstore.init;

import com.ningcong.petstore.entity.SysUser;
import com.ningcong.petstore.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 演示账号初始化：只在 sys_user 为空时执行一次。
 *
 * <p>密码在这里用 BCrypt 加密后入库，所以 schema.sql 里不用、也不应该出现明文或固定哈希。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String DEFAULT_PASSWORD = "123456";

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(String... args) {
        Long count = sysUserMapper.selectCount(null);
        if (count != null && count > 0) {
            log.info("sys_user 已有 {} 条数据，跳过演示账号初始化", count);
            return;
        }

        createUser("admin", "13800000001", 1);
        createUser("staff", "13800000002", 2);
        createUser("owner", "13800000003", 3);
        log.info("演示账号已创建：admin / staff / owner，初始密码 {}（上线前必须修改）", DEFAULT_PASSWORD);
    }

    private void createUser(String username, String phone, Integer roleId) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRoleId(roleId);
        user.setPhone(phone);
        user.setCreateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
    }
}
