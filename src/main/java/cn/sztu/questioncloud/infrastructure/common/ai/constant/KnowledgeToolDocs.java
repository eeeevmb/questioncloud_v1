package cn.sztu.questioncloud.infrastructure.common.ai.constant;

/**
 * 供大模型在工具调用时阅读的说明文档。
 *
 * <p>用于向模型解释各工具的用途、触发时机、返回值语义与注意事项，帮助模型在合适的场景下正确调用工具。</p>
 */
public class KnowledgeToolDocs {
    public static final String SEARCH_KNOWLEDGE_POINTS = """
    根据输入的关键词或自然语言描述，从知识点向量库中检索相关知识点候选。
    适用于先定位知识点，再基于知识点继续搜索相关题目或构建组卷计划。
    返回结果为候选知识点列表，不直接返回题目。
    """;
    public static final String SEARCH_QUESTIONS_BY_KNOWLEDGE_POINTS = """
    根据一个或多个知识点 ID，在当前会话绑定的题集中检索关联题目候选。
    适用于先定位知识点，再基于知识点继续查找可用于组卷的题目。
    返回结果为题目候选列表，不会直接创建试卷或修改题目。
    """;
}
