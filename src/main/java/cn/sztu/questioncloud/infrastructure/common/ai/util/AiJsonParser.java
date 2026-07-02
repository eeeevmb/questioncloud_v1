package cn.sztu.questioncloud.infrastructure.common.ai.util;

import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiJsonParser {

    private final ObjectMapper objectMapper;

    public <T> T parseArray(String text, TypeReference<T> typeReference, String scene) {
        return parseJson(text, '[', ']', typeReference, scene);
    }

    public <T> T parseObject(String text, TypeReference<T> typeReference, String scene) {
        return parseJson(text, '{', '}', typeReference, scene);
    }

    private <T> T parseJson(
            String text,
            char startChar,
            char endChar,
            TypeReference<T> typeReference,
            String scene
    ) {
        if (text == null || text.isBlank()) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    scene + "为空"
            );
        }

        String trimmed = text.trim();
        int start = trimmed.indexOf(startChar);
        int end = trimmed.lastIndexOf(endChar);

        if (start < 0 || end < start) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    scene + "不是合法JSON: " + trimmed
            );
        }

        String json = trimmed.substring(start, end + 1);

        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    scene + "解析失败: " + trimmed
            );
        }
    }
}