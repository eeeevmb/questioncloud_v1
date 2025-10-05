package cn.sztu.questioncloud.infrastructure.common.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 文件上传配置
 *
 * @author evmb
 */

@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadProperties {
    private Set<String> allowedImageExtensions = new HashSet<>(Set.of("jpg", "jpeg", "png"));

    private Set<String> allowedDocExtensions = new HashSet<>(Set.of("doc", "docx", "pdf"));
}
