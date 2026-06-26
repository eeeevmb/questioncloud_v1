package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.sztu.questioncloud.application.knowledge_point.dto.DirectoryKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.TextbookCheckDTO;
import cn.sztu.questioncloud.application.knowledge_point.enums.KnowledgeSourceTypeEnum;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeQuestionRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeScopeRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.TextbookRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.TextbookKnowledgeRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgeExtractService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgeImportService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.TextbookEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.TextbookKnowledgeRelEntity;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.req.KnowledgeImportConfirmReq;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.req.KnowledgeImportPreviewReq;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgeImportConfirmVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgeImportPreviewVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowledgeImportServiceImpl implements KnowledgeImportService {

    private final KnowledgeScopeRepository knowledgeScopeRepository;
    private final KnowledgeExtractService knowledgeExtractService;
    private final TextbookRepository textbookRepository;
    private final TextbookKnowledgeRelRepository textbookKnowledgeRelRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeQuestionRelRepository knowledgeQuestionRelRepository;
    private final KnowledgePointVectorService knowledgePointVectorService;
    private final KnowledgePointService knowledgePointService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeImportPreviewVO preview(KnowledgeImportPreviewReq req) {
        // 1. 验证知识范围是否存在
        KnowledgeScopeEntity knowledgeScope = getOrCreateKnowledgeScope(req.getKnowledgeScope());
        TextbookCheckDTO duplicateCheckDTO = buildTextbookDuplicateCheckDTO(
                knowledgeScope.getId(),
                req.getTextbookName(),
                req.getAuthor()
        );
        TextbookEntity duplicateTextbook = textbookRepository.getDuplicate(duplicateCheckDTO);

        // 2. 渲染目录文本并提取知识点
        String directoryText = renderDirectoryText(
                req.getKnowledgeScope(),
                req.getTextbookName(),
                req.getEdition(),
                req.getAuthor(),
                req.getPublisher(),
                req.getIsbn(),
                req.getItems()
        );
        List<DirectoryKnowledgeExtractDTO> knowledgePoints =
                knowledgeExtractService.extractFromDirectoryText(directoryText);
        // 3. 构建预览结果
        return KnowledgeImportPreviewVO.builder()
                .textbookName(req.getTextbookName())
                .knowledgeScope(req.getKnowledgeScope())
                .knowledgeScopeId(knowledgeScope.getId())
                .textbookExists(duplicateTextbook != null)
                .existingTextbookId(duplicateTextbook == null ? null : duplicateTextbook.getId())
                .items(knowledgePoints.stream()
                        .map(kp -> KnowledgePointVO.builder()
                                .canonicalName(kp.getCanonicalName())
                                .build())
                        .toList())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeImportConfirmVO confirm(KnowledgeImportConfirmReq req, Long userId) {
        // 1. 验证请求参数
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "确认导入的知识点不能为空");
        }
        KnowledgeScopeEntity knowledgeScope = getOrCreateKnowledgeScope(req.getKnowledgeScope());
        TextbookCheckDTO duplicateCheckDTO = buildTextbookDuplicateCheckDTO(
                knowledgeScope.getId(),
                req.getTextbookName(),
                req.getAuthor()
        );
        TextbookEntity textbook = textbookRepository.getDuplicate(duplicateCheckDTO);
        LocalDateTime now = LocalDateTime.now();
        // 2. 创建或更新教材信息
        if (textbook == null) {
            textbook = TextbookEntity.builder()
                    .knowledgeScopeId(knowledgeScope.getId())
                    .textbookName(req.getTextbookName())
                    .edition(req.getEdition())
                    .author(req.getAuthor())
                    .publisher(req.getPublisher())
                    .isbn(req.getIsbn())
                    .createdBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            textbookRepository.save(textbook);
        } else {
            overwriteTextbookKnowledgeRelations(textbook.getId());
            textbook.setEdition(req.getEdition());
            textbook.setAuthor(req.getAuthor());
            textbook.setPublisher(req.getPublisher());
            textbook.setIsbn(req.getIsbn());
            textbook.setUpdatedAt(now);
            textbookRepository.update(textbook);
        }
        // 3. 创建知识点并建立教材-知识点关系
        int newKnowledgePointCount = 0;
        int reusedKnowledgePointCount = 0;
        int relationSavedCount = 0;
        Set<Long> savedKnowledgePointIds = new LinkedHashSet<>();

        for (KnowledgeImportConfirmReq.Item item : req.getItems()) {
            if (item == null || item.getCanonicalName() == null || item.getCanonicalName().isBlank())
                continue;
            FindOrCreateKnowledgePointResult result = findOrCreateKnowledgePoint(
                    knowledgeScope.getId(),
                    item.getCanonicalName()
            );
            if (result.created())
                newKnowledgePointCount++;
            else
                reusedKnowledgePointCount++;

            if (!savedKnowledgePointIds.add(result.entity().getId()))
                continue;

            textbookKnowledgeRelRepository.save(TextbookKnowledgeRelEntity.builder()
                    .textbookId(textbook.getId())
                    .knowledgePointId(result.entity().getId())
                    .sourceType(KnowledgeSourceTypeEnum.AI_REVIEWED_IMPORT.getCode())
                    .createdBy(userId)
                    .createdAt(now)
                    .updatedAt(now)
                    .isDeleted(0)
                    .build());
            relationSavedCount++;
        }
        // 4. 构建确认结果
        return KnowledgeImportConfirmVO.builder()
                .textbookId(textbook.getId())
                .knowledgeScopeId(knowledgeScope.getId())
                .selectedCount(req.getItems().size())
                .newKnowledgePointCount(newKnowledgePointCount)
                .reusedKnowledgePointCount(reusedKnowledgePointCount)
                .relationSavedCount(relationSavedCount)
                .build();
    }

    // ==================== Private Helper Methods ==================== //
    // 获取或创建知识点领域
    private KnowledgeScopeEntity getOrCreateKnowledgeScope(String knowledgeScopeName) {
        String normalizedScopeName = Optional.ofNullable(knowledgeScopeName)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .orElseThrow(() -> new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "知识点领域不能为空"));

        KnowledgeScopeEntity knowledgeScope = knowledgeScopeRepository.getByScopeName(normalizedScopeName);
        if (knowledgeScope != null) {
            return knowledgeScope;
        }

        knowledgePointService.addKnowledgeScope(normalizedScopeName);
        return knowledgeScopeRepository.getByScopeName(normalizedScopeName);
    }

    // 建立教材重复检查 DTO
    private TextbookCheckDTO buildTextbookDuplicateCheckDTO(Long knowledgeScopeId,
                                                            String textbookName,
                                                            String author) {
        return TextbookCheckDTO.builder()
                .knowledgeScopeId(knowledgeScopeId)
                .textbookName(textbookName)
                .author(author)
                .build();
    }

    // 覆盖教材与知识点的关系，并删除不再使用的知识点
    private void overwriteTextbookKnowledgeRelations(Long textbookId) {
        List<TextbookKnowledgeRelEntity> oldRelations = textbookKnowledgeRelRepository.listByTextbookId(textbookId);
        if (oldRelations == null || oldRelations.isEmpty()) {
            return;
        }

        Set<Long> oldKnowledgePointIds = oldRelations.stream()
                .map(TextbookKnowledgeRelEntity::getKnowledgePointId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        textbookKnowledgeRelRepository.deleteByTextbookId(textbookId);

        for (Long knowledgePointId : oldKnowledgePointIds) {
            boolean stillUsedByTextbook = textbookKnowledgeRelRepository.existsByKnowledgePointId(knowledgePointId);
            boolean stillUsedByQuestion = knowledgeQuestionRelRepository.existsByKnowledgePointId(knowledgePointId);
            if (!stillUsedByTextbook && !stillUsedByQuestion) {
                knowledgePointService.deleteKnowledgePoints(List.of(knowledgePointId));
            }
        }
    }

    // 获取或创建知识点
    private FindOrCreateKnowledgePointResult findOrCreateKnowledgePoint(Long knowledgeScopeId, String canonicalName) {
        String normalizedName = Optional.ofNullable(canonicalName)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .orElseThrow(() -> new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "知识点名称不能为空"));

        List<KnowledgePointEntity> candidates = Optional.ofNullable(
                knowledgePointRepository.listByCanonicalNameOrAlias(knowledgeScopeId, normalizedName)
        ).orElse(List.of());

        Optional<KnowledgePointEntity> matched = candidates.stream()
                .filter(candidate -> hasSameCanonicalOrAlias(candidate, normalizedName))
                .findFirst();

        if (matched.isPresent()) {
            return new FindOrCreateKnowledgePointResult(matched.get(), false);
        }

        LocalDateTime now = LocalDateTime.now();
        KnowledgePointEntity entity = KnowledgePointEntity.builder()
                .knowledgeScopeId(knowledgeScopeId)
                .canonicalName(normalizedName)
                .sourceType(KnowledgeSourceTypeEnum.AI_REVIEWED_IMPORT.getCode())
                .createdAt(now)
                .updatedAt(now)
                .build();
        knowledgePointRepository.save(entity);
        knowledgePointVectorService.upsert(entity);
        return new FindOrCreateKnowledgePointResult(entity, true);
    }

    // 判断知识点实体的规范名称或别名是否与给定的规范名称相同
    private boolean hasSameCanonicalOrAlias(KnowledgePointEntity entity, String normalizedName) {
        String canonicalName = Optional.ofNullable(entity.getCanonicalName())
                .map(String::trim)
                .orElse("");
        if (canonicalName.equals(normalizedName)) {
            return true;
        }
        return Optional.ofNullable(entity.getAliases())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .anyMatch(normalizedName::equals);
    }

    private record FindOrCreateKnowledgePointResult(KnowledgePointEntity entity, boolean created) {
    }

    private String renderDirectoryText(String knowledgeScope,
                                       String textbookName,
                                       String edition,
                                       String author,
                                       String publisher,
                                       String isbn,
                                       List<KnowledgeImportPreviewReq.DirectoryItem> items) {
        StringBuilder builder = new StringBuilder();

        appendLine(builder, "科目", knowledgeScope);
        appendLine(builder, "教材", textbookName);
        appendLine(builder, "版本", edition);
        appendLine(builder, "作者", author);
        appendLine(builder, "出版社", publisher);
        appendLine(builder, "ISBN", isbn);
        builder.append('\n');

        builder.append("下面是教材目录项，每一行格式为：层级 | 标题 | 目录路径。\n");
        builder.append("请从这些目录项中提取适合作为知识点库实体的知识点。\n\n");
        builder.append("目录：\n");
        builder.append("层级 | 标题 | 目录路径\n");

        for (KnowledgeImportPreviewReq.DirectoryItem item : items) {
            if (item == null) {
                continue;
            }

            String title = cleanSeparator(item.getTitle());
            String directoryPath = cleanSeparator(item.getDirectoryPath());
            if (title.isBlank() || directoryPath.isBlank()) {
                continue;
            }

            Integer level = item.getLevel() == null ? 3 : item.getLevel();
            builder.append(level)
                    .append(" | ")
                    .append(title)
                    .append(" | ")
                    .append(directoryPath)
                    .append('\n');
        }

        return builder.toString();
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label)
                .append("：")
                .append(value.trim())
                .append('\n');
    }

    private String cleanSeparator(String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replace("|", " ")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
