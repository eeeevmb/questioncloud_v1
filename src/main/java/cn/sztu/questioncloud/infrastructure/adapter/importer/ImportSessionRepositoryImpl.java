package cn.sztu.questioncloud.infrastructure.adapter.importer;

import cn.sztu.questioncloud.application.importer.port.ImportSessionRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportSession;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.importer.ImportSessionMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import cn.xbatis.core.sql.executor.chain.UpdateChain;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class ImportSessionRepositoryImpl implements ImportSessionRepository {
    private final ImportSessionMapper importSessionMapper;

    public ImportSessionRepositoryImpl(ImportSessionMapper importSessionMapper) {
        this.importSessionMapper = importSessionMapper;
    }

    @Override
    public void save(ImportSession session) {
        importSessionMapper.save(session);
    }

    @Override
    public void update(ImportSession session) {
        UpdateChain chain = UpdateChain.of(importSessionMapper).update(ImportSession.class);
        if (session.getUserId() != null) {
            chain.set(ImportSession::getUserId, session.getUserId());
        }
        if (session.getStatus() != null) {
            chain.set(ImportSession::getStatus, session.getStatus());
        }
        if (session.getFileId() != null) {
            chain.set(ImportSession::getFileId, session.getFileId());
        }
        if (session.getFormat() != null) {
            chain.set(ImportSession::getFormat, session.getFormat());
        }
        if (session.getTotal() != null) {
            chain.set(ImportSession::getTotal, session.getTotal());
        }
        if (session.getValidCnt() != null) {
            chain.set(ImportSession::getValidCnt, session.getValidCnt());
        }
        if (session.getInvalidCnt() != null) {
            chain.set(ImportSession::getInvalidCnt, session.getInvalidCnt());
        }
        if (session.getVersion() != null) {
            chain.set(ImportSession::getVersion, session.getVersion());
        }
        chain.set(ImportSession::getUpdatedAt, LocalDateTime.now());
        chain.eq(ImportSession::getId, session.getId()).execute();
    }

    @Override
    public Optional<ImportSession> findById(Long sessionId) {
        return Optional.ofNullable(QueryChain.of(importSessionMapper)
                .eq(ImportSession::getId, sessionId)
                .get());
    }

    @Override
    public int tryMarkCommitting(Long importId, int expectedStatus, int newStatus) {
        return UpdateChain.of(importSessionMapper)
                .set(ImportSession::getStatus, newStatus)
                .set(ImportSession::getUpdatedAt, LocalDateTime.now())
                .eq(ImportSession::getId, importId)
                .eq(ImportSession::getStatus, expectedStatus)
                .execute();
    }

    @Override
    public void updateCounts(Long sessionId, int validCount, int invalidCount) {
        UpdateChain.of(importSessionMapper)
                .update(ImportSession.class)
                .set(ImportSession::getValidCnt, validCount)
                .set(ImportSession::getInvalidCnt, invalidCount)
                .set(ImportSession::getUpdatedAt, LocalDateTime.now())
                .eq(ImportSession::getId, sessionId)
                .execute();
    }
}
