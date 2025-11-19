package cn.sztu.questioncloud.application.question.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionStatusEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.application.question.port.*;
import cn.sztu.questioncloud.application.question.service.QuestionImportService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.adapter.question.model.ImportSession;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.AssetSnapshotFactory;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.CollectionItem;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.sztu.questioncloud.web.rest.v1.question.req.ConfirmDraftReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.ParseResultVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDraftVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 题目导入编排服务：串联解析器和 VO 转换，生成会话标识。
 */
@Slf4j
@Service
public class QuestionImportServiceImpl implements QuestionImportService {
    private final AssetSnapshotFactory assetSnapshotFactory;
    private final LatexQuestionParser latexQuestionParser;
    private final ImportCacheAdapter importCacheAdapter;
    private final AssetCheckerPort assetCheckerPort;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final CollectionPresenceCheckerPort collectionPresenceCheckerPort;
    private final QuestionQueryRepository questionQueryRepository;

    private final Charset CHAR_SET = StandardCharsets.UTF_8;
    private static final int INITIAL_VERSION = 1;

    public QuestionImportServiceImpl(AssetSnapshotFactory assetSnapshotFactory, LatexQuestionParser latexQuestionParser, ImportCacheAdapter importCacheAdapter, AssetCheckerPort assetCheckerPort, QuestionRepository questionRepository, QuestionVersionRepository questionVersionRepository, CollectionItemRepository collectionItemRepository, CollectionPresenceCheckerPort collectionPresenceCheckerPort, QuestionQueryRepository questionQueryRepository) {
        this.assetSnapshotFactory = assetSnapshotFactory;
        this.latexQuestionParser = latexQuestionParser;
        this.importCacheAdapter = importCacheAdapter;
        this.assetCheckerPort = assetCheckerPort;
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.collectionItemRepository = collectionItemRepository;
        this.collectionPresenceCheckerPort = collectionPresenceCheckerPort;
        this.questionQueryRepository = questionQueryRepository;
    }

    @Override
    public ParseResultVO parseLatex(MultipartFile latexFile) {
        Long userId = StpUtil.getLoginIdAsLong();
        try {
            String originalFilename = latexFile.getOriginalFilename();
            InputStream latexSource = latexFile.getInputStream();

            log.info("收到 LaTeX 解析请求，文件名: {}", originalFilename);
            // Step 1: 调用解析器抽取题干/解析及资产占位
            List<QuestionDraftVO> drafts = latexQuestionParser.parse(latexSource, CHAR_SET);

            // Step 2: 写入缓存并生成一次性的导入会话标识，供后续资源上传复用
            String importSessionId = importCacheAdapter.createImportSession(userId, drafts);

            // Step 3: 包装响应
            return ParseResultVO.of(importSessionId, drafts);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("LaTeX 解析失败: {}", e.getMessage(), e);
            throw new ApplicationException(CommonResultCodeEnum.PARAM_VALIDATION_ERROR, "LaTeX 文件解析失败: " + e.getMessage());
        } catch (IOException e) {
            log.error("文件读取异常");
            throw new ApplicationException(CommonResultCodeEnum.PARAM_VALIDATION_ERROR, "LaTeX 文件解析失败: " + e.getMessage());
        }
    }

    @Override
    public void uploadAsset(MultipartFile assetFile, String slotId, String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 1. 获取上传会话
        ImportSession session = importCacheAdapter.getImportSession(sessionId);

        // 2. 校验用户ID和文件
        if (session == null || !session.userId().equals(userId)) {
            throw new ApplicationException(QuestionErrorCodeEnum.IMPORT_SESSION_NOT_FOUND, "进程不存在");
        }

        if (!assetCheckerPort.validateImage(assetFile)) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_ASSET_TYPE_NOT_ALLOWED, "上传的文件不合法");
        }

        // 3. 上传文件并刷新会话
        importCacheAdapter.uploadAsset(session, userId, slotId, assetFile);
    }

    @Override
    @Transactional
    public void confirmUpload(String sessionId, ConfirmDraftReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        // 1. 获取上传会话
        ImportSession session = importCacheAdapter.getImportSession(sessionId);

        // 2. 校验用户ID和文件
        if (session == null || !session.userId().equals(userId)) {
            throw new ApplicationException(QuestionErrorCodeEnum.IMPORT_SESSION_NOT_FOUND, "进程不存在");
        }

        // 3. 将Req传入的改动字段与会话中的缓存组装对象
        Long collectionId = req.collectionId();
        List<ConfirmDraftReq.Update> updates = req.updates();

        if (!collectionPresenceCheckerPort.existsById(collectionId)) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在");
        }

        LocalDateTime now = LocalDateTime.now();

        List<QuestionVersionEntity> versionEntities = session.drafts().stream()
                .map(draft -> {
                    ConfirmDraftReq.Update update = updates.stream()
                            .filter(a -> a.draftId().equals(draft.draftId()))
                            .findFirst()
                            .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED, "参数错误"));

                    // 题型校验
                    if(!QuestionTypeEnum.ensureValid(update.typeCode())) {
                        throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_TYPE_ERROR, "题型不能为空");
                    }

                    return QuestionVersionEntity.builder()
                            .id(HutoolSnowflakeIdGenerator.generateLongId())
                            .questionId(draft.draftId())
                            .versionNo(INITIAL_VERSION)
                            .typeCode(update.typeCode())
                            .title(update.title() == null ? draft.title() : update.title())
                            .stem(update.stem() == null ? draft.stemLatex() : update.stem())
                            .answer(update.answer())
                            .solution(update.solution() == null ? draft.solutionLatex() : update.solution())
                            .assets(assetSnapshotFactory.from(draft.assets(), session.assets()))
                            .createdAt(now)
                            .createdBy(userId)
                            .build();
                }).toList();
        questionVersionRepository.batchSave(versionEntities);

        List<QuestionEntity> entities = versionEntities.stream()
                .map(versionEntity -> {
                    return QuestionEntity.builder()
                            .id(HutoolSnowflakeIdGenerator.generateLongId())
                            .status(QuestionStatusEnum.ACTIVE.getCode())
                            .currentVersionId(versionEntity.getId())
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
                })
                .toList();
        questionRepository.batchSave(entities);

        // 4. 题集关联添加
        int ordinal = questionQueryRepository.getMaxOrdinal(req.collectionId());
        ordinal = ordinal == 0 ? 1 : ordinal + 1;

        List<CollectionItem> items = new ArrayList<>();

        for (QuestionVersionEntity versionEntity : versionEntities) {
            CollectionItem item = CollectionItem.builder()
                    .collectionId(collectionId)
                    .ordinal(ordinal)
                    .questionId(versionEntity.getQuestionId())
                    .questionVersionId(versionEntity.getId())
                    .build();
            items.add(item);
            ordinal++;
        }
        collectionItemRepository.batchSave(items);

    }

    @Override
    public ImportSession getSession(String sessionId) {
        return importCacheAdapter.getImportSession(sessionId);
    }
}
