package cn.sztu.questioncloud.web.rest.v1.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeScopeRepository;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgeExtractService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.req.KnowledgePointDeleteReq;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.req.KnowledgePointVectorSearchReq;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorDetailVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorSearchVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/*
 * RAG相关测试接口，包含知识点抽取、知识点向量化、知识点查询等功能。
 * 暂时不提供对外服务。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rag")
public class TestController {
    private final KnowledgeExtractService questionKnowledgeExtractService;
    private final KnowledgePointService knowledgePointService;
    private final KnowledgePointVectorService knowledgePointVectorService;

    //手动新增知识点领域
    @PostMapping("/knowledge-scope")
    public ResultVO<Void> testAddKnowledgeScope(@RequestParam String scopeName) {
        knowledgePointService.addKnowledgeScope(scopeName);
        return ResultVO.success();
    }

    // 针对某个题目集合进行知识点抽取和绑定
    @PostMapping("/extract/collection/{collectionId}")
    public ResultVO<Map<Long, List<QuestionKnowledgeExtractDTO>>> testExtractCollection(
            @PathVariable Long collectionId) {
        return ResultVO.success(questionKnowledgeExtractService.extractAndBindCollection(collectionId));
    }

    // 为一定数量的知识点缺失的描述和示例等信息进行AI补充
    @PostMapping("/enrich/batch")
    public ResultVO<Void> testEnrichBatch(@RequestParam(defaultValue = "10") Integer limit) {
        questionKnowledgeExtractService.enrichMissingDetails(limit);
        return ResultVO.success();
    }

    // 根据查询条件搜索相关知识点
    @PostMapping("/search")
    public ResultVO<List<KnowledgePointVectorSearchVO>> testSearch(@RequestBody KnowledgePointVectorSearchReq req) {
        return ResultVO.success(
                knowledgePointVectorService.searchCandidates(
                        req.getSubject(),
                        req.getQuery(),
                        req.getTopK(),
                        req.getMinScore()
                )
        );
    }

    // 将所有知识点重新写入向量数据库
    @PostMapping("/knowledge-points/vector/reindex")
    public ResultVO<Integer> testReindexKnowledgePointVectors() {
        return ResultVO.success(knowledgePointVectorService.reindexAll());
    }

    // 根据知识点ID查询向量库中实际保存的文档和元数据
    @GetMapping("/knowledge-points/vector/{knowledgePointId}")
    public ResultVO<KnowledgePointVectorDetailVO> testGetKnowledgePointVectorDetail(
            @PathVariable Long knowledgePointId) {
        return ResultVO.success(knowledgePointVectorService.getVectorDetail(knowledgePointId));
    }

    // 删除知识点及其相关关系
    @DeleteMapping("/knowledge-points")
    public ResultVO<Void> testDeleteKnowledgePoints(@RequestBody KnowledgePointDeleteReq req) {
        knowledgePointService.deleteKnowledgePoints(req.getKnowledgePointIds());
        return ResultVO.success();
    }
}
