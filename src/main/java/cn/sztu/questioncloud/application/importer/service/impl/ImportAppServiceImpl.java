package cn.sztu.questioncloud.application.importer.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.common.port.FilePort;
import cn.sztu.questioncloud.application.importer.dto.ImportErrorReport;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import cn.sztu.questioncloud.application.importer.port.ImportItemRepository;
import cn.sztu.questioncloud.application.importer.port.ImportSessionRepository;
import cn.sztu.questioncloud.application.importer.service.ImportAppService;
import cn.sztu.questioncloud.application.importer.service.ImportDraftValidator;
import cn.sztu.questioncloud.application.importer.service.ImportParseService;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportItemEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportSession;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private final FilePort filePort;
    private final ImportParseService importParseService;

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

    public ImportSessionVO getSession(Long importId) {
        ImportSession session = loadSession(importId);
        ensureOwner(session);
        return toSessionVO(session);
    }

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

    private ImportSession loadSession(Long importId) {
        return importSessionRepository.findById(importId)
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.IMPORT_SESSION_NOT_FOUND, "导入会话不存在"));
    }

    private void ensureOwner(ImportSession session) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION, "无权访问该导入会话");
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
}
