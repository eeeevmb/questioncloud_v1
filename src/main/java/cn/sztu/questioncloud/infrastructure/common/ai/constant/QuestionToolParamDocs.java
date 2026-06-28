package cn.sztu.questioncloud.infrastructure.common.ai.constant;

/**
 * 题目领域工具参数文档描述
 * 方便大模型快速理解工具调用方法与用途
 */
public class QuestionToolParamDocs {
    public static final String CREATE_QUESTION_PARAM = """
        创建题目参数（JSON对象）。

        作用说明：
        - 该工具用于“在当前会话绑定的题集中创建 1 道题目”。
        - 工具不会接收 collectionId、userId、questionIds 等内部上下文参数；这些信息由系统自动注入。
        - 调用成功时，通常只返回 success=true，data 可能为空；data 为空也表示创建成功。
        - 不要传任何内部ID，不要向用户暴露任何内部ID。

        全题型必填字段（所有题型都必须提供）：
        - typeCode：题型代码
        - stem：题干，可包含 LaTeX 公式

        typeCode 取值：
        - single-choice   单选题
        - multiple-choice 多选题
        - true-false      判断题
        - fill-in         填空题
        - short-answer    简答题

        通用字段（任何题型都可填写，可省略）：
        - title：题目标题
        - answer：答案文本（主要用于展示；填空题/简答题使用它承载答案）
        - solution：题目解析
        - difficulty：number，题目难度，范围 0.00 ~ 1.00；省略时系统默认 0.50

        题型专用字段：
        - options：array，可选。元素结构为 { "key": "A", "content": "..." }。
          - key 使用大写字母：A / B / C / D ...
          - content 可包含 LaTeX 公式
        - correctOptions：array<string>，可选。元素为 "A" / "B" / "C" ...，且必须存在于 options.key 中
        - judgeAnswer：string，可选。仅当 typeCode=true-false 时使用，只能是 "T" 或 "F"

        题型约束：
        1) 当 typeCode=single-choice：
           - options 必填，例如：[{ "key":"A","content":"..." }, { "key":"B","content":"..." }]
           - correctOptions 必填，且长度必须为 1，例如：["A"]
        2) 当 typeCode=multiple-choice：
           - options 必填
           - correctOptions 必填，且长度必须 >= 1，例如：["A","C"]
        3) 当 typeCode=true-false：
           - judgeAnswer 必填，只能是 "T" 或 "F"
        4) 当 typeCode=fill-in：
           - answer 必填，填空答案文本（多个空可用分号或换行分隔）
        5) 当 typeCode=short-answer：
           - answer 必填，简答题答案文本

        调用约束：
        - 只传与当前题型匹配的字段；不相关字段应省略。
        - 缺少必填字段时，不要调用工具，应先补齐参数。
        - 不要编造用户未要求的内部参数，也不要传入 collectionId、userId、questionIds 等上下文字段。
        - 若信息不足以构造合法题目，不应强行调用。
        """;

    public static final String RAG_SEARCH_ARGS_PARAM = """
        题目检索参数（JSON对象）。

        作用说明：
        - 该工具用于“在当前会话绑定的题集中检索题目”。
        - 工具不会接收 collectionId、userId、questionIds 等内部上下文参数；这些信息由系统自动注入。
        - 不要传任何内部ID，不要向用户暴露任何内部ID。

        顶层字段：
        - query: string，必填。用户的自然语言检索描述。
        - ragSearchParam: object，可选。筛选条件对象；可省略，也可传空对象 {}。

        query 示例：
        - “求三重积分，旋转体”
        - “链表反转，快慢指针”
        - “Redis 持久化机制”

        ragSearchParam 支持以下字段：
        - difficultyMin: number，可选。题目难度下限，范围 0 ~ 1（含）
        - difficultyMax: number，可选。题目难度上限，范围 0 ~ 1（含）
        - typeCode: string，可选。题型代码，取值之一：
          - single-choice
          - multiple-choice
          - true-false
          - fill-in
          - short-answer

        约束：
        - query 必须提供，且不能为空白字符串。
        - 若同时提供 difficultyMin 与 difficultyMax，必须满足 difficultyMin <= difficultyMax。
        - 未提供的筛选字段表示“不按该条件过滤”。
        - 若不需要额外筛选，可直接省略 ragSearchParam，或传 {}。

        示例：
        1) 只按自然语言检索：
        {
          "query": "求三重积分，旋转体"
        }

        2) 检索“链表反转”，并筛选难度 0.3 ~ 0.7 的多选题：
        {
          "query": "链表反转，快慢指针",
          "ragSearchParam": {
            "typeCode": "multiple-choice",
            "difficultyMin": 0.3,
            "difficultyMax": 0.7
          }
        }

        3) 只筛选判断题：
        {
          "query": "程序流程判断",
          "ragSearchParam": {
            "typeCode": "true-false"
          }
        }
        """;

    /** 旧版拆分参数文档，已废弃；当前统一使用 RAG_SEARCH_ARGS_PARAM。 */
    @Deprecated
    public static final String RAG_SEARCH_PARAM_QUERY = """
            用户的模糊描述（自然语言）。例如：
            - “求三重积分，旋转体”
            - “链表反转，快慢指针”
            """;

    /** 旧版拆分参数文档，已废弃；当前统一使用 RAG_SEARCH_ARGS_PARAM。 */
    @Deprecated
    public static final String RAG_SEARCH_PARAM = """
            题目筛选参数(JSON对象)。全部字段均可省略；不需要的字段不要传。
            
            字段说明：
            - difficultyMin: number，可选。题目难度下限，范围 0~1（含）。
            - difficultyMax: number，可选。题目难度上限，范围 0~1（含）。
            - typeCode: string，可选。题型代码，取值之一：
              - single-choice   单选题
              - multiple-choice 多选题
              - true-false      判断题
              - fill-in         填空题
              - short-answer    简答题
            
            约束：
            - 若同时提供 difficultyMin 与 difficultyMax，必须满足 difficultyMin <= difficultyMax。
            - 未提供的字段表示“不按该条件过滤”。
            
            示例：
            1) 只筛选难度 0.3~0.7 的多选题：
            { "typeCode":"multiple-choice", "difficultyMin":0.3, "difficultyMax":0.7 }
            
            2) 只筛选判断题：
            { "typeCode":"true-false" }
            
            3) 不筛选（全部省略）：
            { }
            """;
}
