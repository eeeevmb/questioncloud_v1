package cn.sztu.questioncloud.application.importer.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportSession;

import java.util.List;
import java.util.Optional;

public interface ImportSessionRepository {
    void save(ImportSession session);

    void update(ImportSession session);

    Optional<ImportSession> findById(Long sessionId);

    int tryMarkCommitting(Long importId, int expectedStatus, int newStatus);

    void updateCounts(Long sessionId, int validCount, int invalidCount);

    List<ImportSession> findReadyByUserId(Long userId);
}
