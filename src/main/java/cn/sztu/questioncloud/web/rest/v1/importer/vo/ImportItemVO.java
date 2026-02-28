package cn.sztu.questioncloud.web.rest.v1.importer.vo;

import cn.sztu.questioncloud.application.importer.dto.ImportErrorReport;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ImportItemVO {
    private Long itemId;
    private Integer indexNo;
    private String collectionName;
    private QuestionDraft draft;
    private Integer status;
    private List<ImportErrorReport> errors;
    private LocalDateTime updatedAt;
}
