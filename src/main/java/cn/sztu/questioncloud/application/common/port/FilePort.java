package cn.sztu.questioncloud.application.common.port;

import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FilePort {
    /**
     * 上传文件
     * @param file 文件
     * @return 文件元数据
     */
    InfraFileMetadata uploadFile(MultipartFile file, Long userId);

    /**
     * 根据文件ID获取文件元数据
     * @param fileId 文件ID
     * @return 文件元数据
     */
    InfraFileMetadata getFileMetadata(Long fileId);

    /**
     * 根据文件ID加载底层存储的文件资源
     * @param fileId 文件ID
     * @return 可读取的文件资源
     */
    Resource getResource(Long fileId);
}
