package cn.sztu.questioncloud.common.exception;

/**
 * 业务相关异常
 */
public class BaseException extends RuntimeException {
    public BaseException() {}

    public BaseException(String msg) {
        super(msg);
    }
}
