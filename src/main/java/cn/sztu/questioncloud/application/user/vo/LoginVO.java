package cn.sztu.questioncloud.application.user.vo;

import lombok.Builder;

@Builder
public record LoginVO (
        Long userId,
        String username,
        String tokenName,
        String tokenValue
) {}
