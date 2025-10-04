package cn.sztu.questioncloud.infrastructure.common.file.enums;

import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

import static cn.sztu.questioncloud.infrastructure.common.file.exception.FileExceptionCodeEnum.ACCESS_LEVEL_NOT_FOUND;

/**
 * 文件访问级别枚举
 * 定义了文件的基础权限
 *
 * @author LinSanQi
 */
@Getter
@AllArgsConstructor
public enum AccessLevel {
    /**
     * 私有的
     * 只有所有者或被明确授权的用户才能访问。
     */
    PRIVATE(0, "私有"),

    /**
     * 公开的
     * 任何人都可以访问。
     */
    PUBLIC(1, "公开");

    private final Integer code;
    private final String description;

    public static AccessLevel fromCode(Integer code) {
        return Arrays.stream(AccessLevel.values())
                .filter(level -> level.getCode().equals(code))
                .findFirst()
                .orElseThrow(
                        // 抛出异常
                        () -> new InfrastructureException(ACCESS_LEVEL_NOT_FOUND)
                );
    }
} 