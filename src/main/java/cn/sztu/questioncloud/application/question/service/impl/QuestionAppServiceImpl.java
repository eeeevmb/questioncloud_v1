package cn.sztu.questioncloud.application.question.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionStatusEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.application.question.port.*;
import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.CollectionItem;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
public class QuestionAppServiceImpl implements QuestionAppService {
    private final QuestionRepository questionRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionCollectionRepository questionCollectionRepository;
    private final QuestionQueryRepository queryRepository;
    private final CollectionItemRepository collectionItemRepository;
    public static final int INITIAL_VERSION = 1;
    public static final int INITIAL_ORDINAL = 1;

    public QuestionAppServiceImpl(QuestionRepository questionRepository, QuestionVersionRepository questionVersionRepository, QuestionCollectionRepository questionCollectionRepository, QuestionQueryRepository queryRepository, CollectionItemRepository collectionItemRepository) {
        this.questionRepository = questionRepository;
        this.questionVersionRepository = questionVersionRepository;
        this.questionCollectionRepository = questionCollectionRepository;
        this.queryRepository = queryRepository;
        this.collectionItemRepository = collectionItemRepository;
    }

    /**
     * 创建题目（Latex文本）
     *
     * @param req 创建题目请求
     * @return 题目视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionVO createQuestion(CreateQuestionReq req) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 权限检验
        QuestionCollectionEntity collectionEntity = questionCollectionRepository.findById(req.collectionId())
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在，debug:数据库中没有"));

        if (!collectionEntity.getOwnerId().equals(userId)) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在，debug：题集主不匹配");
        }

        // 3. 校验题型
        if(!QuestionTypeEnum.ensureValid(req.typeCode())){
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_TYPE_ERROR, "题型不能为空");
        }

        // 4. 创建题目实体和题目版本实体
        QuestionEntity entity = QuestionEntity.builder()
                .id(HutoolSnowflakeIdGenerator.generateLongId())
                .status(QuestionStatusEnum.ACTIVE.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        QuestionVersionEntity versionEntity = QuestionVersionEntity.builder()
                .questionId(entity.getId())
                .versionNo(INITIAL_VERSION)
                .typeCode(req.typeCode())
                .title(req.title())
                .stem(req.stem())
                .answer(req.answer())
                .solution(req.solution())
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        // 5. 获取落库的题目版本ID后回填题目实体
        Long versionId = questionVersionRepository.save(versionEntity)
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED, "新建题目失败"));

        entity.setCurrentVersionId(versionId);
        questionRepository.save(entity);

        // 6. 将题目添加进题集
        // TODO：统计题目数有并发问题，但是现在还没有协作修改题集接口，暂时忽略
        int currentCount = queryRepository.CountCollectionItemsById(req.collectionId());

        CollectionItem item = CollectionItem.builder()
                .collectionId(req.collectionId())
                .ordinal(currentCount == 0 ? INITIAL_ORDINAL : currentCount + 1)
                .questionId(entity.getId())
                .questionVersionId(versionEntity.getId())
                .build();
        collectionItemRepository.save(item);

        // 7. 返回题目视图
        return QuestionVO.fromEntity(entity, versionEntity);
    }


}
