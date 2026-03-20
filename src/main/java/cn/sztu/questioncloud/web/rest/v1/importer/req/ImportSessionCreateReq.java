package cn.sztu.questioncloud.web.rest.v1.importer.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ImportSessionCreateReq {
    @NotNull(message = "fileId 不能为空")
    private Long fileId;

    /**
     * 目前仅支持 excel
     */
    @NotNull(message = "format 不能为空")
    private String format;
}
