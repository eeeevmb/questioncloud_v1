package cn.sztu.questioncloud.web.rest.v1.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeTagDTO;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.QuestionKnowledgeTagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgePointService knowledgePointService;

    @GetMapping("/questions/{questionVersionId}/tags")
    public ResultVO<List<QuestionKnowledgeTagVO>> listQuestionKnowledgeTags(@PathVariable Long questionVersionId) {
        return ResultVO.success(
                knowledgePointService.listQuestionKnowledgeTags(questionVersionId)
                        .stream()
                        .map(this::toVO)
                        .toList()
        );
    }

    private QuestionKnowledgeTagVO toVO(QuestionKnowledgeTagDTO dto) {
        return QuestionKnowledgeTagVO.builder()
                .canonicalName(dto.getCanonicalName())
                .isMain(dto.getIsMain())
                .build();
    }
}
