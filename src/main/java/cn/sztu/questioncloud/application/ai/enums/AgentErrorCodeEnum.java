package cn.sztu.questioncloud.application.ai.enums;

import cn.sztu.questioncloud.common.constant.enums.result.ResultCodeEnum;
import lombok.Getter;

@Getter
public enum AgentErrorCodeEnum implements ResultCodeEnum {
    AGENT_NOT_FOUND("A00001", "智能体不存在"),
    AGENT_CHAT_SESSION_NOT_FOUND("A00002", "聊天会话已过期或不存在"),
    AGENT_SEARCH_RESULT_NOT_ENOUGH("A00003", "题目数量不足，请调整组卷条件或补充题库题目");

    private final String code;
    private final String message;

    AgentErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
