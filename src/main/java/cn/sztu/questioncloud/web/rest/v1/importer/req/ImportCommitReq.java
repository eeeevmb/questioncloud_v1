package cn.sztu.questioncloud.web.rest.v1.importer.req;

import lombok.Data;

@Data
public class ImportCommitReq {
    // true=允许存在不合法的题目提交，提交过程会自动忽略
    private Boolean ignoreInvalidDraft;
}
