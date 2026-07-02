package cn.sztu.questioncloud.infrastructure.common.ai.spec;

import cn.sztu.questioncloud.infrastructure.common.ai.constant.KnowledgeToolDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.KnowledgeToolParamDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.QuestionToolDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.QuestionToolParamDocs;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 负责定义组卷 Agent 可使用的手写工具规范。
 */
@Component
public class PaperToolSpecificationFactory {

    private static final List<String> TYPE_CODES = List.of(
            "single-choice",
            "multiple-choice",
            "true-false",
            "fill-in",
            "short-answer"
    );

    private static final List<String> OPTION_KEYS = List.of(
            "A", "B", "C", "D", "E", "F", "G", "H"
    );

    /**
     * 返回手写工具规格映射，key 为 toolName。
     */
    public Map<String, ToolSpecification> manualSpecifications() {
        return Map.of(
                "createQuestion", createQuestionSpecification(),
                "searchQuestion", searchQuestionSpecification(),
                "getQuestionDetail", getQuestionDetailSpecification(),
                "searchKnowledgePoints", searchKnowledgePointsSpecification(),
                "searchQuestionsByKnowledgePoints", searchQuestionsByKnowledgePointsSpecification()
        );
    }

    /**
     * createQuestion 的 ToolSpecification。
     */
    public ToolSpecification createQuestionSpecification() {
        JsonObjectSchema optionItemSchema = JsonObjectSchema.builder()
                .description("选择题选项对象")
                .addProperty("key", JsonEnumSchema.builder()
                        .description("选项键，必须为大写字母")
                        .enumValues(OPTION_KEYS)
                        .build())
                .addProperty("content", JsonStringSchema.builder()
                        .description("选项内容")
                        .build())
                .required("key", "content")
                .additionalProperties(false)
                .build();

        JsonObjectSchema argsSchema = JsonObjectSchema.builder()
                .description("创建题目参数，当前会话已绑定题集")
                .addProperty("typeCode", JsonEnumSchema.builder()
                        .description("题型代码")
                        .enumValues(TYPE_CODES)
                        .build())
                .addProperty("title", JsonStringSchema.builder()
                        .description("题目标题，建议 12 字以内")
                        .build())
                .addProperty("stem", JsonStringSchema.builder()
                        .description("题干，支持 LaTeX 文本")
                        .build())
                .addProperty("options", JsonArraySchema.builder()
                        .description("选择题选项，仅单选题和多选题使用")
                        .items(optionItemSchema)
                        .build())
                .addProperty("correctOptions", JsonArraySchema.builder()
                        .description("正确选项，单选长度为 1，多选长度大于等于 1")
                        .items(JsonEnumSchema.builder()
                                .enumValues(OPTION_KEYS)
                                .build())
                        .build())
                .addProperty("judgeAnswer", JsonEnumSchema.builder()
                        .description("判断题答案，仅 true-false 使用")
                        .enumValues("T", "F")
                        .build())
                .addProperty("answer", JsonStringSchema.builder()
                        .description("展示用答案文本，fill-in / short-answer 建议提供")
                        .build())
                .addProperty("solution", JsonStringSchema.builder()
                        .description("题目解析，可省略")
                        .build())
                .addProperty("difficulty", JsonNumberSchema.builder()
                        .description("难度，范围 0.00 ~ 1.00，除非用户指定否则可省略")
                        .build())
                .required("typeCode", "stem")
                .additionalProperties(false)
                .build();

        return ToolSpecification.builder()
                .name("createQuestion")
                .description(QuestionToolDocs.CREATE_QUESTION)
                .parameters(argsSchema)
                .build();
    }

    /**
     * searchQuestion 的 ToolSpecification。
     */
    public ToolSpecification searchQuestionSpecification() {
        JsonObjectSchema ragSearchParamSchema = JsonObjectSchema.builder()
                .description("题目检索筛选参数，可省略或传空对象")
                .addProperty("difficultyMin", JsonNumberSchema.builder()
                        .description("难度下限，范围 0.00 ~ 1.00")
                        .build())
                .addProperty("difficultyMax", JsonNumberSchema.builder()
                        .description("难度上限，范围 0.00 ~ 1.00")
                        .build())
                .addProperty("typeCode", JsonEnumSchema.builder()
                        .description("题型代码筛选")
                        .enumValues(TYPE_CODES)
                        .build())
                .additionalProperties(false)
                .build();

        JsonObjectSchema argsSchema = JsonObjectSchema.builder()
                .description(QuestionToolParamDocs.RAG_SEARCH_ARGS_PARAM)
                .addProperty("query", JsonStringSchema.builder()
                        .description("用户自然语言检索描述，不能为空")
                        .build())
                .addProperty("ragSearchParam", ragSearchParamSchema)
                .required("query")
                .additionalProperties(false)
                .build();

        return ToolSpecification.builder()
                .name("searchQuestion")
                .description(QuestionToolDocs.RAG_SEARCH)
                .parameters(argsSchema)
                .build();
    }

