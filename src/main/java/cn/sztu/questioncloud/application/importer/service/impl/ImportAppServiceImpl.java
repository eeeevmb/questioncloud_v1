package cn.sztu.questioncloud.application.importer.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.common.port.FilePort;
import cn.sztu.questioncloud.application.importer.dto.CreatedQuestionIds;
import cn.sztu.questioncloud.application.importer.dto.ImportCommitContext;
import cn.sztu.questioncloud.application.importer.dto.ImportErrorReport;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import cn.sztu.questioncloud.application.importer.enums.ImportSessionStatusEnum;
import cn.sztu.questioncloud.application.importer.port.ImportItemRepository;
import cn.sztu.questioncloud.application.importer.port.ImportSessionRepository;
import cn.sztu.questioncloud.application.importer.service.CollectionAutoCreator;
import cn.sztu.questioncloud.application.importer.service.ImportAppService;
import cn.sztu.questioncloud.application.importer.service.ImportDraftValidator;
import cn.sztu.questioncloud.application.importer.service.ImportParseService;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionStatusEnum;
import cn.sztu.questioncloud.application.question.messaging.QuestionEventMessage;
import cn.sztu.questioncloud.application.question.messaging.QuestionEventPublisher;
import cn.sztu.questioncloud.application.question.port.*;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.infrastructure.adapter.utils.QuestionUtils;
import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.*;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.web.rest.v1.importer.query.ImportItemPageQuery;
import cn.sztu.questioncloud.web.rest.v1.importer.req.ImportItemBatchUpdateReq;
import cn.sztu.questioncloud.web.rest.v1.importer.req.ImportSessionCreateReq;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportCreateVO;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportItemVO;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.sztu.questioncloud.application.question.service.impl.QuestionAppServiceImpl.*;

/**
 * 批量导入应用服务实现。
 *
 * @author Codex
 */

@Service
@RequiredArgsConstructor
public class ImportAppServiceImpl implements ImportAppService {
    private final ImportSessionRepository importSessionRepository;
    private final ImportItemRepository importItemRepository;

    private final QuestionCollectionRepository questionCollectionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionStatRepository questionStatRepository;
    private final QuestionQueryRepository queryRepository;
    private final CollectionItemRepository collectionItemRepository;

    private final FilePort filePort;
    private final ImportParseService importParseService;
    private final CollectionAutoCreator collectionAutoCreator;
    private final QuestionEventPublisher questionEventPublisher;

