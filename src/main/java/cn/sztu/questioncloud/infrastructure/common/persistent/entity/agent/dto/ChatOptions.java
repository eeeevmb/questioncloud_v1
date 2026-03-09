package cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto;

import lombok.Data;

@Data
public class ChatOptions {
    /**
     * 要使用的模型名称（例如，gpt-4o、gpt-4o-mini 等）
     */
    private String modelName;

    /**
     * 使用的采样温度，介于 0 和 2 之间。
     * 较高的值如 0.8 会使输出更随机，而较低的值如 0.2 会使其更集中和确定性。
     */
    private Double temperature;

    /**
     * 在聊天完成中可以生成的最大Tokens数。
     */
    private Integer maxTokens;

    /**
     * 介于 -2.0 和 2.0 之间的数字。
     * 正值会根据文本中已有的频率惩罚新Token，降低模型逐字重复相同行的可能性。
     */
    private Double frequencyPenalty;
}
