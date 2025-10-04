package cn.sztu.questioncloud.infrastructure.common.exception;

/**
 * 通用基础设施异常枚举
 * 定义基础设施层通用异常码
 *
 * @author LinSanQi
 */
public enum CommonInfraExceptionEnum implements InfraExceptionCode {

    // 通用基础设施错误
    INFRA_COMMON_UNKNOWN_ERROR("INFRA_COMMON_UNKNOWN_ERROR", "基础设施层未知错误"),
    INFRA_COMMON_INIT_ERROR("INFRA_COMMON_INIT_ERROR", "基础设施组件初始化失败"),
    INFRA_COMMON_CONFIG_ERROR("INFRA_COMMON_CONFIG_ERROR", "基础设施配置错误");

    private final String code;
    private final String message;

    CommonInfraExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

