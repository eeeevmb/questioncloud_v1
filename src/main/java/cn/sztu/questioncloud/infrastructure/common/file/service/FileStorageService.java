package cn.sztu.questioncloud.infrastructure.common.file.service;


import cn.sztu.questioncloud.infrastructure.common.file.enums.AccessLevel;
import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * 文件存储服务接口
 * 定义了上层应用与文件模块交互的唯一入口
 *
 * @author LinSanQi
 */
public interface FileStorageService {
    /**
     * 上传文件 (便捷版)
     *
     * @param file            Spring的MultipartFile对象
     * @param ownerIdentifier 文件所有者的唯一标识
     * @param accessLevel     访问级别
     * @return 文件的元数据
     */
    InfraFileMetadata upload(MultipartFile file, String ownerIdentifier, AccessLevel accessLevel);

    /**
     * 上传文件。
     * 此方法不仅保存文件流，还会执行配额检查、生成存储路径，并为文件注册元数据。
     *
     * @param inputStream      文件输入流
     * @param ownerIdentifier  文件所有者的唯一标识
     * @param originalFilename 原始文件名
     * @param fileSize         文件大小
     * @param mimeType         MIME类型
     * @param accessLevel      访问级别
     * @return 文件的元数据
     */
    InfraFileMetadata upload(InputStream inputStream, String ownerIdentifier, String originalFilename, long fileSize, String mimeType, AccessLevel accessLevel);

    /**
     * 下载文件。
     * 在读取文件前，会根据 accessorIdentifier（访问者）进行严格的权限校验。
     *
     * @param fileId             文件ID
     * @param accessorIdentifier 访问者的唯一标识
     * @param outputStream       文件输出流
     * @return 如果成功写入输出流则返回true，否则返回false
     */
    boolean download(Long fileId, String accessorIdentifier, OutputStream outputStream);

    /**
     * 获取文件的元数据信息。
     *
     * @param fileId 文件ID
     * @return 文件元数据，如果不存在则返回null或抛出异常
     */
    InfraFileMetadata getMetadata(Long fileId);

    /**
     * 删除文件。
     * 包含权限验证，确保只有所有者才能删除。
     *
     * @param fileId          文件ID
     * @param ownerIdentifier 文件所有者的唯一标识
     */
    void delete(Long fileId, String ownerIdentifier);

    /**
     * 授予文件访问权限。
     * 将指定文件的访问权限授予另一位用户。
     *
     * @param fileId            文件ID
     * @param ownerIdentifier   文件所有者的唯一标识
     * @param granteeIdentifier 被授权者的唯一标识
     */
    void grantAccess(Long fileId, String ownerIdentifier, String granteeIdentifier);

    /**
     * 取消文件访问授权。
     *
     * @param fileId            文件ID
     * @param ownerIdentifier   文件所有者的唯一标识
     * @param granteeIdentifier 被授权者的唯一标识
     */
    void revokeAccess(Long fileId, String ownerIdentifier, String granteeIdentifier);

    /**
     * 根据存储路径获取文件的绝对物理路径
     *
     * @param storagePath 文件的相对存储路径
     * @return 文件的绝对路径
     */
    Path getAbsolutePath(String storagePath);
} 