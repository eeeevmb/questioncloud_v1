package cn.sztu.questioncloud.infrastructure.common.file.repository.impl;

import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import cn.sztu.questioncloud.infrastructure.common.file.exception.FileExceptionCodeEnum;
import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.infrastructure.common.file.repository.FileMetadataRepository;
import cn.sztu.questioncloud.infrastructure.common.file.repository.mapper.FileMetadataMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FileMetadataRepositoryImpl implements FileMetadataRepository {

    private final FileMetadataMapper fileMetadataMapper;

    public FileMetadataRepositoryImpl(FileMetadataMapper fileMetadataMapper) {
        this.fileMetadataMapper = fileMetadataMapper;
    }

    @Override
    public InfraFileMetadata saveOrUpdate(InfraFileMetadata metadata) {
        int count = fileMetadataMapper.saveOrUpdate(metadata);
        if (count <= 0 || metadata.getFmId() == null) {
            throw new InfrastructureException(FileExceptionCodeEnum.STORAGE_ERROR);
        }
        return metadata;
    }

    @Override
    public Optional<InfraFileMetadata> findById(Long fileId) {
        InfraFileMetadata metadata = fileMetadataMapper.getById(fileId);
        return Optional.ofNullable(metadata);
    }

    /**
     * 统计指定用户在指定时间段内上传的文件数量
     *
     * @param ownerIdentifier 用户标识
     * @param start           开始时间
     * @param end             结束时间
     * @return 文件数量
     */
    @Override
    public long countByOwnerIdentifierAndCreatedAtBetween(String ownerIdentifier, LocalDateTime start, LocalDateTime end) {

        if (end.isBefore(start)) {
            throw new InfrastructureException(FileExceptionCodeEnum.INVALID_TIME_RANGE);
        }
        return QueryChain.of(fileMetadataMapper)
                .eq(InfraFileMetadata::getFmOwnerIdentifier, ownerIdentifier)
                .between(InfraFileMetadata::getFmCreatedAt, start, end)
                .count();
    }

    @Override
    public void delete(InfraFileMetadata metadata) {

        int delete = fileMetadataMapper.delete(metadata);
        if (delete <= 0) {
            throw new InfrastructureException(FileExceptionCodeEnum.DELETE_ERROR);
        }
    }
}
