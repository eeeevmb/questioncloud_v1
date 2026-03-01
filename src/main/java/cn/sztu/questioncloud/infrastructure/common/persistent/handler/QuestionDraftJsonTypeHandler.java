package cn.sztu.questioncloud.infrastructure.common.persistent.handler;

import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

@MappedJdbcTypes(org.apache.ibatis.type.JdbcType.VARCHAR)
@MappedTypes(QuestionDraft.class)
public class QuestionDraftJsonTypeHandler extends JacksonTypeHandler<QuestionDraft> {
    public QuestionDraftJsonTypeHandler() {
        super(QuestionDraft.class);
    }
}