    public ImportCreateVO createImportSession(ImportSessionCreateReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        String identifier = StpUtil.getLoginIdAsString();
        if (!"excel".equalsIgnoreCase(req.getFormat())) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "暂仅支持 Excel 导入");
        }
        InfraFileMetadata metadata = filePort.getFileMetadata(req.getFileId());
        if (metadata == null) {
            throw new ApplicationException(CommonResultCodeEnum.NOT_FOUND, "上传文件不存在");
        }
        if (!identifier.equals(metadata.getFmOwnerIdentifier())) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION, "无权使用该文件");
        }
        LocalDateTime now = LocalDateTime.now();
        ImportSession session = new ImportSession();
        session.setId(HutoolSnowflakeIdGenerator.generateLongId());
        session.setUserId(userId);
        session.setStatus(0);
        session.setFileId(req.getFileId());
        session.setFormat(0);
        session.setTotal(0);
        session.setValidCnt(0);
        session.setInvalidCnt(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        session.setVersion(1);
        importSessionRepository.save(session);
        importParseService.parseAsync(session.getId());
        return ImportCreateVO.builder()
                .importId(session.getId())
                .parseJobId(String.valueOf(session.getId()))
                .build();
    }

    @Override
    public ImportSessionVO getSession(Long importId) {
        ImportSession session = loadSession(importId);
        ensureOwner(session);
        return toSessionVO(session);
    }

    /**
     * 获取当前用户的所有导入会话详情。
     *
     * @param userId 用户ID
     * @return 导入会话视图列表
     */
    @Override
    public List<ImportSessionVO> getSessionList(Long userId) {
        List<ImportSession> importSessions = importSessionRepository.findReadyByUserId(userId);
        return importSessions.stream()
                .map(this::toSessionVO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<ImportItemVO> pageItems(Long importId, ImportItemPageQuery query) {
        ImportSession session = loadSession(importId);
        ensureOwner(session);
        Integer status = parseStatus(query.getStatus());
        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<ImportItemEntity> records = importItemRepository.pageItems(importId, status, offset, query.getPageSize());
        long total = importItemRepository.countByStatus(importId, status);
        List<ImportItemVO> voList = records.stream().map(this::toItemVO).toList();
        return PageResult.of(voList, total, query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ImportItemVO> updateItems(Long importId, ImportItemBatchUpdateReq req) {
        ImportSession session = loadSession(importId);
        ensureOwner(session);
        List<ImportItemVO> result = new ArrayList<>();
        for (ImportItemBatchUpdateReq.Item itemReq : req.getItems()) {
            ImportItemEntity entity = importItemRepository.findById(itemReq.getItemId());
            if (entity == null || !Objects.equals(entity.getImportId(), importId)) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND, "草稿不存在或不属于当前导入会话");
            }
            QuestionDraft draft = itemReq.getDraft();
            List<ImportErrorReport> errors = ImportDraftValidator.validate(entity.getCollectionName(), draft);
            entity.setDraft(draft);
            entity.setErrors(errors);
            entity.setStatus(errors.isEmpty() ? 0 : 1);
            entity.setUpdatedAt(LocalDateTime.now());
            importItemRepository.update(entity);
            result.add(toItemVO(entity));
        }
        recalcAndUpdateCounts(importId);
        return result;
    }

    /**
     * 提交题目草稿。
     *
     * @param importId 导入会话ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void commitImport(Long userId, Long importId, boolean ignoreInvalidDraft) {
        // 验证用户
        ImportSession session = loadSession(importId);
        ensureOwner(session);

        // 尝试取锁
        int updated = importSessionRepository.tryMarkCommitting(
                importId,
                ImportSessionStatusEnum.READY.getCode(),
                ImportSessionStatusEnum.COMMITTING.getCode()
        );

        // 没有取到锁，要么PARSING, 要么COMMITTING，要么COMMITTED或用户CANCELED/FAILED
        if (updated == 0) {
            session = loadSession(importId);

            if (session.getStatus().equals(ImportSessionStatusEnum.COMMITTING.getCode())) {
                throw new ApplicationException(QuestionErrorCodeEnum.IMPORT_SESSION_COMMITTING);
            } else if (session.getStatus().equals(ImportSessionStatusEnum.COMMITTED.getCode())) {
                return;
            } else if (session.getStatus().equals(ImportSessionStatusEnum.PARSING.getCode())) {
                throw new ApplicationException(QuestionErrorCodeEnum.IMPORT_SESSION_PARSING);
            } else if (session.getStatus().equals(ImportSessionStatusEnum.CANCELED.getCode())) {
                throw new ApplicationException(QuestionErrorCodeEnum.IMPORT_SESSION_CANCELED);
            } else if (session.getStatus().equals(ImportSessionStatusEnum.FAILED.getCode())) {
                throw new ApplicationException(QuestionErrorCodeEnum.IMPORT_SESSION_FAILED);
            }

            // 兜底，正常情况下不会触发
            throw new ApplicationException(CommonResultCodeEnum.TOO_MANY_REQUESTS);
        }

        // 抢到锁了，继续提交逻辑
        session = loadSession(importId);

        // 不忽略非法题目时，校验是否存在非法题目
        if (!ignoreInvalidDraft && session.getInvalidCnt() != null && session.getInvalidCnt() > 0) {
            throw new ApplicationException(QuestionErrorCodeEnum.IMPORT_CONTAINS_INVALID_ITEMS);
        }

        // 拉取合法题目草稿列表
        List<ImportItemEntity> drafts = importItemRepository.findValidByImportId(importId);
        if (drafts.isEmpty()) {
            throw new ApplicationException(QuestionErrorCodeEnum.IMPORT_NO_VALID_ITEM);
        }

        ImportCommitContext ctx = prepareContext(userId, drafts);
        List<QuestionEventMessage> messages = new ArrayList<>(drafts.size());

        // 提交题目草稿并构造消息
        for (ImportItemEntity draft : drafts) {
            CreatedQuestionIds ids = commitOne(ctx, draft);
            messages.add(QuestionEventMessage.builder()
                    .questionId(ids.getQuestionId())
                    .versionId(ids.getVersionId())
                    .collectionId(ids.getCollectionId())
                    .ownerId(userId)
                    .occurredAt(ctx.getNow())
                    .build());
        }

        // 发布领域事件
        messages.forEach(questionEventPublisher::publishCreated);

        // 更新草稿状态
//        drafts.forEach(item -> item.setStatus(2));

        session.setStatus(ImportSessionStatusEnum.COMMITTED.getCode());
        importSessionRepository.update(session);
    }

    /**
     * 取消批量导入
     *
     * @param userId   用户ID
     * @param importId 导入会话ID
     */
    @Override
    public void cancelImport(Long userId, Long importId) {
        // 验证用户
        ImportSession session = loadSession(importId);
        ensureOwner(session);

        if (session.getStatus().equals(ImportSessionStatusEnum.READY.getCode())) {
            session.setStatus(ImportSessionStatusEnum.CANCELED.getCode());
            importSessionRepository.update(session);
            return;
        } else if (session.getStatus().equals(ImportSessionStatusEnum.CANCELED.getCode())) {
            return;
        }

        throw new ApplicationException(QuestionErrorCodeEnum.IMPORT_SESSION_CANNOT_CANCEL);
    }

    private ImportSession loadSession(Long importId) {
        return importSessionRepository.findById(importId)
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.IMPORT_SESSION_NOT_FOUND));
    }

    private void ensureOwner(ImportSession session) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }
    }

    private ImportSessionVO toSessionVO(ImportSession session) {
        BigDecimal total = session.getTotal() == null ? BigDecimal.ZERO : BigDecimal.valueOf(session.getTotal());
        BigDecimal finished = BigDecimal.valueOf(
                (session.getValidCnt() == null ? 0 : session.getValidCnt())
                        + (session.getInvalidCnt() == null ? 0 : session.getInvalidCnt()));
        BigDecimal progress = total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : finished.divide(total, 4, RoundingMode.HALF_UP);
        return ImportSessionVO.builder()
                .importId(session.getId())
                .status(session.getStatus())
                .total(session.getTotal())
                .validCnt(session.getValidCnt())
                .invalidCnt(session.getInvalidCnt())
                .progress(progress)
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private ImportItemVO toItemVO(ImportItemEntity entity) {
        return ImportItemVO.builder()
                .itemId(entity.getId())
                .indexNo(entity.getIndexNo())
                .collectionName(entity.getCollectionName())
                .draft(entity.getDraft())
                .status(entity.getStatus())
                .errors(entity.getErrors())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Integer parseStatus(String status) {
        if (status == null || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "VALID" -> 0;
            case "INVALID" -> 1;
            default -> null;
        };
    }

    private void recalcAndUpdateCounts(Long importId) {
        long valid = importItemRepository.countByStatus(importId, 0);
        long invalid = importItemRepository.countByStatus(importId, 1);
        importSessionRepository.updateCounts(importId, (int) valid, (int) invalid);
    }

    // 准备提交的上下文
    private ImportCommitContext prepareContext(Long userId, List<ImportItemEntity> drafts) {
        LocalDateTime now = LocalDateTime.now();

        Map<String, Long> collectionIdByName = questionCollectionRepository.getMapsByUserId(userId);

        Set<Long> collectionIds = drafts.stream()
                .map(ImportItemEntity::getCollectionName)
                .map(name -> name == null ? "" : name.trim())
                .map(name -> {
                    Long id = collectionIdByName.get(name);
                    if (id != null) {
                        return id;
                    }
                    Long createdId = collectionAutoCreator.ensureCollection(userId, name).getId();
                    collectionIdByName.put(name, createdId);
                    return createdId;
                })
                .collect(Collectors.toSet());

        Map<Long, Integer> nextOrdinalByCollectionId = new HashMap<>();
        for (Long cid : collectionIds) {
            int max = queryRepository.getMaxOrdinal(cid);
            nextOrdinalByCollectionId.put(cid, max + 1);
        }

        return new ImportCommitContext(userId, collectionIdByName, nextOrdinalByCollectionId, now);
    }

    // 把一个 draft 落库并返回创建的题目相关ID
    private CreatedQuestionIds commitOne(ImportCommitContext ctx, ImportItemEntity item) {
        QuestionDraft d = item.getDraft();

        String name = item.getCollectionName() == null ? "" : item.getCollectionName().trim();
        Long collectionId = ctx.getCollectionIdByName().get(name);
        if (collectionId == null) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "题集不存在: " + item.getCollectionName());
        }

        Long questionId = HutoolSnowflakeIdGenerator.generateLongId();
        Long versionId  = HutoolSnowflakeIdGenerator.generateLongId();

        LocalDateTime now = ctx.getNow();

        QuestionEntity q = QuestionEntity.builder()
                .id(questionId)
                .status(QuestionStatusEnum.ACTIVE.getCode())
                .currentVersionId(versionId)
                .ownerId(ctx.getUserId())
                .createdAt(now)
                .updatedAt(now)
                .build();

        QuestionVersionEntity v = QuestionVersionEntity.builder()
                .id(versionId)
                .questionId(questionId)
                .versionNo(INITIAL_VERSION)
                .typeCode(d.getTypeCode())
                .title(d.getTitle())
                .stem(d.getStem())
                .options(d.getOptions())
                .answer(d.getAnswer())
                .answerKey(QuestionUtils.getAnswerKey(d.getTypeCode(), d.getCorrectOptions(), d.getJudgeAnswer()))
                .solution(d.getSolution())
                .createdBy(ctx.getUserId())
                .createdAt(now)
                .build();

        questionRepository.save(q);
        questionVersionRepository.save(v);

        // ordinal：用 ctx 的缓存，避免每题查 maxOrdinal
        int ordinal = ctx.getNextOrdinalByCollectionId().compute(collectionId, (k, oldVal) -> {
            int cur = (oldVal == null ? 1 : oldVal);
            return cur + 1;
        }) - 1;

        CollectionItem ci = CollectionItem.builder()
                .collectionId(collectionId)
                .ordinal(ordinal)
                .questionId(questionId)
                .questionVersionId(versionId)
                .build();
        collectionItemRepository.save(ci);

        QuestionStat stat = QuestionStat.builder()
                .questionId(questionId)
                .versionId(versionId)
                .attempts(INITIAL_COUNT)
                .correctCount(INITIAL_COUNT)
                .correctRate(null)
                .difficulty(d.getDifficulty() == null ? null : d.getDifficulty().doubleValue())
                .exposureFactor(INITIAL_EXP)
                .lastExposedAt(now)
                .updatedAt(now)
                .build();
        questionStatRepository.save(stat);

        return new CreatedQuestionIds(questionId, versionId, collectionId);
    }
}
