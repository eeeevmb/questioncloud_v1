package cn.sztu.questioncloud.common.exception;


import cn.sztu.questioncloud.common.constant.enums.result.ResultCodeEnum;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import lombok.Getter;

/**
 * 应用异常类
 * 用于处理应用层面的各种异常，包括业务逻辑错误、文件操作错误等
 *
 * @author LinSanQi
 */
@Getter
public class ApplicationException extends RuntimeException {

    private final String code;
    private final String message;
    private final Object data;
    private final Class<?> targetType;

    public ApplicationException(ResultCodeEnum resultCode) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.data = null;
        this.targetType = null;
    }

    public ApplicationException(ResultCodeEnum resultCode, String additionalMessage) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage() + ": " + additionalMessage;
        this.data = null;
        this.targetType = null;
    }

    public ApplicationException(String message) {
        this.code = CommonResultCodeEnum.ERROR.getCode();
        this.message = message;
        this.data = null;
        this.targetType = null;
    }

    public ApplicationException(ResultCodeEnum resultCode, Object data, Class<?> targetType) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.data = data;
        this.targetType = targetType;
    }

    public ApplicationException(ResultCodeEnum resultCode, String additionalMessage, Object data, Class<?> targetType) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage() + ": " + additionalMessage;
        this.data = data;
        this.targetType = targetType;
    }

    public ApplicationException(String message, Object data, Class<?> targetType) {
        this.code = CommonResultCodeEnum.ERROR.getCode();
        this.message = message;
        this.data = data;
        this.targetType = targetType;
    }

    // 为了向后兼容，保留原有的构造方法，不指定目标类型
    public ApplicationException(ResultCodeEnum resultCode, Object data) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage();
        this.data = data;
        this.targetType = null;
    }

    public ApplicationException(ResultCodeEnum resultCode, String additionalMessage, Object data) {
        this.code = resultCode.getCode();
        this.message = resultCode.getMessage() + ": " + additionalMessage;
        this.data = data;
        this.targetType = null;
    }

    public ApplicationException(String message, Object data) {
        this.code = CommonResultCodeEnum.ERROR.getCode();
        this.message = message;
        this.data = data;
        this.targetType = null;
    }

    // 便捷的静态工厂方法
    public static <T> ApplicationException withType(ResultCodeEnum resultCode, Object data, Class<T> targetType) {
        return new ApplicationException(resultCode, data, targetType);
    }

    public static <T> ApplicationException withType(String message, Object data, Class<T> targetType) {
        return new ApplicationException(message, data, targetType);
    }

    public static <T> ApplicationException withType(ResultCodeEnum resultCode, String additionalMessage, Object data, Class<T> targetType) {
        return new ApplicationException(resultCode, additionalMessage, data, targetType);
    }
}
