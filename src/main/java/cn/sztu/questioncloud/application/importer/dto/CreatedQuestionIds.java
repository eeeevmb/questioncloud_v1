package cn.sztu.questioncloud.application.importer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreatedQuestionIds {
    private Long questionId;
    private Long versionId;
    private Long collectionId;
}