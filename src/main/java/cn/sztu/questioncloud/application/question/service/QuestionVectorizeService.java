package cn.sztu.questioncloud.application.question.service;

import cn.sztu.questioncloud.application.question.messaging.QuestionEventMessage;

/**
 * 向量化题目相关服务
 */
public interface QuestionVectorizeService {
    /**
     * 题目创建时，将题目向量化入库，更新时，则更新向量和元数据
     *
     * @param message 消息
     */
    void onQuestionUpsert(QuestionEventMessage message);

    /**
     * 题目删除时，删除向量库中的记录
     *
     * @param message 消息
     */
    void onQuestionDeleted(QuestionEventMessage message);
}
