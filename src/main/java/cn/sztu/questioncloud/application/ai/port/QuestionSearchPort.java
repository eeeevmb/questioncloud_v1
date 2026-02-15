package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.common.dto.SearchFilter;

import java.util.List;

public interface QuestionSearchPort {
    List<QuestionHitDTO> searchQuestions(String query, SearchFilter filter);
}
