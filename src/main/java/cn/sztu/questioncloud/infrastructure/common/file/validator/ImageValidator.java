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
public class ImageValidator implements FileValidator {

    private final FileUploadProperties properties;
    private final FileTypeDetector detector;

    public ImageValidator(FileUploadProperties properties, FileTypeDetector detector) {
        this.properties = properties;
        this.detector = detector;
    }

    // TODO 修改
    @Override
    public boolean validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InfrastructureException(FileExceptionCodeEnum.FILE_EMPTY);
        }

        String ext = FileValidator.getExtension(file.getOriginalFilename());
        Set<String> allowedExts = properties.getAllowedImageExtensions();
        String allowed = String.join(",", allowedExts);

        if (!allowedExts.contains(ext)) {
            throw new InfrastructureException(FileExceptionCodeEnum.FILE_EXTENSION_NOT_ALLOWED,
                    String.format("当前扩展名：%s，允许的扩展名：%s", ext, allowed));
        }

        // 魔数嗅探（内容检测）
        String detectedType;
        try {
            detectedType = detector.detect(file);
        } catch (IOException e) {
            log.error("文件读取失败: {}", file.getOriginalFilename(), e);
            throw InfrastructureException.of(FileExceptionCodeEnum.FILE_READ_ERROR, e);
        }

        // 检测到的类型必须是 JPEG 或 PNG
        if (detectedType == null || !allowedExts.contains(detectedType)) {
            throw InfrastructureException.of(
                    FileExceptionCodeEnum.FILE_TYPE_INVALID,
                    String.format("检测到的文件类型: %s, 期望类型: jpeg/png", detectedType)
            );
        }

        // 一致性校验（扩展名与内容匹配）
        if (!isConsistent(ext, detectedType)) {
            throw InfrastructureException.of(
                    FileExceptionCodeEnum.FILE_TYPE_MISMATCH,
                    String.format("文件扩展名: %s, 实际类型: %s", ext, detectedType)
            );
        }

        log.debug("图片文件校验通过: {} ({})", file.getOriginalFilename(), detectedType);

        return true;
    }

    @Override
    public String supportedType() {
        return "image";
    }

    private boolean isConsistent(String ext, String detected) {
        if ("jpeg".equals(detected)) return "jpg".equals(ext) || "jpeg".equals(ext);
        return ext.equals(detected);
    }
}