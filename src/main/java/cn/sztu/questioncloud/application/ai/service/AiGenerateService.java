package cn.sztu.questioncloud.application.ai.service;

import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;

public interface AiGenerateService {
    QuestionDraft generateQuestionDraft(String userInput);
}
