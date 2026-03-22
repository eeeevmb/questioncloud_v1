package cn.sztu.questioncloud.web.rest.v1.paper.req;

import cn.sztu.questioncloud.common.model.query.BasePageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 试卷列表分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaperPageQuery extends BasePageQuery {

    /**
     * 试卷状态，可选
     */
    private Integer status;
}
