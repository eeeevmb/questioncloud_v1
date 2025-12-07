package cn.sztu.questioncloud.web.rest.v1.question.req;

import cn.sztu.questioncloud.common.model.query.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题集内题目分页查询请求
 * TODO 知识点条件筛选待补充
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QuestionInCollectionPageQuery extends BasePageQuery {

    /**
     * 题型编号
     */
    private String typeCode;

    /**
     * 难度下限
     */
    private Double levelMin;

    /**
     * 难度上限
     */
    private Double levelMax;
}
