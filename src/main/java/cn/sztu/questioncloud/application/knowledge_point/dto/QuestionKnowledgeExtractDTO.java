package cn.sztu.questioncloud.application.knowledge_point.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionKnowledgeExtractDTO {
    /**
     * 知识点领域名称
     */
    @JsonAlias("subject")
    private String knowledgeScope;

    /**
     * 标准知识点名称，当前仍以中文为主
     */
    private String canonicalName;

    /**
     * 别名列表，当前主要用于接收英文知识点名
     */
    private List<String> aliases;

    /**
     * 0=次要考点，1=主要考点
     */
    private Integer isMain;

    /**
     * 与当前题目的相关度，0-100
     */
    private Integer relevanceScore;
}
