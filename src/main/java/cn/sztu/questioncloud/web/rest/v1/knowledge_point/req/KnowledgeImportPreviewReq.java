package cn.sztu.questioncloud.web.rest.v1.knowledge_point.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeImportPreviewReq {

    @NotBlank(message = "知识点领域不能为空")
    @JsonAlias("subject")
    private String knowledgeScope;

    @NotBlank(message = "教材名称不能为空")
    private String textbookName;

    private String edition;

    private String author;

    private String publisher;

    private String isbn;

    @Valid
    @NotEmpty(message = "目录项不能为空")
    private List<DirectoryItem> items;

    @Data
    public static class DirectoryItem {
        private Integer level;

        @NotBlank(message = "目录标题不能为空")
        private String title;

        @NotBlank(message = "目录路径不能为空")
        private String directoryPath;
    }
}
