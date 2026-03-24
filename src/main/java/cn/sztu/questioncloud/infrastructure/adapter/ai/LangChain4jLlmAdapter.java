package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import cn.sztu.questioncloud.application.ai.dto.PaperGenerationPlan;
import cn.sztu.questioncloud.application.ai.dto.QuestionSemanticEnrichmentDTO;
import cn.sztu.questioncloud.application.ai.port.LlmPort;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.math.BigDecimal;
import java.util.List;

/**
 * LLM适配器，提供大模型对话能力
 */
@Component
@RequiredArgsConstructor
public class LangChain4jLlmAdapter implements LlmPort {
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final ObjectMapper objectMapper;

    private static final BigDecimal DEFAULT_DIFFICULTY = BigDecimal.valueOf(0.50);
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
     * 同步返回聊天响应（用于决定是否调用工具）
     *
     * @param definition         智能体定义
     * @param messages           聊天记录
     * @param toolSpecifications 工具介绍
     * @return 聊天响应
     */
    @Override
    public ChatResponse chat(AgentDefinition definition, List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder()
                        .modelName(definition.getChatOptions().getModelName())
                        .temperature(definition.getChatOptions().getTemperature())
                        .maxOutputTokens(definition.getChatOptions().getMaxTokens())
                        .frequencyPenalty(definition.getChatOptions().getFrequencyPenalty())
                        .toolSpecifications(toolSpecifications)
                        .build())
                .build();
        return chatModel.chat(request);
    }

    /**
     * 生成会话标题
     * @param userMessage 用户消息
     * @return 会话标题
     */
    @Override
    public String generateSessionTitle(UserMessage userMessage) {
        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            SystemMessage.from("""
                                你是会话标题生成助手。
                                请根据用户首条消息生成一个简洁的中文会话标题。
                                要求：
                                1. 只输出标题本身
                                2. 不要加引号、句号、冒号、序号、解释
                                3. 不要换行
                                4. 长度控制在 8~15 个汉字
                                5. 若内容偏技术，标题尽量概括主题，不要复述整句
                                """),
                            userMessage
                    ))
                    .parameters(ChatRequestParameters.builder()
                            .temperature(0.2)
                            .maxOutputTokens(30)
                            .build())
                    .build();

            String title = chatModel.chat(request).aiMessage().text();
            // 错误生成兜底
            if (title == null || title.isBlank()) {
                return "新会话";
            }

            title = title.trim()
                    .replace("\n", "")
                    .replace("\"", "")
                    .replace("“", "")
                    .replace("”", "");

            if (title.length() > 20) {
                title = title.substring(0, 20);
            }

            return title.isBlank() ? "新会话" : title;
        } catch (Exception e) {
            return "新会话";
        }
    }

    /**
     * 根据题目文本生成语义增强后的题目对象
     *
     * @param input 输入
     * @return 语义增强题目对象
     */
    @Override
    public QuestionSemanticEnrichmentDTO generateQuestionSemanticEnrichmentDTO(String input) {
        try {
            ResponseFormat responseFormat = buildQuestionSemanticEnrichmentResponseFormat();

            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            SystemMessage.from("""
                                    你是题目语义增强助手。
                                    你必须严格输出 QuestionSemanticEnrichmentDTO 的 JSON 对象，不要输出 markdown 代码块，不要输出解释。

                                    任务要求：
                                    1. 对输入中的题干与解析进行语义清洗，去除 LaTeX 排版噪声（如无意义换行、重复空格、冗余格式符）。
                                    2. 保留数学语义与计算逻辑，不要改写题意本身。
                                    3. 返回 stem、solution、knowledgePoints 三个字段。
                                    4. knowledgePoints 为简洁中文知识点标签列表，建议 2~4 个核心知识点 + 1 个解题方法标签。
                                    5. 不要输出过于宽泛、过于碎片化、偏讲解总结的标签，例如“充分条件”“反例分析”“系数提取”这类低检索价值表达。
                                    6. 不要为了凑数量强行补充标签；如果核心标签只有 2~3 个，就输出 2~3 个。
                                    7. 若某个候选标签不是该题的主要求解路径或主干知识点，则不要输出。
                                    8. 不要生成与题目无关的知识点；不要输出空对象。

                                    输出必须可被 JSON 反序列化为 QuestionSemanticEnrichmentDTO。
                                    """),
                            UserMessage.from("""
                                    原始题目文本如下：
                                    %s
                                    """.formatted(input))
                    ))
                    .parameters(ChatRequestParameters.builder()
                            .temperature(0.1)
                            .maxOutputTokens(1024)
                            .responseFormat(responseFormat)
                            .build())
                    .build();

            ChatResponse response = chatModel.chat(request);
            AiMessage aiMessage = response.aiMessage();
            if (aiMessage == null || aiMessage.text() == null || aiMessage.text().isBlank()) {
                throw new IllegalStateException("模型未返回可解析的语义增强结果");
            }

            QuestionSemanticEnrichmentDTO dto = objectMapper.readValue(aiMessage.text(), QuestionSemanticEnrichmentDTO.class);
            if (dto.getKnowledgePoints() == null) {
                dto.setKnowledgePoints(List.of());
            }
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("生成题目语义增强结果失败", e);
        }
    }

    /**
     * 根据用户描述生成题目草稿
     *
     * @param userInput 用户输入
     * @return 题目草稿
     */
    @Override
    public QuestionDraft generateQuestionDraft(String userInput) {
        try {
            ResponseFormat responseFormat = buildQuestionDraftResponseFormat();

            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            SystemMessage.from("""
                                    你是题目草稿生成助手。
                                    你必须严格输出 QuestionDraft 的 JSON 对象，不要输出 markdown 代码块，不要输出解释。
                                    
                                    字段规则（与创建题目参数一致）：
                                    - typeCode: 必填，枚举为 single-choice/multiple-choice/true-false/fill-in/short-answer
                                    - stem: 必填
                                    - title: 必选
                                    - difficulty: 可选；若用户未指定请输出 0.50
                                    - solution: 必选
                                    - answer: fill-in/short-answer 建议提供；true-false 也可提供
                                    
                                    题型专用字段：
                                    - single-choice/multiple-choice:
                                      必须提供 options（每项含 key 和 content）与 correctOptions
                                    - true-false:
                                      必须提供 judgeAnswer，且只能是 T 或 F
                                    - fill-in/short-answer:
                                      使用 answer，且不要提供 options/correctOptions/judgeAnswer
                                    
                                    输出必须可被 JSON 反序列化为 QuestionDraft。
                                    """),
                            UserMessage.from(userInput)
                    ))
                    .parameters(ChatRequestParameters.builder()
                            .temperature(0.1)
                            .maxOutputTokens(1024)
                            .responseFormat(responseFormat)
                            .build())
                    .build();

            ChatResponse response = chatModel.chat(request);
            AiMessage aiMessage = response.aiMessage();
            if (aiMessage == null || aiMessage.text() == null || aiMessage.text().isBlank()) {
                throw new IllegalStateException("模型未返回可解析的题目草稿");
            }

            QuestionDraft draft = objectMapper.readValue(aiMessage.text(), QuestionDraft.class);
            if (draft.getDifficulty() == null) {
                draft.setDifficulty(DEFAULT_DIFFICULTY);
            }
            return draft;
        } catch (Exception e) {
            throw new RuntimeException("生成题目草稿失败", e);
        }
    }

    /**
     * 根据用户描述生成组卷计划
     *
     * @param message 用户描述
     * @param allowedTypeCodes 允许的题型范围
     * @return 组卷计划
     */
    @Override
    public PaperGenerationPlan generatePaperGenerationPlan(String message, List<String> allowedTypeCodes) {
        try {
            ResponseFormat responseFormat = buildPaperGenerationPlanResponseFormat();

            List<String> safeAllowedTypeCodes = (allowedTypeCodes == null || allowedTypeCodes.isEmpty())
                    ? TYPE_CODES
                    : allowedTypeCodes;

            String userPrompt = """
                    组卷描述：%s
                    
                    允许的题型范围：%s
                    
                    请只在允许的题型范围内生成 bucketPlans。
                    不要输出范围外的题型。
                    """.formatted(message, safeAllowedTypeCodes);

            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            SystemMessage.from("""
                                    你是组卷计划生成助手。
                                    你必须严格输出 PaperGenerationPlan 的 JSON 对象，不要输出 markdown 代码块，不要输出解释。
                                    
                                    你的职责：
                                    - 根据用户的组卷描述，为允许的题型范围补充合适的检索 topics。
                                    - 你只能补充“每种题型下应该检索哪些知识点/考点关键词”，不能擅自新增题型，也不能决定每种题型出几道题。
                                    - 题型数量、每种题型出几道题，属于外部约束，不由你决定。
                                    
                                    输出规则：
                                    1. 只输出可被 JSON 反序列化的 PaperGenerationPlan 对象。
                                    2. reason 必填，用 30～60 字简洁中文说明本次组卷计划的覆盖思路与题型分工。
                                    3. bucketPlans 必填，只能包含“允许的题型范围”中的 typeCode；不得新增范围外的题型。
                                    4. 若允许的题型范围包含多个题型，尽量为每个允许题型都生成一个 bucketPlan，除非用户描述与该题型明显无关。
                                    5. bucketPlans 中每个元素只包含：
                                       - typeCode: 题型代码
                                       - topics: 该题型下建议检索的知识点/考点关键词列表
                                    6. topics 必须是“与用户当前学科/课程相匹配的知识点短语”或“可直接用于检索的考点关键词”，不要写成“极限值填空”“导数值填空”“定积分数值”这类题目形式描述，也不要脱离用户给定的课程范围。
                                    7. topics 应尽量简洁、可检索、风格统一，建议长度为 4~12 个字，例如“函数极限计算”“连续性的判定”“隐函数求导”；若用户是其他学科，应替换为对应学科的知识点表达。
                                    8. topics 要尽量覆盖用户描述中的核心模块，优先依据用户输入中明确提到的课程名称、章节范围、知识模块与考查重点进行展开；若用户未明确课程名称，也要从描述中归纳出对应学科的核心知识点，不要默认固定为高等数学。
                                    9. 不同题型的 topics 要体现题型特点：
                                       - single-choice / true-false：更适合基础概念、性质辨析、定义判定类知识点
                                       - multiple-choice：更适合关系辨析、性质组合、多个结论同时判断类知识点
                                       - fill-in：更适合公式、结果、基础计算对应的知识点
                                       - short-answer：更适合综合计算、证明、推导、应用类知识点
                                    10. topics 之间不要大量重复，也不要过于空泛，例如“高数基础”“综合应用”这类词不要使用。
                                    11. 每个题型建议给出 3~5 个 topics；若用户描述信息特别丰富，可适当增加，但不要堆砌，若用户描述极其简略，topics 数量应以覆盖核心考点为主，宁缺毋滥，避免产生无关干扰词。
                                    12. 若用户描述较泛，也要尽量提炼出合理 topics；不要返回空 topics。
                                    13. 若用户描述与允许题型范围存在冲突，优先服从允许题型范围。
                                    
                                    typeCode 取值示例：
                                    - single-choice
                                    - multiple-choice
                                    - true-false
                                    - fill-in
                                    - short-answer
                                    
                                    示例1：
                                    用户描述：覆盖极限、连续、导数、定积分、多元函数微分、二重积分、级数等内容。
                                    允许题型范围：[single-choice, fill-in, short-answer]
                                    合理示例（仅示意风格，不要求逐字复现）：
                                    {
                                      "reason": "覆盖高等数学主干模块，单选侧重概念辨析，填空强调基础计算，简答突出综合应用。",
                                      "bucketPlans": [
                                        {
                                          "typeCode": "single-choice",
                                          "topics": ["函数极限定义", "连续性的判定", "间断点类型"]
                                        },
                                        {
                                          "typeCode": "fill-in",
                                          "topics": ["重要极限计算", "导数计算", "定积分计算"]
                                        },
                                        {
                                          "typeCode": "short-answer",
                                          "topics": ["导数应用", "二重积分计算", "幂级数收敛区间"]
                                        }
                                      ]
                                    }
                                    
                                    示例2：
                                    用户描述：覆盖随机事件与概率、条件概率、随机变量及其分布、数字特征、大数定律与中心极限定理等概率论内容。
                                    允许题型范围：[multiple-choice, true-false, short-answer]
                                    合理示例（仅示意风格，不要求逐字复现）：
                                    {
                                      "reason": "覆盖概率论主干模块，多选与判断突出概念关系辨析，简答强调分布分析、数字特征计算与综合应用。",
                                      "bucketPlans": [
                                        {
                                          "typeCode": "multiple-choice",
                                          "topics": ["条件概率性质", "分布函数性质", "数学期望与方差关系"]
                                        },
                                        {
                                          "typeCode": "true-false",
                                          "topics": ["独立与互斥关系", "随机变量分布性质", "中心极限定理条件"]
                                        },
                                        {
                                          "typeCode": "short-answer",
                                          "topics": ["全概率公式与贝叶斯公式", "离散型随机变量分布", "正态分布概率计算", "期望与方差计算"]
                                        }
                                      ]
                                    }
                                    
                                    输出必须可被 JSON 反序列化为 PaperGenerationPlan。
                                    """),
                            UserMessage.from(userPrompt)
                    ))
                    .parameters(ChatRequestParameters.builder()
                            .temperature(0.1)
                            .maxOutputTokens(1024)
                            .responseFormat(responseFormat)
                            .build())
                    .build();

            ChatResponse response = chatModel.chat(request);
            AiMessage aiMessage = response.aiMessage();
            if (aiMessage == null || aiMessage.text() == null || aiMessage.text().isBlank()) {
                throw new IllegalStateException("模型未返回可解析的组卷计划");
            }

            return objectMapper.readValue(aiMessage.text(), PaperGenerationPlan.class);
        } catch (Exception e) {
            throw new RuntimeException("生成组卷计划失败", e);
        }
    }

    /**
     * 流式返回聊天响应（用于生成最终回复）
     *
     * @param definition         智能体定义
     * @param messages           聊天记录
     * @param toolSpecifications 工具介绍
     * @return 流式聊天响应
     */
    @Override
    public Flux<String> StreamingChat(AgentDefinition definition, List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder()
                        .modelName(definition.getChatOptions().getModelName())
                        .temperature(definition.getChatOptions().getTemperature())
                        .maxOutputTokens(definition.getChatOptions().getMaxTokens())
                        .frequencyPenalty(definition.getChatOptions().getFrequencyPenalty())
                        .toolSpecifications(toolSpecifications)
                        .build())
                .build();

        return Flux.create(emitter -> streamingChatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse != null && !partialResponse.isEmpty() && !emitter.isCancelled()) {
                    emitter.next(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (!emitter.isCancelled()) {
                    emitter.complete();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!emitter.isCancelled()) {
                    emitter.error(error);
                }
            }
        }), FluxSink.OverflowStrategy.BUFFER);
    }

    private ResponseFormat buildQuestionDraftResponseFormat() {
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

        JsonObjectSchema rootSchema = JsonObjectSchema.builder()
                .description("题目草稿输出参数")
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
                .addProperty("answer", JsonStringSchema.builder()
                        .description("展示用答案文本；fill-in/short-answer 建议提供")
                        .build())
                .addProperty("correctOptions", JsonArraySchema.builder()
                        .description("正确选项；单选长度=1，多选长度>=1")
                        .items(JsonEnumSchema.builder()
                                .description("选项键，必须为大写字母")
                                .enumValues(OPTION_KEYS)
                                .build())
                        .build())
                .addProperty("judgeAnswer", JsonEnumSchema.builder()
                        .description("判断题答案，仅 true-false 使用")
                        .enumValues("T", "F")
                        .build())
                .addProperty("solution", JsonStringSchema.builder()
                        .description("题目解析")
                        .build())
                .addProperty("difficulty", JsonNumberSchema.builder()
                        .description("难度，范围 0.00~1.00，除非用户指定否则建议为 0.50")
                        .build())
                .required("typeCode", "title", "stem", "solution")
                .additionalProperties(false)
                .build();

        JsonSchema jsonSchema = JsonSchema.builder()
                .name("QuestionDraft")
                .rootElement(rootSchema)
                .build();

        return ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(jsonSchema)
                .build();
    }

    private ResponseFormat buildPaperGenerationPlanResponseFormat() {
        JsonObjectSchema bucketPlanSchema = JsonObjectSchema.builder()
                .description("题型维度的候选检索计划")
                .addProperty("typeCode", JsonEnumSchema.builder()
                        .description("题型代码")
                        .enumValues(TYPE_CODES)
                        .build())
                .addProperty("topics", JsonArraySchema.builder()
                        .description("该题型下建议检索的考点/主题关键词列表")
                        .items(JsonStringSchema.builder()
                                .description("单个检索关键词")
                                .build())
                        .build())
                .required("typeCode", "topics")
                .additionalProperties(false)
                .build();

        JsonObjectSchema rootSchema = JsonObjectSchema.builder()
                .description("组卷计划输出参数")
                .addProperty("reason", JsonStringSchema.builder()
                        .description("本次组卷计划的简要出题思路")
                        .build())
                .addProperty("bucketPlans", JsonArraySchema.builder()
                        .description("按题型拆分的候选检索桶")
                        .items(bucketPlanSchema)
                        .build())
                .required("reason", "bucketPlans")
                .additionalProperties(false)
                .build();

        JsonSchema jsonSchema = JsonSchema.builder()
                .name("PaperGenerationPlan")
                .rootElement(rootSchema)
                .build();

        return ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(jsonSchema)
                .build();
    }

    private ResponseFormat buildQuestionSemanticEnrichmentResponseFormat() {
        JsonObjectSchema rootSchema = JsonObjectSchema.builder()
                .description("题目语义增强输出参数")
                .addProperty("stem", JsonStringSchema.builder()
                        .description("清洗后的题干，保留原题语义，去除 LaTeX 排版噪声")
                        .build())
                .addProperty("solution", JsonStringSchema.builder()
                        .description("清洗后的解析，保留解题逻辑，去除 LaTeX 排版噪声")
                        .build())
                .addProperty("knowledgePoints", JsonArraySchema.builder()
                        .description("知识点标签列表，简洁可检索")
                        .items(JsonStringSchema.builder()
                                .description("单个知识点标签")
                                .build())
                        .build())
                .required("stem", "solution", "knowledgePoints")
                .additionalProperties(false)
                .build();

        JsonSchema jsonSchema = JsonSchema.builder()
                .name("QuestionSemanticEnrichmentDTO")
                .rootElement(rootSchema)
                .build();

        return ResponseFormat.builder()
                .type(ResponseFormatType.JSON)
                .jsonSchema(jsonSchema)
                .build();
    }
}
