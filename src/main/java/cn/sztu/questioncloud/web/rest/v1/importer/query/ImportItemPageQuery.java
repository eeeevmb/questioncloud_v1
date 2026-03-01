package cn.sztu.questioncloud.web.rest.v1.importer.query;

import cn.sztu.questioncloud.common.model.query.BasePageQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportItemPageQuery extends BasePageQuery {
    /**
     * VALID / INVALID / ALL
     */
    private String status = "ALL";
}
