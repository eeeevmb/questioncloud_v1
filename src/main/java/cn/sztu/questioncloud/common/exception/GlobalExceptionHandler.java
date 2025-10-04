package cn.sztu.questioncloud.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理类
 *
 * @author LinSanQi
 */
@Slf4j
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 处理应用异常
     */
    @ExceptionHandler(ApplicationException.class)
    public ResultVO<Object> handleApplicationException(ApplicationException e) {
        log.warn("应用异常: code={}, message={}, targetType={}",
                e.getCode(), e.getMessage(),
                e.getTargetType() != null ? e.getTargetType().getSimpleName() : "null");

        // 转换数据为目标类型
        Object transformedData = transformToTargetType(e.getData(), e.getTargetType());

        return ResultVO.error(e.getCode(), e.getMessage(), transformedData);
    }

    /**
     * 处理基础设施层异常
     * 基础设施层异常不暴露给前端，统一返回系统错误
     */
    @ExceptionHandler(InfrastructureException.class)
    public ResultVO<Void> handleInfrastructureException(InfrastructureException e) {
        // 记录详细的基础设施异常信息用于排查问题
        log.error("基础设施异常: code={}, message={}, data={}, rootCause={}",
                e.getCode(), e.getMessage(), e.getData(),
                e.getRootCause() != null ? e.getRootCause().getMessage() : "null", e);

        // 对外统一返回系统错误，不暴露内部细节
        return ResultVO.error(CommonResultCodeEnum.ERROR.getCode(), CommonResultCodeEnum.ERROR.getMessage());
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResultVO<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("请求资源未找到: message={}", e.getMessage());
        return ResultVO.error(CommonResultCodeEnum.NOT_FOUND.getCode(),
                CommonResultCodeEnum.NOT_FOUND.getMessage());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public ResultVO<Void> handleException(Exception e) {
        log.error("系统异常: message={}, rootCause={}",
                e.getMessage(),
                e.getCause() != null ? e.getCause().getMessage() : "null", e);

        // 对外统一返回系统错误，不暴露内部细节
        return ResultVO.error(CommonResultCodeEnum.ERROR.getCode(), CommonResultCodeEnum.ERROR.getMessage());
    }

    /**
     * 处理SaToken未登录异常
     */
    @ExceptionHandler(NotLoginException.class)
    public ResultVO<Void> handleNotLoginException(NotLoginException e) {
        log.warn("用户未登录: type={}, message={}", e.getType(), e.getMessage());
        return ResultVO.error(CommonResultCodeEnum.NOT_LOGIN.getCode(), CommonResultCodeEnum.NOT_LOGIN.getMessage());
    }

    /**
     * 处理SaToken权限不足异常
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResultVO<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("权限不足: permission={}, message={}", e.getPermission(), e.getMessage());
        return ResultVO.error(CommonResultCodeEnum.NO_PERMISSION.getCode(), CommonResultCodeEnum.NO_PERMISSION.getMessage());
    }

    /**
     * 处理SaToken角色权限不足异常
     */
    @ExceptionHandler(NotRoleException.class)
    public ResultVO<Void> handleNotRoleException(NotRoleException e) {
        log.warn("角色权限不足: role={}, message={}", e.getRole(), e.getMessage());
        return ResultVO.error(CommonResultCodeEnum.NO_ROLE.getCode(), CommonResultCodeEnum.NO_ROLE.getMessage());
    }

    /**
     * 处理ValidationException
     */
    @ExceptionHandler(ValidationException.class)
    public ResultVO<Void> handleValidationException(ValidationException e) {
        log.warn("验证异常: message={}", e.getMessage());
        return ResultVO.error(CommonResultCodeEnum.PARAM_VALIDATION_ERROR.getCode(),
                CommonResultCodeEnum.PARAM_VALIDATION_ERROR.getMessage());
    }

    /**
     * 处理请求体参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVO<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("参数校验失败: {}", errors);
        return ResultVO.error(CommonResultCodeEnum.PARAM_VALIDATION_ERROR.getCode(),
                CommonResultCodeEnum.PARAM_VALIDATION_ERROR.getMessage(), errors);
    }

    /**
     * 处理表单参数校验异常
     */
    @ExceptionHandler(BindException.class)
    public ResultVO<Map<String, String>> handleBindException(BindException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("参数绑定校验失败: {}", errors);
        return ResultVO.error(CommonResultCodeEnum.PARAM_VALIDATION_ERROR.getCode(),
                CommonResultCodeEnum.PARAM_VALIDATION_ERROR.getMessage(), errors);
    }

    /**
     * 处理方法参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResultVO<Map<String, String>> handleConstraintViolationException(ConstraintViolationException e) {
        Map<String, String> errors = e.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        log.warn("约束校验失败: {}", errors);
        return ResultVO.error(CommonResultCodeEnum.PARAM_VALIDATION_ERROR.getCode(),
                CommonResultCodeEnum.PARAM_VALIDATION_ERROR.getMessage(), errors);
    }

    /**
     * 将数据转换为目标类型
     */
    private Object transformToTargetType(Object data, Class<?> targetType) {
        // 判空处理
        if (data == null) {
            return null;
        }

        // 如果没有指定目标类型，直接返回原数据
        if (targetType == null) {
            return data;
        }

        try {
            // 判断是否已经是目标类型
            if (data.getClass() == targetType) {
                return data;
            }

            // 判断是否是目标类型的子类或实现类
            if (targetType.isAssignableFrom(data.getClass())) {
                return data;
            }

            // 判断是否是目标类型的实例
            if (targetType.isInstance(data)) {
                return targetType.cast(data);
            }

            // 使用ObjectMapper进行类型转换
            String jsonString = objectMapper.writeValueAsString(data);
            return objectMapper.readValue(jsonString, targetType);

        } catch (Exception ex) {
            log.error("数据类型转换失败: data={}, targetType={}, error={}",
                    data, targetType.getSimpleName(), ex.getMessage());

            // 转换失败时返回原数据
            return data;
        }
    }
}
