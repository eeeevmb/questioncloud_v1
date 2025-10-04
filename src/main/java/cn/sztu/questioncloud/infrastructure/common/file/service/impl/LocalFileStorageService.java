package cn.sztu.questioncloud.infrastructure.common.file.service.impl;

import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import cn.sztu.questioncloud.infrastructure.common.file.config.FileStorageProperties;
import cn.sztu.questioncloud.infrastructure.common.file.enums.AccessLevel;
import cn.sztu.questioncloud.infrastructure.common.file.exception.FileExceptionCodeEnum;
import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.infrastructure.common.file.repository.FileMetadataRepository;
import cn.sztu.questioncloud.infrastructure.common.file.service.FileStorageService;
import cn.sztu.questioncloud.infrastructure.common.file.util.FileHandlingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Objects;

/**
 * 本地文件存储服务实现
 *
 * @author LinSanQi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private static final String PROVIDER_NAME = "local";
    private final FileStorageProperties properties;
    private final FileMetadataRepository metadataRepository;

    /**
     * 通过 MultipartFile 对象上传文件。
     * 这是对另一个 upload 方法的封装，简化了 Controller 层的调用。
     *
     * @param file            Spring 的 MultipartFile 对象，包含文件所有信息
     * @param ownerIdentifier 文件所有者的唯一标识
     * @param accessLevel     文件的访问级别 (公开, 私有等)
     * @return 存储成功后文件的元数据
     */
    @Override
    public InfraFileMetadata upload(MultipartFile file, String ownerIdentifier, AccessLevel accessLevel) {
        try {
            // 从 MultipartFile 中获取输入流，并调用核心上传逻辑
            return this.upload(
                    file.getInputStream(),
                    ownerIdentifier,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    accessLevel
            );
        } catch (IOException e) {
            log.error("从MultipartFile获取输入流失败", e);
            throw new InfrastructureException(FileExceptionCodeEnum.STORAGE_ERROR, "读取上传文件失败", e);
        }
    }

    /**
     * 核心文件上传逻辑。
     *
     * @param inputStream      文件输入流
     * @param ownerIdentifier  文件所有者标识
     * @param originalFilename 原始文件名
     * @param fileSize         文件大小
     * @param mimeType         MIME 类型
     * @param accessLevel      访问级别
     * @return 存储成功后文件的元数据
     */
    @Override
    public InfraFileMetadata upload(InputStream inputStream, String ownerIdentifier, String originalFilename, long fileSize, String mimeType, AccessLevel accessLevel) {
        // 1. 配额检查：验证用户本周上传文件数量是否超出限制
        checkQuota(ownerIdentifier);

        // 2. 路径生成：为文件创建唯一的存储路径和文件名
        Path relativePath = FileHandlingUtil.generateStoragePath(ownerIdentifier);
        String uniqueFilename = FileHandlingUtil.generateUniqueFilename(originalFilename);
        Path finalRelativePath = relativePath.resolve(uniqueFilename);

        // 3. 物理存储：将文件流写入到磁盘的指定位置
        try {
            // 获取物理基础路径，如果不存在则创建
            Path physicalBasePath = FileHandlingUtil.getPhysicalBasePath(properties.getLocal().getBasePath(), true);
            // 解析出最终的物理目标路径
            Path physicalTargetPath = physicalBasePath.resolve(finalRelativePath);
            // 创建父目录
            Files.createDirectories(physicalTargetPath.getParent());
            // 将输入流复制到目标文件，如果文件已存在则替换
            Files.copy(inputStream, physicalTargetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件物理存储失败", e);
            throw new InfrastructureException(FileExceptionCodeEnum.STORAGE_ERROR, e.getMessage(), e);
        }

        // 4. 元数据注册：在数据库中记录文件的相关信息
        InfraFileMetadata metadata = new InfraFileMetadata(
                finalRelativePath.toString().replace('\\', '/'), // 统一使用 / 作为路径分隔符
                ownerIdentifier,
                accessLevel.getCode(),
                originalFilename,
                fileSize,
                mimeType,
                PROVIDER_NAME
        );
        InfraFileMetadata savedMetadata = metadataRepository.saveOrUpdate(metadata);
        log.info("文件上传成功，元数据ID: {}", savedMetadata.getFmId());

        return savedMetadata;
    }

    /**
     * 检查指定所有者的文件上传配额。
     *
     * @param ownerIdentifier 文件所有者标识
     */
    private void checkQuota(String ownerIdentifier) {
        // 如果未启用配额检查，则直接返回
        if (!properties.getQuota().isEnabled()) {
            return;
        }

        // 计算本周的起始和结束日期
        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        // 查询该用户在本周内已上传的文件数量
        long count = metadataRepository.countByOwnerIdentifierAndCreatedAtBetween(
                ownerIdentifier,
                startOfWeek.atStartOfDay(),
                // endOfWeek 的 23:59:59.999...
                endOfWeek.plusDays(1).atStartOfDay()
        );

        // 如果上传数量达到或超过每周限制，则抛出异常
        if (count >= properties.getQuota().getFilesPerWeek()) {
            throw new InfrastructureException(FileExceptionCodeEnum.QUOTA_EXCEEDED);
        }
    }

    /**
     * 下载文件。
     *
     * @param fileId             文件元数据ID
     * @param accessorIdentifier 请求下载的用户标识
     * @param outputStream       用于写入文件内容的输出流
     * @return 如果成功写入则返回 true
     */
    @Override
    public boolean download(Long fileId, String accessorIdentifier, OutputStream outputStream) {
        InfraFileMetadata metadata = getMetadata(fileId);
        // 1. 权限校验：验证当前用户是否有权下载此文件
        validateAccess(metadata, accessorIdentifier);

        // 2. 文件读取与写入：从物理存储中读取文件并写入到输出流
        try {
            // 获取物理基础路径，此处不创建
            Path physicalBasePath = FileHandlingUtil.getPhysicalBasePath(properties.getLocal().getBasePath(), false);
            Path physicalTargetPath = physicalBasePath.resolve(metadata.getFmStoragePath());

            if (!Files.exists(physicalTargetPath)) {
                log.error("文件物理记录不存在: {}", physicalTargetPath);
                throw new InfrastructureException(FileExceptionCodeEnum.FILE_NOT_FOUND, "物理文件丢失");
            }

            Files.copy(physicalTargetPath, outputStream);
            return true;
        } catch (IOException e) {
            log.error("文件下载失败，fileId: {}", fileId, e);
            throw new InfrastructureException(FileExceptionCodeEnum.DOWNLOAD_ERROR, e.getMessage(), e);
        }
    }

    /**
     * 根据文件ID获取文件元数据。
     *
     * @param fileId 文件元数据ID
     * @return 文件元数据
     * @throws InfrastructureException 如果找不到文件
     */
    @Override
    public InfraFileMetadata getMetadata(Long fileId) {
        return metadataRepository.findById(fileId)
                .orElseThrow(() -> new InfrastructureException(FileExceptionCodeEnum.FILE_NOT_FOUND));
    }

    /**
     * 删除文件（包括物理文件和元数据）。
     *
     * @param fileId          文件元数据ID
     * @param ownerIdentifier 请求删除的用户标识，必须是文件所有者
     */
    @Override
    public void delete(Long fileId, String ownerIdentifier) {
        InfraFileMetadata metadata = getMetadata(fileId);

        // 1. 权限校验 (只有所有者能删除)
        if (!Objects.equals(metadata.getFmOwnerIdentifier(), ownerIdentifier)) {
            throw new InfrastructureException(FileExceptionCodeEnum.OWNER_MISMATCH);
        }

        // 2. 删除元数据
        metadataRepository.delete(metadata);

        // 3. 删除物理文件
        try {
            Path physicalBasePath = FileHandlingUtil.getPhysicalBasePath(properties.getLocal().getBasePath(), false);
            Path physicalTargetPath = physicalBasePath.resolve(metadata.getFmStoragePath());
            Files.deleteIfExists(physicalTargetPath);
        } catch (IOException e) {
            log.error("物理文件删除失败, path: {}", metadata.getFmStoragePath(), e);
            // 注意：这里不直接抛出异常，以允许在物理文件丢失的情况下也能删除元数据。
            // 这种策略可以清理脏数据。
        }
        log.info("文件已删除, fileId: {}", fileId);
    }


    /**
     * 授权其他用户访问私有文件。
     *
     * @param fileId            文件元数据ID
     * @param ownerIdentifier   文件所有者标识
     * @param granteeIdentifier 被授权的用户标识
     */
    @Override
    public void grantAccess(Long fileId, String ownerIdentifier, String granteeIdentifier) {
        InfraFileMetadata metadata = getMetadata(fileId);

        // 1. 权限校验 (只有所有者能授权)
        if (!Objects.equals(metadata.getFmOwnerIdentifier(), ownerIdentifier)) {
            throw new InfrastructureException(FileExceptionCodeEnum.OWNER_MISMATCH);
        }

        // 2. 添加授权并持久化
        // 检查是否已授权，避免重复添加
        if (!metadata.getFmGrantees().contains(granteeIdentifier)) {
            metadata.getFmGrantees().add(granteeIdentifier);
            metadataRepository.saveOrUpdate(metadata);
            log.info("文件访问已授权: fileId={}, grantee={}", fileId, granteeIdentifier);
        } else {
            log.warn("重复授权: fileId={}, grantee={}", fileId, granteeIdentifier);
        }
    }

    /**
     * 取消对其他用户的访问授权。
     *
     * @param fileId            文件元数据ID
     * @param ownerIdentifier   文件所有者标识
     * @param granteeIdentifier 被取消授权的用户标识
     */
    @Override
    public void revokeAccess(Long fileId, String ownerIdentifier, String granteeIdentifier) {
        InfraFileMetadata metadata = getMetadata(fileId);

        // 1. 权限校验 (只有所有者能取消授权)
        if (!Objects.equals(metadata.getFmOwnerIdentifier(), ownerIdentifier)) {
            throw new InfrastructureException(FileExceptionCodeEnum.OWNER_MISMATCH);
        }

        // 2. 移除授权并持久化
        if (metadata.getFmGrantees().remove(granteeIdentifier)) {
            metadataRepository.saveOrUpdate(metadata);
            log.info("文件访问授权已取消: fileId={}, grantee={}", fileId, granteeIdentifier);
        }
    }

    @Override
    public Path getAbsolutePath(String storagePath) {
        // 1. 基础路径获取：从配置中获取本地存储的基础路径
        Path physicalBasePath = FileHandlingUtil.getPhysicalBasePath(properties.getLocal().getBasePath(), false);

        // 2. 路径拼接：将基础路径与存储路径拼接成绝对路径
        return physicalBasePath.resolve(storagePath);
    }

    /**
     * 验证用户是否有权访问文件。
     *
     * @param metadata           文件元数据
     * @param accessorIdentifier 访问者标识
     */
    private void validateAccess(InfraFileMetadata metadata, String accessorIdentifier) {
        AccessLevel level = AccessLevel.fromCode(metadata.getFmAccessLevel());

        // 如果文件是公开的，任何人都可以访问
        if (level == AccessLevel.PUBLIC) {
            return; // 公开文件，任何人可访问
        }

        // 如果访问者是文件所有者，则允许访问
        if (Objects.equals(metadata.getFmOwnerIdentifier(), accessorIdentifier)) {
            return;
        }

        // 如果文件是私有的，检查访问者是否在授权列表中
        if (metadata.getFmGrantees().contains(accessorIdentifier)) {
            return;
        }

        // 如果以上条件都不满足，则拒绝访问
        throw new InfrastructureException(FileExceptionCodeEnum.ACCESS_DENIED);
    }
}

