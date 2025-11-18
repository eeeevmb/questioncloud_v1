package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.ImportCacheAdapter;
import cn.sztu.questioncloud.infrastructure.adapter.question.model.ImportSession;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import cn.sztu.questioncloud.infrastructure.common.file.enums.AccessLevel;
import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.infrastructure.common.file.service.FileStorageService;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDraftVO;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class ImportCacheAdapterImpl implements ImportCacheAdapter {
    private final CacheService cacheService;
    private final FileStorageService fileStorageService;

    private final Long TIME_OUT = 30L;
    private final TimeUnit TIME_UNIT = TimeUnit.MINUTES;

    public ImportCacheAdapterImpl(CacheService cacheService, FileStorageService fileStorageService) {
        this.cacheService = cacheService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 创建上传会话
     *
     * @param userId 用户Id
     * @param drafts 题目草稿
     * @return 会话id
     */
    @Override
    public String createImportSession(Long userId, List<QuestionDraftVO> drafts) {
        // 1. 获取会话ID
        String importSessionId = String.valueOf(UUID.randomUUID());

        // 2. 构建会话对象
        ImportSession session = ImportSession.builder()
                .sessionId(importSessionId)
                .userId(userId)
                .drafts(drafts)
                .assets(new HashMap<>())
                .build();

        // 3. 设置Redis缓存30MIN
        cacheService.set(importSessionId, session, TIME_OUT, TIME_UNIT);

        return importSessionId;
    }

    /**
     * 获取上传会话
     *
     * @param sessionId 会话ID
     * @return 会话
     */
    @Override
    public ImportSession getImportSession(String sessionId) {

        return cacheService.get(sessionId);
    }

    /**
     * 上传附件并刷新会话
     *
     * @param session 会话
     * @param slotId  附件ID
     * @param asset   附件文件
     */
    @Override
    public void uploadAsset(ImportSession session, Long userId, String slotId, MultipartFile asset) {
        InfraFileMetadata fileMetadata = fileStorageService.upload(asset, String.valueOf(userId), AccessLevel.PRIVATE);

        if (!session.assets().containsKey(slotId)) {
            session.assets().put(slotId, fileMetadata);
        } else {
            session.assets().replace(slotId, fileMetadata);
        }

        cacheService.set(session.sessionId(), session, TIME_OUT, TIME_UNIT);
    }

}
