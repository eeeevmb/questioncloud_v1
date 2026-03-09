package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.dto;

import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetSection;
import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionAsset implements Serializable {
    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 图片出现的位置，PRO/SOLU
     */
    private AssetSection section;

    /**
     * 在该section中出现的顺序
     */
    private Integer ordinal;
}
