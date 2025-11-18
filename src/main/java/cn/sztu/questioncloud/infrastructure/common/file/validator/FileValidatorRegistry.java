package cn.sztu.questioncloud.infrastructure.common.file.validator;

import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import cn.sztu.questioncloud.infrastructure.common.file.exception.FileExceptionCodeEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文件校验器注册中心（策略路由）
 *
 * @author LinSanQi
 */
@Component
public class FileValidatorRegistry {

    private final Map<String, FileValidator> validators;

    // Spring 自动注入所有 FileValidator 实现
    public FileValidatorRegistry(List<FileValidator> validatorList) {
        this.validators = validatorList.stream()
                .collect(Collectors.toMap(FileValidator::supportedType, Function.identity()));
    }

    /**
     * 根据类型获取对应校验器
     * @param type "image", "document" 等
     * @return 对应的校验器
     * @throws InfrastructureException 找不到校验器时抛出
     */
    public FileValidator get(String type) {
        FileValidator validator = validators.get(type);
        if (validator == null) {
            throw InfrastructureException.of(
                    FileExceptionCodeEnum.FILE_VALIDATOR_NOT_FOUND,
                    "不支持的文件类型: " + type
            );
        }
        return validator;
    }
}