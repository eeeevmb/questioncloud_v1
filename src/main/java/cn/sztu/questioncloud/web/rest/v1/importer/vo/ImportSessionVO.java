package cn.sztu.questioncloud.web.rest.v1.importer.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ImportSessionVO {
    private Long importId;
    private Integer status;
    private Integer total;
    private Integer validCnt;
    private Integer invalidCnt;
    private BigDecimal progress;
}
