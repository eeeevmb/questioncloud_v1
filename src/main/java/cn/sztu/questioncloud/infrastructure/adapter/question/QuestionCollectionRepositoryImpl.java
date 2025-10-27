package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.QuestionCollectionRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionCollectionMapper;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionCollectionRepositoryImpl implements QuestionCollectionRepository {
    private final QuestionCollectionMapper questionCollectionMapper;

    public QuestionCollectionRepositoryImpl(QuestionCollectionMapper questionCollectionMapper) {
        this.questionCollectionMapper = questionCollectionMapper;
    }

    /**
     * 保存题集
     *
     * @param questionCollection 保存的题集对象
     */
    @Override
    public void save(QuestionCollectionEntity questionCollection) {
        if (questionCollection.getId() == null) {
            questionCollection.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        questionCollectionMapper.save(questionCollection);
    }
}
