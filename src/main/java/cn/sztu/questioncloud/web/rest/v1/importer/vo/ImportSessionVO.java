package cn.sztu.questioncloud.web.rest.v1.importer.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ImportSessionVO {
    private Long importId;
    private Integer status;
    private Integer total;
    private Integer validCnt;
    private Integer invalidCnt;
    private BigDecimal progress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
