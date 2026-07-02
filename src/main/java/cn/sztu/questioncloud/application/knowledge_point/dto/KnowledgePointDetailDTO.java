package cn.sztu.questioncloud.application.knowledge_point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointDetailDTO {
    private String description;
    private String formulaOrCode;
    private String example;
}
