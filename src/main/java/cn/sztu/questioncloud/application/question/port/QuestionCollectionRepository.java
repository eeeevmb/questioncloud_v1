package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;

public interface QuestionCollectionRepository {



    // ===== 写入操作 =====
    /**
     * 保存题集
     * @param questionCollection 保存的题集对象
     */
    void save(QuestionCollectionEntity questionCollection);
}
