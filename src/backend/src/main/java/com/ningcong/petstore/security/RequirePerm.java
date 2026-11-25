package com.ningcong.petstore.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级权限声明（两级鉴权的第二级）。
 *
 * <p>写在 Controller 方法上，由 {@link PermAspect} 在方法执行前校验；多个权限码之间是"满足其一即可"。</p>
 *
 * <pre>
 * &#64;RequirePerm("pet:add")            // 需要 pet:add
 * &#64;RequirePerm({"pet:edit", "pet:add"}) // 有其中一个即可
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePerm {

    /** 权限码，如 pet:add、foster:audit；角色 permission 字段为 * 时视为拥有全部权限。 */
    String[] value();
}
