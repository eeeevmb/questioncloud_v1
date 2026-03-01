package cn.sztu.questioncloud.application.importer.service;

import cn.hutool.core.util.StrUtil;
import cn.sztu.questioncloud.application.importer.dto.ImportErrorReport;
import cn.sztu.questioncloud.application.importer.dto.ImportExcelRow;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import cn.sztu.questioncloud.application.importer.port.ImportItemRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionOption;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportItemEntity;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导入会话解析监听器。
 *
 * @author Codex
 */
@Slf4j
public class ImportExcelListener implements ReadListener<ImportExcelRow> {
    private static final Map<String, String> TYPE_MAPPING = Map.of(
            "单选题", "single-choice",
            "多选题", "multiple-choice",
            "判断题", "true-false",
            "填空题", "fill-in",
            "简答题", "short-answer"
    );
    private static final String DEFAULT_COLLECTION_NAME = "默认题集";

    private final ImportItemRepository importItemRepository;
    private final CollectionAutoCreator collectionAutoCreator;
    private final Long importId;
    private final Long ownerId;
    private final int batchSize;
    private final List<ImportItemEntity> buffer = new ArrayList<>();
    private final Map<String, Boolean> ensuredCollections = new ConcurrentHashMap<>();
    private int index = 1;
    @Getter
    private int total;
    @Getter
    private int valid;
    @Getter
    private int invalid;

    public ImportExcelListener(ImportItemRepository importItemRepository,
                               CollectionAutoCreator collectionAutoCreator,
                               Long importId,
                               Long ownerId,
                               int batchSize) {
        this.importItemRepository = importItemRepository;
        this.collectionAutoCreator = collectionAutoCreator;
        this.importId = importId;
        this.ownerId = ownerId;
        this.batchSize = batchSize;
    }

