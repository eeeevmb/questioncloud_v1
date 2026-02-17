package cn.sztu.questioncloud.web.rest.v1.paper.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaperItemVO {
    private Long questionId;
    private Long questionVersionId;
}
