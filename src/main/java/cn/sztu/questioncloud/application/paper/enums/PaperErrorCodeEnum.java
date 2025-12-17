package cn.sztu.questioncloud.application.paper.enums;

import cn.sztu.questioncloud.common.constant.enums.result.ResultCodeEnum;
import lombok.Getter;

/**
 * 试卷模块错误码
 */
@Getter
public enum PaperErrorCodeEnum implements ResultCodeEnum {
    // ===== 试卷模块错误码 =====
    PAPER_NOT_FOUND("P00001", "试卷不存在"),
    PAPER_SAVE_FAILED("P00002", "保存试卷失败"),
    PAPER_TITLE_DUPLICATE("P00003", "试卷标题重复"),
    PAPER_STATUS_ERROR("P00004", "试卷状态错误");

    private final String code;
    private final String message;

    PaperErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
