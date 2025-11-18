package cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto;

import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetSection;
import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetType;
import cn.sztu.questioncloud.web.rest.v1.question.vo.AssetVO;
import lombok.Builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 附件快照
 *
 * @author Codex
 */
@Builder
public record AssetSnapshot(
        String slotId,                  // 解析阶段生成的占位符 ID，如 pro-1 或 solu-3
        AssetSection section,           // 枚举，标记属于题干(PRO)还是解析(SOLU)
        Integer ordinal,                // 在所属段落里的出现顺序，便于重排
        AssetType type,                 // IMAGE / LATEX_ATTACHMENT 等，方便扩展
        String latexPlaceholder,        // 原文中替换出来的占位符，例如 "asset://{slotId}"
        Long fileId,                    // 对应物理文件的引用
        String storagePath              // 存储路径
) {}
