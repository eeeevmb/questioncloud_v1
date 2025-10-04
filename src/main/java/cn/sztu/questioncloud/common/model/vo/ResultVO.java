package cn.sztu.questioncloud.common.model.vo;

import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * @author LinSanQi
 */
@Getter
@Setter
public class ResultVO<T> {
    /**
     * 状态码
     */
    private String code;

    /**
     * 消息描述
     */
    private String message;

    /**
     * 数据，可以是任何类型的VO
     */
    private T data;

    public ResultVO() {
    }

    public ResultVO(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResultVO<T> success() {
        return success(null);
    }

    public static <T> ResultVO<T> success(T data) {
        return new ResultVO<>(CommonResultCodeEnum.SUCCESS.getCode(), CommonResultCodeEnum.SUCCESS.getMessage(), data);
    }

    public static <T> ResultVO<T> error(String code, String message) {
        return new ResultVO<>(code, message, null);
    }

    public static <T> ResultVO<T> error(String code, String message, T data) {
        return new ResultVO<>(code, message, data);
    }
}

