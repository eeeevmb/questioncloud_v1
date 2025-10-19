package cn.sztu.questioncloud.web.rest.v1.user.vo;

import lombok.Builder;

@Builder
public record LoginVO (
        Long userId,
        String username,
        String tokenName,
        String tokenValue
) {}
