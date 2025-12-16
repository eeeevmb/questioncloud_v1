package cn.sztu.questioncloud.infrastructure.common.file.validator;

import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import cn.sztu.questioncloud.infrastructure.common.file.config.FileUploadProperties;
import cn.sztu.questioncloud.infrastructure.common.file.detector.FileTypeDetector;
import cn.sztu.questioncloud.infrastructure.common.file.exception.FileExceptionCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
public class DocumentValidator implements FileValidator{
    private final FileUploadProperties properties;
    private final FileTypeDetector detector;

    public DocumentValidator(FileUploadProperties properties, FileTypeDetector detector) {
        this.properties = properties;
        this.detector = detector;
    }

    /**
     * 校验文件
     *
     * @param file 文档文件
     * @return 校验是否通过
     */
    @Override
    public boolean validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InfrastructureException(FileExceptionCodeEnum.FILE_EMPTY);
        }

        String ext = FileValidator.getExtension(file.getOriginalFilename());
        Set<String> allowedExts = properties.getAllowedDocExtensions();
        String allowed = String.join(",", allowedExts);

        if (!allowedExts.contains(ext)) {
            throw new InfrastructureException(FileExceptionCodeEnum.FILE_EXTENSION_NOT_ALLOWED,
                    String.format("当前扩展名：%s，允许的扩展名：%s", ext, allowed));
        }

        // 如果是 tex，直接放行（只基于后缀）
        if ("tex".equalsIgnoreCase(ext)) {
            log.debug("tex 文件，仅基于扩展名通过校验: {}", file.getOriginalFilename());
            return true;
        }

        // 魔数嗅探（内容检测）
        String detectedType;
        try {
            detectedType = detector.detect(file);
        } catch (IOException e) {
            log.error("文件读取失败: {}", file.getOriginalFilename(), e);
            throw InfrastructureException.of(FileExceptionCodeEnum.FILE_READ_ERROR, e);
        }

        // 检测到的类型必须是 pdf、tex或doc
        if (detectedType == null || !allowedExts.contains(detectedType)) {
            throw InfrastructureException.of(
                    FileExceptionCodeEnum.FILE_TYPE_INVALID,
                    String.format("检测到的文件类型: %s, 期望类型: doc、docx、pdf或tex", detectedType)
            );
        }

        if (!isConsistent(ext, detectedType)) {
            throw InfrastructureException.of(
                    FileExceptionCodeEnum.FILE_TYPE_MISMATCH,
                    String.format("文件扩展名: %s, 实际类型: %s", ext, detectedType)
            );
        }


        log.debug("文档文件校验通过: {} ({})", file.getOriginalFilename(), detectedType);

        return true;
    }

    /**
     * 该校验器支持的文件类型
     *
     * @return 例如 "image", "document", "video"
     */
    @Override
    public String supportedType() {
        return "document";
    }

    private boolean isConsistent(String ext, String detected) {
        return ext.equalsIgnoreCase(detected);
    }

}
