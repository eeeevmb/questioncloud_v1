package cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeImportPreviewVO {

    private String textbookName;

    private String knowledgeScope;

    private Long knowledgeScopeId;

    private Boolean textbookExists;

    private Long existingTextbookId;

    private List<KnowledgePointVO> items;
}
