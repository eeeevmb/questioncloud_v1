package cn.sztu.questioncloud.application.ai.service.impl;

import cn.sztu.questioncloud.application.ai.port.LlmPort;
import cn.sztu.questioncloud.application.ai.service.AiGenerateService;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiGenerateServiceImpl implements AiGenerateService {
    private final LlmPort llmPort;

    @Override
    public QuestionDraft generateQuestionDraft(String userInput) {
        return llmPort.generateQuestionDraft(userInput);
    }
}
