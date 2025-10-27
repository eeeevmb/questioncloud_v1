package cn.sztu.questioncloud.application.user.dto;

import lombok.Builder;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

/**
 * 头像图片传输对象
 * 用于存储图片文件载体和MIME类型
 */
@Builder
public record AvatarDTO(
        Resource resource,
        MediaType mediaType
) {}
