package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgePointExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.application.knowledge_point.service.QuestionKnowledgeExtractService;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.ai.service.KnowledgeExtractorAiService;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionOption;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionKnowledgeExtractServiceImpl implements QuestionKnowledgeExtractService {
    private final ObjectMapper objectMapper;
    private final QuestionVersionRepository questionVersionRepository;
    private final KnowledgeExtractorAiService knowledgeExtractorAiService;
    private final KnowledgePointService knowledgePointService;

    @Override
    public List<KnowledgePointExtractDTO> extractAndBind(Long questionVersionId) {
        QuestionVersionEntity questionVersion = questionVersionRepository.getVersionById(questionVersionId);
        if (questionVersion == null) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND, "题目版本不存在");
        }

        String questionText = buildQuestionText(questionVersion);
        String extractedPointsText = knowledgeExtractorAiService.extractKnowledgePoints(questionText);
        List<KnowledgePointExtractDTO> extractedPoints = parseExtractResult(extractedPointsText);
        if (extractedPoints == null || extractedPoints.isEmpty()) {
            return List.of();
        }

        knowledgePointService.bindExtractedKnowledgePoints(
                questionVersion.getQuestionId(),
                questionVersion.getId(),
                extractedPoints
        );
        return extractedPoints;
    }

    private String buildQuestionText(QuestionVersionEntity version) {
        StringBuilder builder = new StringBuilder();

        appendLine(builder, "题型", version.getTypeCode());
        appendLine(builder, "标题", version.getTitle());
        appendLine(builder, "题干", version.getStem());

        if (version.getOptions() != null && !version.getOptions().isEmpty()) {
            builder.append("选项：\n");
            for (QuestionOption option : version.getOptions()) {
                builder.append(nullToEmpty(option.getKey()))
                        .append(". ")
                        .append(nullToEmpty(option.getContent()))
                        .append("\n");
            }
            builder.append("\n");
        }

        appendLine(builder, "答案", version.getAnswer());
        appendLine(builder, "判分答案", version.getAnswerKey());
        appendLine(builder, "解析", version.getSolution());

        return builder.toString();
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        builder.append(label)
                .append("：\n")
                .append(value)
                .append("\n\n");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private List<KnowledgePointExtractDTO> parseExtractResult(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String trimmed = text.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');

        if (start < 0 || end < start) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "AI知识点提取结果不是JSON数组"
            );
        }
        String json = trimmed.substring(start, end + 1);

        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<KnowledgePointExtractDTO>>() {}
            );
        } catch (JsonProcessingException e) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "AI知识点提取结果解析失败"
            );
        }
    }
}
