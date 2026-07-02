package cn.sztu.questioncloud.application.knowledge_point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextbookCheckDTO {

    private Long knowledgeScopeId;

    private String textbookName;

    private String author;
}
