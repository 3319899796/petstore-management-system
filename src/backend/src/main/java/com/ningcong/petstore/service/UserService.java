package com.ningcong.petstore.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.PageQuery;
import com.ningcong.petstore.common.PageResult;
import com.ningcong.petstore.dto.UserDTO;
import com.ningcong.petstore.entity.Role;
import com.ningcong.petstore.entity.SysUser;
import com.ningcong.petstore.mapper.RoleMapper;
import com.ningcong.petstore.mapper.SysUserMapper;
import com.ningcong.petstore.security.UserContext;
import com.ningcong.petstore.util.MaskUtil;
import com.ningcong.petstore.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户与角色管理（管理员专属）。
 *
 * <p>两个安全规则：不能删除自己；不能把最后一个管理员降级，
 * 否则系统会变成"没人能管"的死锁状态。</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /** 管理员角色主键（与 schema.sql 的 role 初始化数据一致） */
    private static final int ROLE_ADMIN = 1;

    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public PageResult<UserVO> page(PageQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        if (keyword != null) {
            wrapper.like(SysUser::getUsername, keyword);
        }
        wrapper.orderByAsc(SysUser::getUserId);

        Page<SysUser> page = sysUserMapper.selectPage(
                new Page<>(query.safePageNum(), query.safePageSize()), wrapper);
        return PageResult.of(page, toVoList(page.getRecords()));
    }

    /** 角色下拉选项（新增用户时选角色）。 */
    public List<Role> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getRoleId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer create(UserDTO dto) {
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new BusinessException("新增用户必须设置初始密码");
        }
        checkUsernameFree(dto.getUsername().trim(), null);
        if (roleMapper.selectById(dto.getRoleId()) == null) {
            throw new BusinessException("角色不存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRoleId(dto.getRoleId());
        user.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        user.setCreateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
        return user.getUserId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(UserDTO dto) {
        SysUser user = getOrThrow(dto.getUserId());
        checkUsernameFree(dto.getUsername().trim(), user.getUserId());

        if (ROLE_ADMIN == user.getRoleId() && dto.getRoleId() != ROLE_ADMIN) {
            ensureNotLastAdmin(user.getUserId());
        }

        user.setUsername(dto.getUsername().trim());
        user.setRoleId(dto.getRoleId());
        user.setPhone(StringUtils.hasText(dto.getPhone()) ? dto.getPhone().trim() : null);
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        sysUserMapper.updateById(user);
    }

    /** 重置为默认密码 123456（忘记密码时管理员兜底）。 */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Integer userId) {
        SysUser user = getOrThrow(userId);
        user.setPassword(passwordEncoder.encode("123456"));
        sysUserMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer userId) {
        SysUser user = getOrThrow(userId);
        if (UserContext.get() != null && Objects.equals(UserContext.get().getUserId(), userId)) {
            throw new BusinessException("不能删除当前登录的账号");
        }
        if (ROLE_ADMIN == user.getRoleId()) {
            ensureNotLastAdmin(userId);
        }
        // owner.user_id 是 ON DELETE SET NULL，删除用户不会破坏主人档案
        sysUserMapper.deleteById(userId);
    }

    private SysUser getOrThrow(Integer userId) {
        if (userId == null) {
            throw new BusinessException("用户不能为空");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在（可能已被删除）");
        }
        return user;
    }

    private void checkUsernameFree(String username, Integer selfId) {
        SysUser other = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("LIMIT 1"));
        if (other != null && !other.getUserId().equals(selfId)) {
            throw new BusinessException("账号「" + username + "」已被占用");
        }
    }

    private void ensureNotLastAdmin(Integer excludeUserId) {
        long others = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, ROLE_ADMIN)
                .ne(SysUser::getUserId, excludeUserId));
        if (others == 0) {
            throw new BusinessException("系统至少要保留一个管理员账号");
        }
    }

    private List<UserVO> toVoList(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> roleIds = users.stream().map(SysUser::getRoleId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Integer, Role> roleMap = roleIds.isEmpty() ? Collections.emptyMap()
                : roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getRoleId, Function.identity(), (a, b) -> a));

        return users.stream().map(user -> {
            UserVO vo = new UserVO();
            vo.setUserId(user.getUserId());
            vo.setUsername(user.getUsername());
            vo.setRoleId(user.getRoleId());
            vo.setPhone(MaskUtil.maskPhone(user.getPhone()));
            vo.setCreateTime(user.getCreateTime());
            Role role = roleMap.get(user.getRoleId());
            if (role != null) {
                vo.setRoleName(role.getRoleName());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
