package com.ningcong.petstore.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：把异常统一翻译成 {@link Result}，保证任何情况下前端拿到的都是同一种 JSON。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务规则不满足（如"芯片号已存在"）：400，属可预期结果，只记 warn。 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @RequestBody 上的 @Valid 校验失败。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), firstFieldMessage(e.getBindingResult().getFieldError()));
    }

    /** 表单/查询参数绑定失败。 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), firstFieldMessage(e.getBindingResult().getFieldError()));
    }

    /**
     * 数据库完整性约束被触发，最常见的两种：
     * 1) 唯一索引冲突（芯片号/用户名/一个主人账号重复绑定）；
     * 2) 外键约束（例如删除还有就诊记录的宠物）。
     * 原始报错是英文 + 表结构细节，不适合直接给用户看，这里翻译成人话。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        String detail = e.getMostSpecificCause() == null ? "" : e.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("Duplicate entry")) {
            log.warn("唯一约束冲突: {}", detail);
            return Result.fail(ResultCode.PARAM_ERROR.getCode(), "该记录已存在，请勿重复提交");
        }
        log.warn("外键/完整性约束拦截: {}", detail);
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), "该数据已被其它记录引用，无法删除或修改");
    }

    /** 兜底：未预料的异常，记 error 日志并返回 500，不把堆栈暴露给前端。 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(ResultCode.ERROR);
    }

    private String firstFieldMessage(FieldError fieldError) {
        return fieldError == null ? ResultCode.PARAM_ERROR.getMessage() : fieldError.getDefaultMessage();
    }
}
