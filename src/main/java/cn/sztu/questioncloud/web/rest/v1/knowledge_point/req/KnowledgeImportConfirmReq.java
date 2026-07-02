package cn.sztu.questioncloud.web.rest.v1.knowledge_point.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeImportConfirmReq {

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
    @NotEmpty(message = "确认导入的知识点不能为空")
    private List<Item> items;

    @Data
    public static class Item {
        @NotBlank(message = "知识点名称不能为空")
        private String canonicalName;
    }
}