    /**
     * getQuestionDetail 的 ToolSpecification。
     */
    public ToolSpecification getQuestionDetailSpecification() {
        JsonObjectSchema argsSchema = JsonObjectSchema.builder()
                .description("该工具无需显式业务参数，调用时传 {} 即可")
                .additionalProperties(false)
                .build();

        return ToolSpecification.builder()
                .name("getQuestionDetail")
                .description(QuestionToolDocs.GET_QUESTION_DETAIL)
                .parameters(argsSchema)
                .build();
    }

    /**
     * searchKnowledgePoints 的 ToolSpecification。
     */
    public ToolSpecification searchKnowledgePointsSpecification() {
        JsonObjectSchema argsSchema = JsonObjectSchema.builder()
                .description(KnowledgeToolParamDocs.SEARCH_KNOWLEDGE_POINTS_ARGS_PARAM)
                .addProperty("query", JsonStringSchema.builder()
                        .description("知识点检索词，必填，例如：导数、洛必达法则、大数定律")
                        .build())
                .addProperty("knowledgeScopes", JsonArraySchema.builder()
                        .description("知识点所属知识点领域列表，可选，例如：高等数学、概率论与数理统计、大学物理")
                        .items(JsonStringSchema.builder()
                                .description("单个知识点领域名称")
                                .build())
                        .build())
                .addProperty("topK", JsonNumberSchema.builder()
                        .description("返回前 K 个候选知识点，可选，建议 3~10")
                        .build())
                .addProperty("minScore", JsonNumberSchema.builder()
                        .description("最低相似度阈值，可选，范围通常为 0~1")
                        .build())
                .required("query")
                .additionalProperties(false)
                .build();

        return ToolSpecification.builder()
                .name("searchKnowledgePoints")
                .description(KnowledgeToolDocs.SEARCH_KNOWLEDGE_POINTS)
                .parameters(argsSchema)
                .build();
    }
    /**
     * searchQuestionsByKnowledgePoints 鐨?ToolSpecification銆?     */
    public ToolSpecification searchQuestionsByKnowledgePointsSpecification() {
        JsonObjectSchema argsSchema = JsonObjectSchema.builder()
                .description(KnowledgeToolParamDocs.SEARCH_QUESTIONS_BY_KNOWLEDGE_POINTS_ARGS_PARAM)
                .addProperty("knowledgePointIds", JsonArraySchema.builder()
                        .description("鐭ヨ瘑鐐?ID 鍒楄〃")
                        .items(JsonNumberSchema.builder()
                                .description("鍗曚釜鐭ヨ瘑鐐?ID")
                                .build())
                        .build())
                .addProperty("topK", JsonNumberSchema.builder()
                        .description("杩斿洖鍓?K 鏉￠鐩紝鍙€?")
                        .build())
                .addProperty("typeCode", JsonEnumSchema.builder()
                        .description("棰樺瀷杩囨护锛屽彲閫?")
                        .enumValues(TYPE_CODES)
                        .build())
                .addProperty("difficultyMin", JsonNumberSchema.builder()
                        .description("闅惧害涓嬮檺锛屽彲閫?")
                        .build())
                .addProperty("difficultyMax", JsonNumberSchema.builder()
                        .description("闅惧害涓婇檺锛屽彲閫?")
                        .build())
                .required("knowledgePointIds")
                .additionalProperties(false)
                .build();

        return ToolSpecification.builder()
                .name("searchQuestionsByKnowledgePoints")
                .description(KnowledgeToolDocs.SEARCH_QUESTIONS_BY_KNOWLEDGE_POINTS)
                .parameters(argsSchema)
                .build();
    }
}
