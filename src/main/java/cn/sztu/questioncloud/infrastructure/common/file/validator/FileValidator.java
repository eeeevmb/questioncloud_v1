package cn.sztu.questioncloud.infrastructure.common.file.validator;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件校验器接口
 */
public interface FileValidator {
    /**
     * 校验文件
     */
    boolean validate(MultipartFile file);

    /**
     * 该校验器支持的文件类型
     * @return 例如 "image", "document", "video"
     */
    String supportedType();
}
