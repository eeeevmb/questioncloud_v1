package cn.sztu.questioncloud.infrastructure.common.ai.spec;

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
 * 手写工具规格工厂。
 *
 */
@Component
public class QuestionToolSpecificationFactory {

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
                "getQuestionDetail", getQuestionDetailSpecification()
        );
    }

    /**
     * createQuestion 的手写 ToolSpecification。
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
                .description("创建题目参数（当前会话绑定题集）")
                .addProperty("typeCode", JsonEnumSchema.builder()
                        .description("题型代码")
                        .enumValues(TYPE_CODES)
                        .build())
                .addProperty("title", JsonStringSchema.builder()
                        .description("题目标题，建议在12字以内")
                        .build())
                .addProperty("stem", JsonStringSchema.builder()
                        .description("题干，支持 LaTeX 文本")
                        .build())
                .addProperty("options", JsonArraySchema.builder()
                        .description("选择题选项；仅单选/多选题使用")
                        .items(optionItemSchema)
                        .build())
                .addProperty("correctOptions", JsonArraySchema.builder()
                        .description("正确选项；单选长度=1，多选长度>=1")
                        .items(JsonEnumSchema.builder()
                                .enumValues(OPTION_KEYS)
                                .build())
                        .build())
                .addProperty("judgeAnswer", JsonEnumSchema.builder()
                        .description("判断题答案，仅 true-false 使用")
                        .enumValues("T", "F")
                        .build())
                .addProperty("answer", JsonStringSchema.builder()
                        .description("展示用答案文本；fill-in/short-answer 建议提供")
                        .build())
                .addProperty("solution", JsonStringSchema.builder()
                        .description("题目解析，可省略")
                        .build())
                .addProperty("difficulty", JsonNumberSchema.builder()
                        .description("难度，范围 0.00~1.00，除非用户指定否则省略")
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
     * searchQuestion 的手写 ToolSpecification。
     */
    public ToolSpecification searchQuestionSpecification() {
        JsonObjectSchema ragSearchParamSchema = JsonObjectSchema.builder()
                .description("题目检索筛选参数，可省略或传空对象")
                .addProperty("difficultyMin", JsonNumberSchema.builder()
                        .description("难度下限，范围 0.00~1.00")
                        .build())
                .addProperty("difficultyMax", JsonNumberSchema.builder()
                        .description("难度上限，范围 0.00~1.00")
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
     * getQuestionDetail 的手写 ToolSpecification。
     */
    public ToolSpecification getQuestionDetailSpecification() {
        JsonObjectSchema argsSchema = JsonObjectSchema.builder()
                .description("该工具无显式业务参数，系统内部自动注入参数，调用时传 {} 即可")
                .additionalProperties(false)
                .build();

        return ToolSpecification.builder()
                .name("getQuestionDetail")
                .description(QuestionToolDocs.GET_QUESTION_DETAIL)
                .parameters(argsSchema)
                .build();
    }
}
