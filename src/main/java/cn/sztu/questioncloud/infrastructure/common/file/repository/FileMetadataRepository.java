package cn.sztu.questioncloud.infrastructure.common.file.repository;


import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 文件元数据仓库
 *
 * @author LinSanQi
 */
public interface FileMetadataRepository {

    /**
     * 保存文件元数据
     *
     * @param metadata 文件元数据
     * @return 已保存的文件元数据（可能包含生成的ID）
     */
    InfraFileMetadata saveOrUpdate(InfraFileMetadata metadata);

    /**
     * 根据ID查找文件元数据
     *
     * @param fileId 文件ID
     * @return 文件元数据
     */
    Optional<InfraFileMetadata> findById(Long fileId);

    /**
     * 根据所有者标识和创建时间范围统计文件数量
     *
     * @param ownerIdentifier 所有者标识
     * @param start           起始时间
     * @param end             结束时间
     * @return 文件数量
     */
    long countByOwnerIdentifierAndCreatedAtBetween(String ownerIdentifier, LocalDateTime start, LocalDateTime end);

    /**
     * 根据文件元数据删除记录
     *
     * @param metadata 文件元数据
     */
    void delete(InfraFileMetadata metadata);
} 