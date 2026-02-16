package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用返回结果包装类
 *
 * @param <T> 数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult <T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
}