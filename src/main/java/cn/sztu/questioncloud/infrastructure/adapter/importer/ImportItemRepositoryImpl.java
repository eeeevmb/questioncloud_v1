package cn.sztu.questioncloud.infrastructure.adapter.importer;

import cn.sztu.questioncloud.application.importer.port.ImportItemRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportItemEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.importer.ImportItemMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class ImportItemRepositoryImpl implements ImportItemRepository {
    private final ImportItemMapper importItemMapper;

    public ImportItemRepositoryImpl(ImportItemMapper importItemMapper) {
        this.importItemMapper = importItemMapper;
    }

    @Override
    public void batchSave(List<ImportItemEntity> items) {
        importItemMapper.saveBatch(items);
    }

    @Override
    public List<ImportItemEntity> pageItems(Long importId, Integer status, int offset, int limit) {
        return importItemMapper.selectPage(importId, status, offset, limit);
    }

    @Override
    public long countByStatus(Long importId, Integer status) {
        return importItemMapper.countByImportId(importId, status);
    }

    @Override
    public ImportItemEntity findById(Long itemId) {
        return importItemMapper.getById(itemId);
    }

    @Override
    public List<ImportItemEntity> findValidByImportId(Long importId) {
        List<ImportItemEntity> result = QueryChain.of(importItemMapper)
                .eq(ImportItemEntity::getImportId, importId)
                .eq(ImportItemEntity::getStatus, 0)
                .orderBy(ImportItemEntity::getIndexNo)
                .list();

        return result == null ? List.of() : result;
    }

    @Override
    public void update(ImportItemEntity item) {
        importItemMapper.update(item);
    }
}
