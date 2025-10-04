package cn.sztu.questioncloud.infrastructure.common.exception;

/**
 * 基础设施层异常码接口
 * 定义基础设施层异常码的统一规范
 *
 * @author LinSanQi
 */
public interface InfraExceptionCode {

    /**
     * 获取异常码
     *
     * @return 异常码
     */
    String getCode();

    /**
     * 获取异常信息
     *
     * @return 异常信息
     */
    String getMessage();
}
