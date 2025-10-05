package cn.sztu.questioncloud.application.user.dto;

import lombok.Builder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@Builder
public record AvatarDTO(
        Resource resource,
        MediaType mediaType
) {}
