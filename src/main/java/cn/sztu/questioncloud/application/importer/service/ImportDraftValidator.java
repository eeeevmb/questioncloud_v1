package cn.sztu.questioncloud.application.importer.service;

import cn.hutool.core.util.StrUtil;
import cn.sztu.questioncloud.application.importer.dto.ImportErrorReport;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionOption;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 导入草稿校验器。
 *
 * @author Codex
 */
public class ImportDraftValidator {
    private ImportDraftValidator() {}

    public static List<ImportErrorReport> validate(String collectionName, QuestionDraft draft) {
        return validate(collectionName, draft, null);
    }

    public static List<ImportErrorReport> validate(String collectionName, QuestionDraft draft, String rawTypeLabel) {
        List<ImportErrorReport> errors = new ArrayList<>();
        if (StrUtil.isBlank(collectionName)) {
            errors.add(ImportErrorReport.builder()
                    .field("collectionName")
                    .code("REQUIRED")
                    .message("题库名不能为空")
                    .build());
        }
        if (draft == null) {
            errors.add(ImportErrorReport.builder()
                    .field("draft")
                    .code("INVALID")
                    .message("题目数据缺失")
                    .build());
            return errors;
        }
        boolean hasTypeCode = StrUtil.isNotBlank(draft.getTypeCode());
        if (!hasTypeCode) {
            if (StrUtil.isNotBlank(rawTypeLabel)) {
                errors.add(error("typeCode", "UNSUPPORTED", "不支持的题型：" + rawTypeLabel));
            } else {
                errors.add(error("typeCode", "REQUIRED", "题型不能为空"));
            }
        }
        checkRequired(draft.getStem(), "stem", "题干", errors);
        if (draft.getDifficulty() == null) {
            errors.add(error("difficulty", "REQUIRED", "难度系数不能为空"));
        } else if (!inRange(draft.getDifficulty())) {
            errors.add(error("difficulty", "INVALID", "难度系数取值应在[0,1]"));
        }
        if (!hasTypeCode) {
            return errors;
        }
        switch (draft.getTypeCode()) {
            case "single-choice", "multiple-choice" -> validateChoice(draft, errors);
            case "true-false" -> validateJudge(draft, errors);
            case "fill-in", "short-answer" -> {
                if (StrUtil.isBlank(draft.getAnswer())) {
                    errors.add(error("answer", "REQUIRED", "答案不能为空"));
                }
            }
            default -> errors.add(error("typeCode", "UNSUPPORTED", "不支持的题型：" + draft.getTypeCode()));
        }
        return errors;
    }

    private static void validateChoice(QuestionDraft draft, List<ImportErrorReport> errors) {
        List<QuestionOption> options = draft.getOptions();
        if (options == null || options.stream().filter(opt -> StrUtil.isNotBlank(opt.getContent())).count() < 2) {
            errors.add(error("options", "INVALID", "选择题至少需要2个选项"));
            return;
        }
        // key 必须唯一且非空
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (QuestionOption option : options) {
            if (StrUtil.isBlank(option.getKey()) || StrUtil.isBlank(option.getContent())) {
                errors.add(error("options", "INVALID", "选项必须包含非空的 key 与内容"));
                return;
            }
            String upperKey = option.getKey().trim().toUpperCase();
            if (!keys.add(upperKey)) {
                errors.add(error("options", "INVALID", "选项 key 不可重复"));
                return;
            }
            option.setKey(upperKey);
        }
        List<String> correct = normalizeCorrectOptions(draft.getCorrectOptions());
        draft.setCorrectOptions(correct);
        if (correct == null || correct.isEmpty()) {
            errors.add(error("correctOptions", "REQUIRED", "请选择正确答案"));
            return;
        }
        Set<String> available = options.stream()
                .map(QuestionOption::getKey)
                .collect(java.util.stream.Collectors.toSet());
        if (!available.containsAll(correct)) {
            errors.add(error("correctOptions", "INVALID", "正确答案必须在选项范围内"));
        }
        if ("single-choice".equals(draft.getTypeCode()) && correct.size() != 1) {
            errors.add(error("correctOptions", "INVALID", "单选题只能有一个正确答案"));
        }
    }

    private static void validateJudge(QuestionDraft draft, List<ImportErrorReport> errors) {
        if (StrUtil.isBlank(draft.getJudgeAnswer())) {
            errors.add(error("judgeAnswer", "REQUIRED", "判断题答案不能为空"));
            return;
        }
        String ans = draft.getJudgeAnswer();
        if (!"T".equals(ans) && !"F".equals(ans)) {
            errors.add(error("judgeAnswer", "INVALID", "判断题答案必须为 T 或 F"));
        }
    }

    private static void checkRequired(String value, String field, String label, List<ImportErrorReport> errors) {
        if (StrUtil.isBlank(value)) {
            errors.add(error(field, "REQUIRED", label + "不能为空"));
        }
    }

    private static boolean inRange(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) >= 0 && value.compareTo(BigDecimal.ONE) <= 0;
    }

    private static ImportErrorReport error(String field, String code, String message) {
        return ImportErrorReport.builder()
                .field(field)
                .code(code)
                .message(message)
                .build();
    }

    private static List<String> normalizeCorrectOptions(List<String> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return inputs;
        }
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        for (String raw : inputs) {
            if (StrUtil.isBlank(raw)) {
                continue;
            }
            String cleaned = raw.trim().toUpperCase();
            if (cleaned.length() > 1 && cleaned.chars().allMatch(Character::isLetter)) {
                for (char c : cleaned.toCharArray()) {
                    result.add(String.valueOf(c));
                }
            } else if (cleaned.contains(",")) {
                for (String part : cleaned.split(",")) {
                    if (StrUtil.isNotBlank(part)) {
                        result.add(part.trim().toUpperCase());
                    }
                }
            } else {
                result.add(cleaned);
            }
        }
        return new java.util.ArrayList<>(result);
    }
}
