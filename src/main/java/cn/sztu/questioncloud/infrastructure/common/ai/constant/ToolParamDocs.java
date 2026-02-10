package cn.sztu.questioncloud.infrastructure.common.ai.constant;

public class ToolParamDocs {
    public static final String CREATE_QUESTION_PARAM = """
        创建题目参数(JSON对象)。
        全题型必填字段（所有题型都必须提供）：
        - typeCode：题型代码
        - stem：题干
        - collectionId：题集ID

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
        - difficulty：题目难度 0.00~1.00（省略时系统默认 0.50）
        
        题型专用字段规则（仅当 typeCode 匹配时才允许出现；不相关字段不要传/省略）：
        - options 的 key 使用大写字母：A/B/C/D...
        
        1) 仅当 typeCode=single-choice：
           - options 必填：[{ "key":"A","content":"..." }, ...]
           - correctOptions 必填：例如 ["A"]（必须在 options.key 中）
        2) 仅当 typeCode=multiple-choice：
           - options 必填：[{ "key":"A","content":"..." }, ...]
           - correctOptions 必填：例如 ["A","C"]（每个都必须在 options.key 中）
        3) 仅当 typeCode=true-false：
           - judgeAnswer 必填：只能是 "T" 或 "F"
        4) 仅当 typeCode=fill-in：
           - answer 必填：填空答案文本（多个空可用分号/换行分隔）
        5) 仅当 typeCode=short-answer：
           - answer 必填：简答题答案文本
        """;
}
