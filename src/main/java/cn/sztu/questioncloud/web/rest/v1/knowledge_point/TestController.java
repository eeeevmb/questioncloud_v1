package cn.sztu.questioncloud.web.rest.v1.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgeExtractService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.req.KnowledgePointDeleteReq;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.testDTO.KnowledgePointVectorOverviewVO;
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
 * RAG 相关测试接口，包含知识点抽取、知识点向量化、知识点查询等功能。
 * 暂时不提供对外服务。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rag")
public class TestController {

    private final KnowledgeExtractService questionKnowledgeExtractService;
    private final KnowledgePointService knowledgePointService;
    private final KnowledgePointVectorService knowledgePointVectorService;

    @PostMapping("/knowledge-scope")
    public ResultVO<Void> testAddKnowledgeScope(@RequestParam String scopeName) {
        knowledgePointService.addKnowledgeScope(scopeName);
        return ResultVO.success();
    }

    @GetMapping("/identify-scope/{questionVersionId}")
    public ResultVO<List<String>> testIdentifyKnowledgeScope(@PathVariable Long questionVersionId) {
        return ResultVO.success(questionKnowledgeExtractService.identifyKnowledgeDomains(questionVersionId));
    }

    @PostMapping("/extract/collection/{collectionId}")
    public ResultVO<Map<Long, List<QuestionKnowledgeExtractDTO>>> testExtractCollection(
            @PathVariable Long collectionId) {
        return ResultVO.success(questionKnowledgeExtractService.extractAndBindCollection(collectionId));
    }

    @PostMapping("/enrich/batch")
    public ResultVO<Void> testEnrichBatch(@RequestParam(defaultValue = "10") Integer limit) {
        questionKnowledgeExtractService.enrichMissingDetails(limit);
        return ResultVO.success();
    }

    @PostMapping("/search")
    public ResultVO<List<KnowledgePointVectorSearchVO>> testSearch(@RequestBody KnowledgePointVectorSearchReq req) {
        return ResultVO.success(
                knowledgePointVectorService.searchCandidates(
                        req.getKnowledgeScopes(),
                        req.getQuery(),
                        req.getTopK(),
                        req.getMinScore()
                )
        );
    }

    @PostMapping("/knowledge-points/vector/reindex")
    public ResultVO<Integer> testReindexKnowledgePointVectors() {
        return ResultVO.success(knowledgePointVectorService.reindexAll());
    }

    @DeleteMapping("/knowledge-points/vector")
    public ResultVO<Integer> testClearKnowledgePointVectors() {
        return ResultVO.success(knowledgePointVectorService.clearKnowledgePointVectors());
    }

    @GetMapping("/knowledge-points/vector/overview")
    public ResultVO<KnowledgePointVectorOverviewVO> testGetKnowledgePointVectorOverview() {
        return ResultVO.success(knowledgePointVectorService.getKnowledgePointVectorOverview());
    }

    @GetMapping("/knowledge-points/vector/{knowledgePointId}")
    public ResultVO<KnowledgePointVectorDetailVO> testGetKnowledgePointVectorDetail(
            @PathVariable Long knowledgePointId) {
        return ResultVO.success(knowledgePointVectorService.getVectorDetail(knowledgePointId));
    }

    @DeleteMapping("/knowledge-points")
    public ResultVO<Void> testDeleteKnowledgePoints(@RequestBody KnowledgePointDeleteReq req) {
        knowledgePointService.deleteKnowledgePoints(req.getKnowledgePointIds());
        return ResultVO.success();
    }
}
