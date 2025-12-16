package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionAsset;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionOption;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.QuestionAssetListTypeHandler;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.QuestionOptionListTypeHandler;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目版本，同题目不同版本的正文快照
 */
@Builder
@Data
@Table("question_version")
@AllArgsConstructor
@NoArgsConstructor
public class QuestionVersionEntity implements Serializable {
    /**
     * 题目版本主键，使用雪花算法生成
     */
    @TableId(value = IdAutoType.NONE)
    private Long id;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 题目版本号，从1递增
     */
    private Integer versionNo;

    /**
     * 题目类型代码
     */
    private String typeCode;

    /**
     * 题目标题/摘要
     */
    private String title;

    /**
     * 题干原文（LaTex格式）
     */
    private String stem;

    /**
     * 题目选项
     */
    @TableField(typeHandler = QuestionOptionListTypeHandler.class)
    private List<QuestionOption> options;

    /**
     * 答案原文（LaTex格式）
     */
    private String answer;

    /**
     * 判分用答案
     */
    private String answerKey;

    /**
     * 题目解析（LaTex格式）
     */
    private String solution;

    /**
     * 图片/附件
     */
    @TableField(typeHandler = QuestionAssetListTypeHandler.class)
    private List<QuestionAsset> assets;

    /**
     * 创建者id
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}