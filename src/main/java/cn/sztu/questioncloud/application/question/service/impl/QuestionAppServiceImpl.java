package cn.sztu.questioncloud.application.question.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionStatusEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.application.question.messaging.QuestionEventMessage;
import cn.sztu.questioncloud.application.question.messaging.QuestionEventPublisher;
import cn.sztu.questioncloud.application.question.port.*;
import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.common.util.ExposureFactorUtil;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.*;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.QuestionInCollectionPageQuery;
import cn.sztu.questioncloud.web.rest.v1.question.req.UpdateQuestionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionCreatedVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionSummaryVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAppServiceImpl implements QuestionAppService {
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionCollectionRepository questionCollectionRepository;
    private final QuestionQueryRepository queryRepository;
    private final QuestionStatRepository questionStatRepository;
    private final CollectionItemRepository collectionItemRepository;
    private final QuestionEventPublisher questionEventPublisher;
    public static final Integer INITIAL_VERSION = 1;
    public static final Integer INITIAL_COUNT = 0;
    public static final Double INITIAL_EXP = 1.00;

    /**
     * 创建题目
     *
     * @param req 创建题目请求
     * @return 题目ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionCreatedVO createQuestion(CreateQuestionReq req, Long userId) {
//        // 1. 获取用户ID
//        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 权限检验
        QuestionCollectionEntity collectionEntity = questionCollectionRepository.findById(req.getCollectionId())
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND));

        if (!userId.equals(collectionEntity.getOwnerId())) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND);
        }

        // 3. 校验题型
        if (!QuestionTypeEnum.ensureValid(req.getTypeCode())){
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_TYPE_ERROR);
        }

        // 4. 创建题目实体和题目版本实体
        Long questionId = HutoolSnowflakeIdGenerator.generateLongId();
        Long questionVersionId = HutoolSnowflakeIdGenerator.generateLongId();
        LocalDateTime now = LocalDateTime.now();

        QuestionEntity questionEntity = QuestionEntity.builder()
                .id(questionId)
                .status(QuestionStatusEnum.ACTIVE.getCode())
                .currentVersionId(questionVersionId)
                .ownerId(userId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        QuestionVersionEntity versionEntity = QuestionVersionEntity.builder()
                .id(questionVersionId)
                .typeCode(req.getTypeCode())
                .questionId(questionId)
                .versionNo(INITIAL_VERSION)
                .title(req.getTitle())
                .stem(req.getStem())
                .options(req.getOptions())
                .answer(req.getAnswer())
                .answerKey(getAnswerKey(req.getTypeCode(), req.getCorrectOptions(), req.getJudgeAnswer()))
                .solution(req.getSolution())
                .assets(req.getAssets())
                .createdBy(userId)
                .createdAt(now)
                .build();

        questionRepository.save(questionEntity);
        questionVersionRepository.save(versionEntity);

        // 5. 将题目添加进题集
        Integer ordinal = queryRepository.getMaxOrdinal(req.getCollectionId()) + 1;

        CollectionItem collectionItem = CollectionItem.builder()
                .collectionId(req.getCollectionId())
                .ordinal(ordinal)
                .questionId(questionId)
                .questionVersionId(questionVersionId)
                .build();
        collectionItemRepository.save(collectionItem);

        // 6. 添加题目初始元数据
        QuestionStat stat = QuestionStat.builder()
                .questionId(questionId)
                .versionId(questionVersionId)
                .attempts(INITIAL_COUNT)
                .correctCount(INITIAL_COUNT)
                .correctRate(null)
                .difficulty(req.getDifficulty() == null ? null : req.getDifficulty().doubleValue())
                .exposureFactor(INITIAL_EXP)
                .lastExposedAt(now)
                .updatedAt(now)
                .build();

        questionStatRepository.save(stat);

        // 7. 异步向量化入库
        questionEventPublisher.publishCreated(QuestionEventMessage.builder()
                        .questionId(questionId)
                        .versionId(questionVersionId)
                        .collectionId(req.getCollectionId())
                        .ownerId(userId)
                        .occurredAt(now).build());

        // 8. 返回题目创建视图
        return QuestionCreatedVO.builder()
                .questionId(questionId)
                .questionVersionId(questionVersionId)
                .collectionId(req.getCollectionId())
                .build();
    }

    /**
     * 根据题目ID查询题目详情
     *
     * @param questionId 题目ID
     * @param userId     用户ID
     * @return 题目详情视图
     */
    @Override
    public QuestionDetailVO getQuestionDetailById(Long questionId, Long userId) {
        Optional<QuestionDetailVO> detailVO = queryRepository.getQuestionDetailById(questionId);

        // 1. 校验结果以及权限验证
        QuestionDetailVO result = detailVO
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND));
        if (!userId.equals(result.getOwnerId())) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }

        // 2. 计算有效曝光系数
        // 注：库中的曝光系数只在曝光事件（如组卷）时更新，查询时返回根据衰减公式计算出的当日有效曝光系数
        LocalDateTime now = LocalDateTime.now();

        double effExp = ExposureFactorUtil.calcEffectiveExposure(result.getExposureFactor(), result.getLastExposedAt(), now);
        result.setExposureFactor(effExp);

        // 3. 返回题目详情
        return result;
    }

    /**
     * 在题目ID下创建新题目版本
     *
     * @param req        修改请求
     * @param questionId 题目ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuestionById(UpdateQuestionReq req, Long questionId) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 获取题目实体，题目版本，统计信息和题集内容实体
        QuestionEntity questionEntity = questionRepository.getById(questionId);
        QuestionVersionEntity versionEntity = questionVersionRepository.getCurrentVersionByQuestionId(questionId);
        QuestionStat questionStat = questionStatRepository.getByQuestionId(questionId);

        // 3. 权限校验和异常处理
        if (questionEntity == null || versionEntity == null || questionStat == null) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND);
        }

        if (!userId.equals(questionEntity.getOwnerId())) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }

        // 4. 创建新题目版本
        LocalDateTime now = LocalDateTime.now();
        Long newVersionId = HutoolSnowflakeIdGenerator.generateLongId();

        QuestionVersionEntity newVersionEntity = QuestionVersionEntity.builder()
                .id(newVersionId)
                .questionId(questionId)
                .versionNo(versionEntity.getVersionNo() + 1)
                .typeCode(versionEntity.getTypeCode())
                .title(req.getTitle())
                .stem(req.getStem())
                .options(req.getOptions())
                .answer(req.getAnswer())
                .answerKey(getAnswerKey(versionEntity.getTypeCode(), req.getCorrectOptions(), req.getJudgeAnswer()))
                .solution(req.getSolution())
                .assets(req.getAssets())
                .createdBy(userId)
                .createdAt(now)
                .build();

        // 5. 修改题目实体
        questionEntity.setCurrentVersionId(newVersionId);
        questionEntity.setUpdatedAt(now);

        // 6. 修改统计数据
        questionStat.setVersionId(newVersionId);
        questionStat.setUpdatedAt(now);

        // 7. 修改题集内容
        collectionItemRepository.syncVersionToAllCollections(questionId, newVersionId);

        // 8. 统一落库
        questionVersionRepository.save(newVersionEntity);
        questionRepository.update(questionEntity);
        questionStatRepository.update(questionStat);

        // 9. 发布题目领域事件
        Long collectionId = collectionItemRepository.findByQuestionIdAndVersionId(questionId, newVersionId).getCollectionId();
        questionEventPublisher.publishUpdated(QuestionEventMessage.builder()
                        .questionId(questionId)
                        .versionId(newVersionId)
                        .collectionId(collectionId)
                        .ownerId(userId)
                        .occurredAt(now)
                        .build());
    }

    /**
     * 硬删除 question、question_version、question_stats，
     * 并从所有题集中移除该题，不重排 collection_item.ordinal。
     *
     * @param questionId 题目ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestionById(Long questionId) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 权限、存在性校验
        QuestionEntity questionEntity = questionRepository.getById(questionId);
        if (questionEntity == null) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND);
        }

        if (!userId.equals(questionEntity.getOwnerId())) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }

        // 3. 发布题库领域事件
        questionEventPublisher.publishDeleted(QuestionEventMessage.builder()
                        .questionId(questionId)
                        .occurredAt(LocalDateTime.now())
                        .build());

        // 4. 删除实体
        collectionItemRepository.deleteByQuestionId(questionId);
        questionStatRepository.deleteByQuestionId(questionId);
        questionVersionRepository.deleteByQuestionId(questionId);
        questionRepository.delete(questionId);
    }


    /**
     * 查询题集内题目概要
     *
     * @param collectionId 题集ID
     * @return 题目概要列表
     */
    @Override
    public PageResult<QuestionSummaryVO> getQuestionSummariesByCollectionId(Long collectionId,
                                                                            QuestionInCollectionPageQuery query) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 权限验证
        QuestionCollectionEntity collection = questionCollectionRepository.findById(collectionId)
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND));

        if (!userId.equals(collection.getOwnerId())) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }

        // 2. 获取题目概要
        Pager<QuestionSummaryVO> paging = queryRepository.findPageByCollectionId(collectionId, query);

        return PageResult.of(paging.getResults(), paging.getTotal(), query);
    }

    private static String getAnswerKey(String typeCode,
                                       List<String> correctOptions,
                                       String judgeAnswer) {
        // 单选
        if (QuestionTypeEnum.SINGLE_CHOICE.getCode().equals(typeCode)) {
            if (correctOptions == null || correctOptions.size() != 1) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED,
                        "单选题只能有一个正确选项");
            }
            return normalizeOption(correctOptions.getFirst());
        }

        // 多选
        if (QuestionTypeEnum.MULTIPLE_CHOICE.getCode().equals(typeCode)) {
            if (correctOptions == null || correctOptions.isEmpty()) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED,
                        "多选题至少要有一个正确选项");
            }
            return correctOptions.stream()
                    .filter(Objects::nonNull)
                    .map(QuestionAppServiceImpl::normalizeOption)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining());
        }

//        // 填空
//        if (QuestionTypeEnum.FILL_IN_BLANK.getCode().equals(typeCode)) {
//            return (answer == null) ? "" : answer;
//        }

        // 判断
        if (QuestionTypeEnum.TRUE_FALSE.getCode().equals(typeCode)) {
            if (judgeAnswer == null) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED,
                        "判断题答案不能为空");
            }
            String ans = judgeAnswer.trim().toUpperCase();
            if (!"T".equals(ans) && !"F".equals(ans)) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED,
                        "判断题答案必须为 T 或者 F");
            }
            return ans;
        }

        // 其它题型目前不参与机器判分
        return "";
    }

    private static String normalizeOption(String option) {
        if (option == null) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED ,"选项值不能为空");
        }
        return option.trim().toUpperCase();
    }
}
