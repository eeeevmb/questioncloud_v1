package cn.sztu.questioncloud.infrastructure.common.ai.constant;

/**
 * 题目领域工具参数文档描述
 * 方便大模型快速理解工具调用方法与用途
 */
public class QuestionToolParamDocs {
    public static final String CREATE_QUESTION_PARAM = """
        创建题目参数(JSON对象)。
        全题型必填字段（所有题型都必须提供）：
        - typeCode：题型代码
        - stem：题干，可包含LaTeX公式

        typeCode 取值
        - single-choice   单选题
        - multiple-choice 多选题
        - true-false      判断题
        - fill-in         填空题
        - short-answer    简答题
       
        通用字段（任何题型都可填写，可省略）：
        - title：题目标题
        - answer：答案文本（用于展示；填空/简答用它承载答案）
        - solution：题目解析
        - difficulty：number，题目难度 0.00~1.00（省略时系统默认 0.50）
        
        题型专用字段规则（仅当 typeCode 匹配时才允许出现；不相关字段不要传/省略）：
        - options：array，可选。元素结构 { "key": "A", "content": "..." }，key 使用大写字母：A/B/C/D...，content可包含LaTeX公式
        - correctOptions：array<string>，可选。元素为 "A"/"B"/"C"...（必须存在于 options.key）
        - judgeAnswer：string，可选。仅当 typeCode=true-false 时必填，只能是 "T" 或 "F"
        
        1) 仅当 typeCode=single-choice：
           - options 必填：[{ "key":"A","content":"..." }, ...]
           - correctOptions 必填：例如 ["A"]（必须在 options.key 中），correctOptions 长度必须为 1
        2) 仅当 typeCode=multiple-choice：
           - options 必填：[{ "key":"A","content":"..." }, ...]
           - correctOptions 必填：例如 ["A","C"]（每个都必须在 options.key 中），correctOptions 长度必须 >= 1
        3) 仅当 typeCode=true-false：
           - judgeAnswer 必填：只能是 "T" 或 "F"
        4) 仅当 typeCode=fill-in：
           - answer 必填：填空答案文本（多个空可用分号/换行分隔）
        5) 仅当 typeCode=short-answer：
           - answer 必填：简答题答案文本
           
       【调用约束】
       - 缺少必填字段时：不要调用工具，先向用户追问补齐。
       - 不要编造选项/答案/解析：信息不足就先问清楚再调用。
        """;

    public static final String RAG_SEARCH_PARAM_QUERY = """
            用户的模糊描述（自然语言）。例如：
            - “求三重积分，旋转体”
            - “链表反转，快慢指针”
            """;

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
