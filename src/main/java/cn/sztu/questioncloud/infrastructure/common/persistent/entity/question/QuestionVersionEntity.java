package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目版本，同题目不同版本的正文快照
 */
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
    private Integer versionNO;

    /**
     * 题目类型代码
     */
    private String typeCode;

    /**
     * 题目标题/摘要
     */
    private String title;

    /**
     * 题干LaTex
     */
    private String stem;

    /**
     * 选择题选项（JSON格式）
     */
    private String options;

    /**
     * 大题答案（JSON格式）
     */
    private String answer;

    /**
     * 解析LaTex
     */
    private String solution;

    /**
     * 图片/附件
     */
    private String assets;

    /**
     * 创建者id
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
