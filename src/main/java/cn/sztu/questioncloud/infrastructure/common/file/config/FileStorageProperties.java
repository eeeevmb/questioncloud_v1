package cn.sztu.questioncloud.infrastructure.common.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置
 *
 * @author LinSanQi
 */
@Data
@Component
@ConfigurationProperties(prefix = "infrastructure.file")
public class FileStorageProperties {

    /**
     * 本地存储配置
     */
    private LocalStorage local = new LocalStorage();

    /**
     * 配额配置
     */
    private Quota quota = new Quota();

    @Data
    public static class LocalStorage {
        /**
         * 本地存储的根目录
         */
        private String basePath = "storage";
    }

    @Data
    public static class Quota {
        /**
         * 每周文件上传的数量配额
         */
        private Integer filesPerWeek = 100;

        /**
         * 是否启用配额检查
         */
        private boolean enabled = true;
    }
} 