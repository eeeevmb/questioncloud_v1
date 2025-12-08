package cn.sztu.questioncloud.application.question.service;

import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.QuestionInCollectionPageQuery;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionCreatedVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionSummaryVO;

/**
 * 题目相关服务
 *
 * @author eeeevmb
 */
public interface QuestionAppService {
    /**
     * 创建题目
     *
     * @param req 创建题目请求
     * @return 题目ID
     */
    QuestionCreatedVO createQuestion(CreateQuestionReq req);

    /**
     * 根据题目ID查询题目详情
     *
     * @param questionId 题目ID
     * @return 题目详情视图
     */
    QuestionDetailVO getQuestionDetailById(Long questionId);

    /**
     * 查询题集内题目概要
     *
     * @param collectionId 题集ID
     * @param query        分页和查询参数
     * @return 分页查询结果
     */
    PageResult<QuestionSummaryVO> getQuestionSummariesByCollectionId(Long collectionId, QuestionInCollectionPageQuery query);


}
