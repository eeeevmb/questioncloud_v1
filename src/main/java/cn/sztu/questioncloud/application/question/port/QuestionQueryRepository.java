package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.web.rest.v1.question.req.QuestionInCollectionPageQuery;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionSummaryVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;

import java.util.List;
import java.util.Optional;

/**
 * 题目模块查询仓储接口
 */
public interface QuestionQueryRepository {

    /**
     * 查询题集内题目总数
     *
     * @param collectionId 题集ID
     * @return 题集内题目总数
     */
    Integer CountCollectionItemsById(Long collectionId);

    /**
     * 查询题集内最大顺序值
     *
     * @param collectionId 题集ID
     * @return 顺序值
     */
    Integer getMaxOrdinal(Long collectionId);

    /**
     * 根据题目ID查询题目详情
     * @param questionId 题目ID
     * @return 查询结果
     */
    Optional<QuestionDetailVO> getQuestionDetailById(Long questionId);

    /**
     * 根据题集ID分页查询题集内题目概要
     *
     * @param collectionId 题集ID
     * @param query        分页查询参数
     * @return 分页结果
    */
    Pager<QuestionSummaryVO> findPageByCollectionId(Long collectionId,
                                                    QuestionInCollectionPageQuery query);

    /**
     *
     * 根据题集IDs和题目类型查询题目ID列表
     * @param collectionIds 题集IDs
     * @param typeCode 题目类型(调用方法前先确保typeCode合法)
     * @return 题目ID列表
     */
    List<QuestionDetailVO> findIdsByCollectionsAndType(List<Long> collectionIds, String typeCode);
}
