package cn.sztu.questioncloud.application.importer.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportItemEntity;
import java.util.List;

public interface ImportItemRepository {
    void batchSave(List<ImportItemEntity> items);

    List<ImportItemEntity> pageItems(Long importId, Integer status, int offset, int limit);

    long countByStatus(Long importId, Integer status);

    ImportItemEntity findById(Long itemId);

    List<ImportItemEntity> findValidByImportId(Long importId);

    void update(ImportItemEntity item);
}
