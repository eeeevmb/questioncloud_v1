package cn.sztu.questioncloud.application.question.service.impl;

import cn.sztu.questioncloud.application.common.enums.VectorDocTypeEnum;
import cn.sztu.questioncloud.application.question.messaging.QuestionEventMessage;
import cn.sztu.questioncloud.application.question.port.QuestionStatRepository;
import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.application.common.port.VectorPort;
import cn.sztu.questioncloud.application.question.service.QuestionVectorizeService;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class QuestionVectorizeServiceImpl implements QuestionVectorizeService {
    private final VectorPort vectorPort;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionStatRepository questionStatRepository;

    public QuestionVectorizeServiceImpl(VectorPort vectorPort, QuestionVersionRepository questionVersionRepository, QuestionStatRepository questionStatRepository) {
        this.vectorPort = vectorPort;
        this.questionVersionRepository = questionVersionRepository;
        this.questionStatRepository = questionStatRepository;
    }

    /**
     * 棰樼洰鍒涘缓鏃讹紝灏嗙浉瀵瑰簲鐨勯鐩悜閲忓寲鍏ュ簱
     *
     * @param message 娑堟伅
     */
    @Override
    public void onQuestionUpsert(QuestionEventMessage message) {
        QuestionVersionEntity versionEntity = questionVersionRepository.getVersionById(message.getVersionId());
        QuestionStat questionStat = questionStatRepository.getByQuestionId(message.getQuestionId());

        String vectorId = "Q_" + message.getQuestionId();

        String text = "棰樺共锛歕n" + versionEntity.getStem() +
                "\n\n瑙ｆ瀽锛歕n" + versionEntity.getSolution();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("docType", VectorDocTypeEnum.QUESTION.getCode());
        metadata.put("questionId", message.getQuestionId());
        metadata.put("versionId", message.getVersionId());
        metadata.put("collectionId", message.getCollectionId());
        metadata.put("typeCode", versionEntity.getTypeCode());
        metadata.put("difficulty", questionStat.getDifficulty() == null ? 0.00 : questionStat.getDifficulty());
        metadata.put("ownerId", message.getOwnerId());

        vectorPort.upsert(vectorId, text, metadata);
    }

    /**
     * 棰樼洰鍒犻櫎鏃讹紝鍒犻櫎鍚戦噺搴撲腑鐨勮褰?
     *
     * @param message 娑堟伅
     */
    @Override
    public void onQuestionDeleted(QuestionEventMessage message) {
        String vectorId = "Q_" + message.getQuestionId();
        vectorPort.delete(vectorId);
    }


}
