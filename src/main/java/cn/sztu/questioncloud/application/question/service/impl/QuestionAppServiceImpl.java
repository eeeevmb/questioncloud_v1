package cn.sztu.questioncloud.application.question.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionStatusEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.application.question.port.*;
import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.*;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.QuestionInCollectionPageQuery;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionCreatedVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionSummaryVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuestionAppServiceImpl implements QuestionAppService {
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionCollectionRepository questionCollectionRepository;
    private final QuestionQueryRepository queryRepository;
    private final QuestionStatRepository questionStatRepository;
    private final CollectionItemRepository collectionItemRepository;
    public static final Integer INITIAL_VERSION = 1;
    public static final Integer INITIAL_COUNT = 0;
    public static final Double INITIAL_EXP = 1.00;


    public QuestionAppServiceImpl(QuestionRepository questionRepository, QuestionVersionRepository questionVersionRepository, QuestionCollectionRepository questionCollectionRepository, QuestionQueryRepository queryRepository, CollectionItemRepository collectionItemRepository, QuestionStatRepository questionStatRepository) {
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.questionCollectionRepository = questionCollectionRepository;
        this.queryRepository = queryRepository;
        this.collectionItemRepository = collectionItemRepository;
        this.questionStatRepository = questionStatRepository;
    }

    /**
     * 创建题目
     *
     * @param req 创建题目请求
     * @return 题目ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionCreatedVO createQuestion(CreateQuestionReq req) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 权限检验
        QuestionCollectionEntity collectionEntity = questionCollectionRepository.findById(req.getCollectionId())
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在"));

        if (!userId.equals(collectionEntity.getOwnerId())) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在");
        }

        // 3. 校验题型
        if (!QuestionTypeEnum.ensureValid(req.getTypeCode())){
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_TYPE_ERROR, "题型非法或为空");
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
                .answerKey(getAnswerKey(req))
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
                .updatedAt(now)
                .build();

        questionStatRepository.save(stat);

        // 7. 返回题目创建视图
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
     * @return 题目详情视图
     */
    @Override
    public QuestionDetailVO getQuestionDetailById(Long questionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Optional<QuestionDetailVO> detailVO = queryRepository.getQuestionDetailById(questionId);

        // 1. 校验结果以及权限验证
        QuestionDetailVO result = detailVO
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND, "题目不存在"));
        if (!userId.equals(result.getOwnerId())) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION, "无查看权限");
        }

        // 2. 获取题目详情
        return result;
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
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "数据库中没有对应题集"));

        if (!userId.equals(collection.getOwnerId())) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "当前登录用户非题集所有者");
        }

        // 2. 获取题目概要
        Pager<QuestionSummaryVO> paging = queryRepository.findPageByCollectionId(collectionId, query);

        return PageResult.of(paging.getResults(), paging.getTotal(), query);
    }

    // 结构化答案转化为answerKey
    @NotNull
    private static String getAnswerKey(CreateQuestionReq req) {
        String typeCode = req.getTypeCode();

        // 单选
        if (typeCode.equals(QuestionTypeEnum.SINGLE_CHOICE.getCode())) {
            List<String> opts = req.getCorrectOptions();
            if (opts == null || opts.size() != 1) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED, "单选题只能有一个正确选项");
            }
            return normalizeOption(opts.getFirst());

        }

        // 多选
        if (typeCode.equals(QuestionTypeEnum.MULTIPLE_CHOICE.getCode())) {
            List<String> opts = req.getCorrectOptions();
            if (opts == null || opts.isEmpty()) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED, "多选题至少要有一个正确选项");
            }
            return opts.stream()
                    .filter(Objects::nonNull)
                    .map(QuestionAppServiceImpl::normalizeOption)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining());
        }

        // 填空
        if (typeCode.equals(QuestionTypeEnum.FILL_IN_BLANK.getCode())) {
            String ans = req.getAnswer();
            return ans == null ? "" : ans;
        }

        // 判断
        if (typeCode.equals(QuestionTypeEnum.TRUE_FALSE.getCode())) {
            String ans = req.getJudgeAnswer();
            if (ans == null) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED, "判断题答案不能为空");
            }
            ans = ans.trim().toUpperCase();
            if (!ans.equals("T") && !ans.equals("F")) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED, "判断题答案必须为 T 或者 F");
            }

            return ans;
        }

        return "";
    }

    private static String normalizeOption(String option) {
        if (option == null) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED ,"选项值不能为空");
        }
        return option.trim().toUpperCase();
    }
}
