package cn.sztu.questioncloud.web.rest.v1.importer.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportCreateVO {
    private Long importId;
    private String parseJobId;
}
