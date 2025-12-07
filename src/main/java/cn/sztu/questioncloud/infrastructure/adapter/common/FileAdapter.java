package cn.sztu.questioncloud.infrastructure.adapter.common;

import cn.sztu.questioncloud.application.common.port.FilePort;
import cn.sztu.questioncloud.infrastructure.common.exception.InfraExceptionCode;
import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import cn.sztu.questioncloud.infrastructure.common.file.enums.AccessLevel;
import cn.sztu.questioncloud.infrastructure.common.file.exception.FileExceptionCodeEnum;
import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.infrastructure.common.file.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;

@Component
public class FileAdapter implements FilePort {

    private final FileStorageService fileStorageService;

    public FileAdapter(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件元数据
     */
    @Override
    public InfraFileMetadata uploadFile(MultipartFile file, Long userId) {
        return fileStorageService.upload(file, String.valueOf(userId), AccessLevel.PRIVATE);
    }

    /**
     * 根据文件ID获取文件元数据
     *
     * @param fileId 文件ID
     * @return 文件元数据
     */
    @Override
    public InfraFileMetadata getFileMetadata(Long fileId) {
        return fileStorageService.getMetadata(fileId);
    }

    /**
     * 根据文件ID加载底层存储的文件资源
     *
     * @param fileId 文件ID
     * @return 可读取的文件资源
     */
    @Override
    public Resource getResource(Long fileId) {
        InfraFileMetadata metadata = getFileMetadata(fileId);
        String relativePath = metadata.getFmStoragePath();
        Path filePath = fileStorageService.getAbsolutePath(relativePath);
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new InfrastructureException(FileExceptionCodeEnum.FILE_NOT_FOUND);
            } else if (!resource.isReadable()) {
                throw new InfrastructureException(FileExceptionCodeEnum.FILE_READ_ERROR);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new InfrastructureException(FileExceptionCodeEnum.INVALID_PATH);
        }
    }
}
