package cn.sztu.questioncloud.infrastructure.common.ai.constant;

/**
 * 供大模型在工具调用时阅读的说明文档。
 *
 * <p>用于向模型解释各工具的用途、触发时机、返回值语义与注意事项，帮助模型在合适的场景下正确调用工具。</p>
 */
public class QuestionToolDocs {
    public static final String CREATE_QUESTION = """
    【createQuestion】在题集中添加题目，返回新题目ID。
    何时调用：用户明确提出“新增/创建题目”或你需要把用户给出的题目落库时。
    返回：ToolResult<Long>
      - success=true: data 为 questionId
      - success=false: code/message 给出失败原因（必须把 message 反馈给用户，并提示补充/修正参数后重试）
    注意：
    - 不要臆造 collectionId/typeCode/options 等；缺信息就先追问或让用户选择。
    - difficulty 可省略；省略时系统默认 0.50（不要传 null）。
    """;

    public static final String GET_QUESTION_DETAIL = """
    【getQuestionDetail】获取“当前会话上下文中已选题目”的详情列表（题干、选项、答案、解析、难度等）。
    入参：
    - memoryId：会话记忆ID（由 @ToolMemoryId 自动注入；调用时无需手动传）
    
    何时调用：
    - 用户说“讲解这道题/这题答案是什么/给这几道题解析”，且前端已通过 UI 选择题目作为上下文
    - 或者你已在 Redis 会话上下文中写入 selectedQuestionIds
    
    返回：ToolResult<List<QuestionDetail>>
    - success=true：data 为题目详情列表（可能为空列表）
    - success=false：code/message 说明原因（例如上下文不存在/已过期、题目不存在、无权限）
    
    注意：
    - 回答必须以返回的题干/选项为准，不要自行改写题干或选项文本。
    - 若 data 为空列表，应提示用户先在 UI 选择题目或先进行搜索。
    """;

    public static final String RAG_SEARCH = """
    【searchQuestion】根据用户的模糊描述 + 题集ID + 可选筛选条件，返回候选题列表。
    何时调用：用户说“找一下类似的题/我记得有道题大概是…/讲解一下这道题”但无法定位 questionId。
    返回：ToolResult<List<QuestionHitDTO>>
      - data 为候选题（含 matchRank、questionId、stemPreview 等）
    注意：
    - 这是“候选”，不是最终定位；通常需要让用户从候选中选一题，再调用 getQuestionDetail。
    """;
}
