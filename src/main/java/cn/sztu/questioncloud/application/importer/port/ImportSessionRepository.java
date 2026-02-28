package cn.sztu.questioncloud.application.importer.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportSession;

import java.util.Optional;

public interface ImportSessionRepository {
    void save(ImportSession session);

    void update(ImportSession session);

    Optional<ImportSession> findById(Long sessionId);

    void updateCounts(Long sessionId, int validCount, int invalidCount);
}
