package cn.sztu.questioncloud.web.rest.v1.paper.req;

import cn.sztu.questioncloud.web.rest.v1.common.req.BasePageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PaperQueryReq extends BasePageReq {
    @Schema(description = "搜索关键词(标题)", example = "期末")
    private String keyword;

    //条件过滤 ：
    //...
}
