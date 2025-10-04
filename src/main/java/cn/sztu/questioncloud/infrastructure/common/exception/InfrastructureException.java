package cn.sztu.questioncloud.infrastructure.common.exception;

import lombok.Getter;

/**
 * 基础设施层异常类
 * 用于处理基础设施层面的各种异常，如数据库连接、缓存操作、文件系统等
 * 此异常仅在基础设施层内部使用，不会直接暴露给前端
 *
 * @author LinSanQi
 */
@Getter
public class InfrastructureException extends RuntimeException {

    private final String code;
    private final String message;
    private final Object data;
    private final Throwable rootCause;

    public InfrastructureException(InfraExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage();
        this.data = null;
        this.rootCause = null;
    }

    public InfrastructureException(InfraExceptionCode exceptionCode, String additionalMessage) {
        super(exceptionCode.getMessage() + ": " + additionalMessage);
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage() + ": " + additionalMessage;
        this.data = null;
        this.rootCause = null;
    }

    public InfrastructureException(InfraExceptionCode exceptionCode, Throwable cause) {
        super(exceptionCode.getMessage(), cause);
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage();
        this.data = null;
        this.rootCause = cause;
    }

    public InfrastructureException(InfraExceptionCode exceptionCode, String additionalMessage, Throwable cause) {
        super(exceptionCode.getMessage() + ": " + additionalMessage, cause);
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage() + ": " + additionalMessage;
        this.data = null;
        this.rootCause = cause;
    }

    public InfrastructureException(InfraExceptionCode exceptionCode, Object data) {
        super(exceptionCode.getMessage());
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage();
        this.data = data;
        this.rootCause = null;
    }

    public InfrastructureException(InfraExceptionCode exceptionCode, String additionalMessage, Object data) {
        super(exceptionCode.getMessage() + ": " + additionalMessage);
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage() + ": " + additionalMessage;
        this.data = data;
        this.rootCause = null;
    }

    public InfrastructureException(InfraExceptionCode exceptionCode, Object data, Throwable cause) {
        super(exceptionCode.getMessage(), cause);
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage();
        this.data = data;
        this.rootCause = cause;
    }

    public InfrastructureException(InfraExceptionCode exceptionCode, String additionalMessage, Object data, Throwable cause) {
        super(exceptionCode.getMessage() + ": " + additionalMessage, cause);
        this.code = exceptionCode.getCode();
        this.message = exceptionCode.getMessage() + ": " + additionalMessage;
        this.data = data;
        this.rootCause = cause;
    }

    // 便捷的静态工厂方法
    public static InfrastructureException of(InfraExceptionCode exceptionCode) {
        return new InfrastructureException(exceptionCode);
    }

    public static InfrastructureException of(InfraExceptionCode exceptionCode, String additionalMessage) {
        return new InfrastructureException(exceptionCode, additionalMessage);
    }

    public static InfrastructureException of(InfraExceptionCode exceptionCode, Throwable cause) {
        return new InfrastructureException(exceptionCode, cause);
    }

    public static InfrastructureException of(InfraExceptionCode exceptionCode, String additionalMessage, Throwable cause) {
        return new InfrastructureException(exceptionCode, additionalMessage, cause);
    }

    public static InfrastructureException withData(InfraExceptionCode exceptionCode, Object data) {
        return new InfrastructureException(exceptionCode, data);
    }

    public static InfrastructureException withData(InfraExceptionCode exceptionCode, String additionalMessage, Object data) {
        return new InfrastructureException(exceptionCode, additionalMessage, data);
    }

    public static InfrastructureException withData(InfraExceptionCode exceptionCode, Object data, Throwable cause) {
        return new InfrastructureException(exceptionCode, data, cause);
    }

    public static InfrastructureException withData(InfraExceptionCode exceptionCode, String additionalMessage, Object data, Throwable cause) {
        return new InfrastructureException(exceptionCode, additionalMessage, data, cause);
    }
}