    @Override
    public void invoke(ImportExcelRow row, AnalysisContext context) {
        if (row == null || row.isEmptyRow()) {
            return;
        }
        try {
            handleRow(row);
        } catch (Exception ex) {
            log.warn("处理导入行失败，importId={}，indexNo={}", importId, index, ex);
            recordRowFailure(row, ex);
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        flush();
    }

    public void flush() {
        if (buffer.isEmpty()) {
            return;
        }
        importItemRepository.batchSave(new ArrayList<>(buffer));
        buffer.clear();
    }

    private void ensureCollection(String name) {
        ensuredCollections.computeIfAbsent(name, key -> {
            collectionAutoCreator.ensureCollection(ownerId, key);
            return Boolean.TRUE;
        });
    }

    private void handleRow(ImportExcelRow row) {
        String collectionName = normalizeCollectionName(row.getCollectionName());
        String originalType = normalizeTypeLabel(row.getType());
        String typeCode = resolveTypeCode(originalType);

        QuestionDraft draft = buildDraft(row, typeCode);
        List<ImportErrorReport> errors = ImportDraftValidator.validate(collectionName, draft, originalType);

        String persistedName = persistDraft(collectionName, draft, errors);
        if (errors.isEmpty() && StrUtil.isNotBlank(persistedName)) {
            ensureCollection(persistedName);
        }
    }

    private void recordRowFailure(ImportExcelRow row, Exception ex) {
        ImportErrorReport error = ImportErrorReport.builder()
                .field("row")
                .code("EXCEPTION")
                .message(buildExceptionMessage(ex))
                .build();
        persistDraft(normalizeCollectionName(row.getCollectionName()), new QuestionDraft(), List.of(error));
    }

    private String persistDraft(String collectionName, QuestionDraft draft, List<ImportErrorReport> errors) {
        String persistedName = StrUtil.blankToDefault(collectionName, DEFAULT_COLLECTION_NAME);
        ImportItemEntity entity = ImportItemEntity.builder()
                .id(HutoolSnowflakeIdGenerator.generateLongId())
                .importId(importId)
                .collectionName(persistedName)
                .indexNo(index++)
                .draft(draft)
                .status(errors.isEmpty() ? 0 : 1)
                .errors(errors)
                .updatedAt(LocalDateTime.now())
                .build();
        total++;
        if (errors.isEmpty()) {
            valid++;
        } else {
            invalid++;
        }
        buffer.add(entity);
        if (buffer.size() >= batchSize) {
            flush();
        }
        return persistedName;
    }

    private String normalizeCollectionName(String raw) {
        String trimmed = StrUtil.trim(raw);
        return StrUtil.blankToDefault(trimmed, DEFAULT_COLLECTION_NAME);
    }

    private String normalizeTypeLabel(String rawType) {
        String trimmed = StrUtil.trim(rawType);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }

    private String resolveTypeCode(String originalType) {
        if (originalType == null) {
            return null;
        }
        return TYPE_MAPPING.get(originalType);
    }

    private String buildExceptionMessage(Exception ex) {
        String message = ex.getMessage();
        if (StrUtil.isBlank(message)) {
            message = ex.getClass().getSimpleName();
        }
        return "系统解析异常：" + message;
    }

    private QuestionDraft buildDraft(ImportExcelRow row, String typeCode) {
        QuestionDraft draft = new QuestionDraft();
        draft.setTypeCode(typeCode);
        draft.setTitle(trim(row.getTitle()));
        draft.setStem(trim(row.getStem()));
        draft.setSolution(trim(row.getSolution()));
        draft.setDifficulty(parseDifficulty(row.getDifficulty()));
        if (isChoiceType(draft.getTypeCode())) {
            draft.setOptions(buildOptions(row));
            draft.setCorrectOptions(parseChoiceAnswer(row.getChoiceAnswer()));
        } else {
            draft.setOptions(Collections.emptyList());
        }
        switch (draft.getTypeCode() == null ? "" : draft.getTypeCode()) {
            case "fill-in" -> draft.setAnswer(trim(row.getCorrectAnswer()));
            case "short-answer" -> draft.setAnswer(trim(row.getCorrectAnswer()));
            case "true-false" -> {
                String normalized = normalizeJudgeAnswer(row.getJudgeAnswer());
                draft.setJudgeAnswer(normalized);
                draft.setAnswer(normalized);
            }
            case "single-choice", "multiple-choice" -> draft.setAnswer(String.join("", draft.getCorrectOptions()));
            default -> draft.setAnswer(trim(row.getCorrectAnswer()));
        }
        return draft;
    }

    private boolean isChoiceType(String typeCode) {
        return "single-choice".equals(typeCode) || "multiple-choice".equals(typeCode);
    }

    private List<QuestionOption> buildOptions(ImportExcelRow row) {
        List<QuestionOption> options = new ArrayList<>();
        addOption(options, "A", row.getOptionA());
        addOption(options, "B", row.getOptionB());
        addOption(options, "C", row.getOptionC());
        addOption(options, "D", row.getOptionD());
        addOption(options, "E", row.getOptionE());
        addOption(options, "F", row.getOptionF());
        return options;
    }

    private void addOption(List<QuestionOption> options, String key, String content) {
        if (StrUtil.isBlank(content)) {
            return;
        }
        QuestionOption option = new QuestionOption();
        option.setKey(key);
        option.setContent(content.trim());
        options.add(option);
    }

    private List<String> parseChoiceAnswer(String raw) {
        if (StrUtil.isBlank(raw)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (char c : raw.toCharArray()) {
            if (Character.isLetter(c)) {
                result.add(String.valueOf(Character.toUpperCase(c)));
            }
        }
        return result;
    }

    private String normalizeJudgeAnswer(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String val = raw.trim().toUpperCase();
        if ("对".equals(val) || "TRUE".equals(val) || "T".equals(val)) {
            return "T";
        }
        if ("错".equals(val) || "FALSE".equals(val) || "F".equals(val)) {
            return "F";
        }
        return val;
    }

    private BigDecimal parseDifficulty(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
