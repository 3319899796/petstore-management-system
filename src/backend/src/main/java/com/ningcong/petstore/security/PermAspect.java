package com.ningcong.petstore.security;

import com.ningcong.petstore.common.BusinessException;
import com.ningcong.petstore.common.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * 权限切面（两级鉴权的第二级）：方法执行前检查 {@link RequirePerm} 声明的权限码。
 *
 * <p>与拦截器的分工：拦截器回答"你是谁"（401），切面回答"你能不能做这件事"（403）。</p>
 */
@Slf4j
@Aspect
@Component
public class PermAspect {

    @Before("@annotation(requirePerm)")
    public void checkPermission(JoinPoint joinPoint, RequirePerm requirePerm) {
        LoginUser user = UserContext.get();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        for (String permission : requirePerm.value()) {
            if (user.hasPermission(permission)) {
                return;
            }
        }

        String need = String.join(" 或 ", requirePerm.value());
        log.warn("权限不足: user={}, role={}, need={}, method={}",
                user.getUsername(), user.getRoleName(), need, joinPoint.getSignature().toShortString());
        throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "缺少权限：" + need);
    }
}
