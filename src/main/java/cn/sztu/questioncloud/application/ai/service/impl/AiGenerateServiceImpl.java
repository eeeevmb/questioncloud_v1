package cn.sztu.questioncloud.application.ai.service.impl;

import cn.sztu.questioncloud.application.ai.dto.BucketCandidateItem;
import cn.sztu.questioncloud.application.ai.dto.CandidateBucket;
import cn.sztu.questioncloud.application.ai.dto.KnowledgePointHitDTO;
import cn.sztu.questioncloud.application.ai.dto.PaperGenerationPlan;
import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.ai.enums.AgentErrorCodeEnum;
import cn.sztu.questioncloud.application.ai.port.LlmPort;
import cn.sztu.questioncloud.application.ai.service.AiGenerateService;
import cn.sztu.questioncloud.application.ai.service.SearchService;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.RAGSearchParam;
import cn.sztu.questioncloud.web.rest.v1.ai.req.GeneratePaperDraftReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.AgentPaperDraftVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.CandidateQuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiGenerateServiceImpl implements AiGenerateService {
    private static final Integer KNOWLEDGE_POINT_TOP_K = 3;
    private static final Double KNOWLEDGE_POINT_MIN_SCORE = 0.65D;
    private static final Integer KNOWLEDGE_POINT_QUESTION_TOP_K = 10;

    private final LlmPort llmPort;
    private final SearchService searchService;

    /**
     * 根据用户描述生成题目草稿
     *
     * @param userInput 用户描述
     * @return 题目草稿
     */
    @Override
    public QuestionDraft generateQuestionDraft(String userInput) {
        return llmPort.generateQuestionDraft(userInput);
    }

    /**
     * 生成Agent组卷草稿
     * @param userId 用户ID
     * @param req    组卷请求
     * @return 试卷草稿视图
     */
    @Override
    public AgentPaperDraftVO generateAgentPaperDraft(Long userId, GeneratePaperDraftReq req) {

        // LLM根据用户输入生成组卷计划
        List<String> allowedTypeCodes = req.getConstrains().stream().map(GeneratePaperDraftReq.BucketConstrain::getTypeCode).toList();
        PaperGenerationPlan plan = llmPort.generatePaperGenerationPlan(req.getMessage(), allowedTypeCodes);

        log.info(plan.toString());

        // req与组卷计划组合为候选题目桶
        Map<String, List<String>> map = plan.getTypeCodeToTopic();
        List<CandidateBucket> buckets = new ArrayList<>();
        for (GeneratePaperDraftReq.BucketConstrain constrain : req.getConstrains()) {
            buckets.add(CandidateBucket.builder()
                    .typeCode(constrain.getTypeCode())
                    .count(constrain.getCount())
                    .topics(map.get(constrain.getTypeCode()))
                    .difficultyMin(constrain.getDifficultyMin())
                    .difficultyMax(constrain.getDifficultyMax())
                    .build());
        }

        // 粗召回：对不同题型桶进行RAG查询
        // CandidateItem包含rag命中次数和最佳相似度排序信息
        Map<String, List<BucketCandidateItem>> bucketCandidates = new HashMap<>();

        for (CandidateBucket bucket : buckets) {
            Map<Long, BucketCandidateItem> questionIdToCandidate = new LinkedHashMap<>();

            RAGSearchParam searchParam = RAGSearchParam.builder()
                    .typeCode(bucket.getTypeCode())
                    .difficultyMin(bucket.getDifficultyMin())
                    .difficultyMax(bucket.getDifficultyMax())
                    .build();

            // 添加不同topic的搜索结果，并在桶内聚合同一题目的命中信息
            for (String topic : Optional.ofNullable(bucket.getTopics()).orElse(List.of())) {
                List<QuestionHitDTO> ragResult = searchQuestionsForTopic(
                        userId,
                        req.getCollectionIds(),
                        topic,
                        searchParam
                );
                if (ragResult.isEmpty()) {
                    continue;
                }
                for (QuestionHitDTO hit : ragResult) {
                    if (hit.getQuestionId() == null) {
                        continue;
                    }
                    BucketCandidateItem item = questionIdToCandidate.get(hit.getQuestionId());
                    if (item == null) { // 首次命中，创建记录
                        BucketCandidateItem newItem = new BucketCandidateItem();
                        newItem.setQuestion(hit);
                        newItem.setHitCount(1);
                        newItem.setBestMatchRank(hit.getSimilarityRank());
                        questionIdToCandidate.put(hit.getQuestionId(), newItem);
                    } else { // 再次命中，更新记录
                        item.setHitCount(item.getHitCount() + 1);
                        int currentRank = hit.getSimilarityRank();
                        item.setBestMatchRank(Math.min(item.getBestMatchRank(), currentRank));
                        if (currentRank < item.getQuestion().getSimilarityRank()) {
                            item.setQuestion(hit);
                        }
                    }
                }
            }

            // 收集结果
            bucketCandidates.put(bucket.getTypeCode(), new ArrayList<>(questionIdToCandidate.values()));
        }

        // 检查结果总数
        Map<String, Integer> typeCodeToCount = req.getConstrains().stream()
                .collect(Collectors.toMap(GeneratePaperDraftReq.BucketConstrain::getTypeCode, GeneratePaperDraftReq.BucketConstrain::getCount));
        for (String typeCode : bucketCandidates.keySet()) {
            // 当前桶结果总数
            int questionCount = bucketCandidates.get(typeCode).size();
            if (questionCount < typeCodeToCount.get(typeCode)) {
                throw new ApplicationException(AgentErrorCodeEnum.AGENT_SEARCH_RESULT_NOT_ENOUGH);
            } else {
                log.info("当前题型桶 {} 搜索到 {} 个结果", typeCode, questionCount);
            }
        }

        // 桶内重排，按照计算出的分数降序排序
        List<CandidateQuestionVO> candidateQuestions = new ArrayList<>();

        for (Map.Entry<String, List<BucketCandidateItem>> entry : bucketCandidates.entrySet()) {
            String typeCode = entry.getKey();
            List<BucketCandidateItem> items = entry.getValue();

            items.sort((a, b) -> {
                double scoreA = calculateFinalScore(
                        a.getHitCount(),
                        a.getBestMatchRank(),
                        a.getQuestion().getExposureFactor());
                double scoreB = calculateFinalScore(
                        b.getHitCount(),
                        b.getBestMatchRank(),
                        b.getQuestion().getExposureFactor());
                // 降序排序
                return Double.compare(scoreB, scoreA);
            });

            // 取所需题数并收集结果
            int needCount = typeCodeToCount.get(typeCode);
            candidateQuestions.addAll(items.subList(0, needCount).stream()
                    .map(item -> CandidateQuestionVO.builder()
                            .questionId(item.getQuestion().getQuestionId())
                            .questionVersionId(item.getQuestion().getQuestionVersionId())
                            .title(item.getQuestion().getTitle())
                            .typeCode(item.getQuestion().getTypeCode())
                            .difficulty(item.getQuestion().getDifficulty())
                            .build())
                    .toList());
        }

        return AgentPaperDraftVO.builder()
                .reason(plan.getReason())
                .candidateQuestions(candidateQuestions)
                .build();
    }

    /**
     * 计算候选题得分
     * @param hitCount       rag检索命中次数
     * @param bestMatchRank  最佳命中排名
     * @param exposureFactor 曝光系数
     * @return 得分
     */
    private double calculateFinalScore(int hitCount, int bestMatchRank, double exposureFactor) {
        // hitCount 权重大
        // bestMatchRank 次之
        // exposureFactor 最后
        return 0.5 * hitCount + 0.3 * (1.0 / bestMatchRank) + 0.2 * (1 - exposureFactor);
    }

    private List<QuestionHitDTO> searchQuestionsForTopic(Long userId,
                                                         List<Long> collectionIds,
                                                         String topic,
                                                         RAGSearchParam searchParam) {
        if (topic == null || topic.isBlank()) {
            return List.of();
        }

        List<KnowledgePointHitDTO> knowledgePointHits = searchService.searchKnowledgePoints(
                null,
                topic,
                KNOWLEDGE_POINT_TOP_K,
                KNOWLEDGE_POINT_MIN_SCORE
        );

        List<Long> knowledgePointIds = knowledgePointHits.stream()
                .map(KnowledgePointHitDTO::getKnowledgePointId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (!knowledgePointIds.isEmpty()) {
            List<QuestionHitDTO> matchedQuestions = searchService.searchQuestionsByKnowledgePoints(
                    knowledgePointIds,
                    collectionIds,
                    KNOWLEDGE_POINT_QUESTION_TOP_K,
                    searchParam.getTypeCode(),
                    searchParam.getDifficultyMin(),
                    searchParam.getDifficultyMax()
            );
            if (!matchedQuestions.isEmpty()) {
                return matchedQuestions;
            }
        }

        return searchService.searchQuestions(userId, collectionIds, topic, searchParam);
    }
}
