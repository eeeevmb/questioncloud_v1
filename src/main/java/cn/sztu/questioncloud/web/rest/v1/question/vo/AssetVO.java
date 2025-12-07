package cn.sztu.questioncloud.web.rest.v1.question.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetSection;
import lombok.Data;

/**
 * 题目附件视图对象
 * 用于在题目详情中描述每一个附件资源
 */
@Data
public class AssetVO {
    /**
     * 文件ID
     */
    private Long fileId;

    /**
     * 附件所在区域
     * 标记附件属于题干(PRO)还是解析(SOLU)
     */
    private AssetSection section;

    /**
     * 在区域出现的顺序
     */
    private Integer ordinal;

}
