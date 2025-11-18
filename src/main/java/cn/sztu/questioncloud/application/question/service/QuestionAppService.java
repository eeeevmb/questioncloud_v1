package cn.sztu.questioncloud.application.question.service;

import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionVO;

/**
 * 题目相关服务
 *
 * @author eeeevmb
 */
public interface QuestionAppService {
    /**
     * 创建题目（Latex文本）
     *
     * @param req 创建题目请求
     * @return 题目视图对象
     */
    QuestionVO createQuestion(CreateQuestionReq req);
}
