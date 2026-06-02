package cn.sztu.questioncloud.web.rest.v1.knowledge_point;

import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.ai.service.SearchService;
import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgePointExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.service.QuestionKnowledgeExtractService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.RAGSearchParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rag")
public class RagController {
    private final QuestionKnowledgeExtractService questionKnowledgeExtractService;

    /*
     * 测试用接口，数据暂时不使用VO封装 -- RAG知识点抽取&存储接口 --
     */
    @PostMapping("/extract/{questionVersionId}")
    public ResultVO<List<KnowledgePointExtractDTO>> testExtract(@PathVariable Long questionVersionId){
        return ResultVO.success(questionKnowledgeExtractService.extractAndBind(questionVersionId));
    }
}
