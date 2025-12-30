package cn.sztu.questioncloud.web.rest.v1.paper.vo;

import db.sql.api.impl.cmd.condition.In;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperItemSaveVO {
    private Long questionId;
    private Long questionVersionId;
}
