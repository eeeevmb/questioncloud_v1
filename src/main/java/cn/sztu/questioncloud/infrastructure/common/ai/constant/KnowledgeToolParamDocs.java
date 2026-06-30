package cn.sztu.questioncloud.infrastructure.common.ai.constant;

/**
 * 知识点工具参数文案描述
 */
public class KnowledgeToolParamDocs {
    public static final String SEARCH_KNOWLEDGE_POINTS_ARGS_PARAM = """
    知识点检索参数：
    - query：必填，知识点关键词或自然语言描述，例如“导数应用”“大数定律”“洛必达法则”
    - knowledgeScopes：可选，限定知识点领域列表，例如 ["高等数学", "概率论与数理统计"]
    - topK：可选，返回前 K 个候选知识点，建议 3~10
    - minScore：可选，最低相似度阈值，通常为 0~1
    """;

    public static final String SEARCH_QUESTIONS_BY_KNOWLEDGE_POINTS_ARGS_PARAM = """
    按知识点搜索题目参数：
    - knowledgePointIds：必填，知识点 ID 列表，通常来自 searchKnowledgePoints 的结果
    - topK：可选，返回前 K 条题目，建议 5~20
    - typeCode：可选，题型过滤，可选值为 single-choice、multiple-choice、true-false、fill-in、short-answer
    - difficultyMin：可选，难度下限，范围 0~1
    - difficultyMax：可选，难度上限，范围 0~1
    """;

    private KnowledgeToolParamDocs() {
    }
}
