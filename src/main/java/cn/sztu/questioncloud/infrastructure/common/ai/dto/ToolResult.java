package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolResult <T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
}