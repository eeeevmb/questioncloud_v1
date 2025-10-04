package cn.sztu.questioncloud.infrastructure.common.file.exception;


import cn.sztu.questioncloud.infrastructure.common.exception.InfraExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件操作相关的结果代码枚举
 *
 * @author LinSanQi
 */
@Getter
@AllArgsConstructor
public enum FileExceptionCodeEnum implements InfraExceptionCode {
    FILE_NOT_FOUND("INFRA_FILE_NOT_FOUND", "文件未找到"),
    ACCESS_LEVEL_NOT_FOUND("INFRA_FILE_ACCESS_LEVEL_NOT_FOUND", "文件访问级别不存在"),
    ACCESS_DENIED("INFRA_FILE_ACCESS_DENIED", "文件访问权限不足"),
    QUOTA_EXCEEDED("INFRA_FILE_QUOTA_EXCEEDED", "文件上传配额已超出"),
    STORAGE_ERROR("INFRA_FILE_STORAGE_ERROR", "文件存储失败"),
    DOWNLOAD_ERROR("INFRA_FILE_DOWNLOAD_ERROR", "文件下载失败"),
    DELETE_ERROR("INFRA_FILE_DELETE_ERROR", "文件删除失败"),
    INVALID_PATH("INFRA_FILE_INVALID_PATH", "无效的文件路径"),
    OWNER_MISMATCH("INFRA_FILE_OWNER_MISMATCH", "文件所有者不匹配"),
    INVALID_FILE_NAME("INFRA_FILE_INVALID_NAME", "无效的文件名，文件名不存在拓展名"),
    AMBIGUOUS_TEMPLATE_FILE("INFRA_FILE_AMBIGUOUS_TEMPLATE", "找到多个同名但不同后缀的模板文件"),
    INVALID_TIME_RANGE("INFRA_FILE_INVALID_TIME_RANGE", "无效的时间范围");


    private final String code;
    private final String message;
} 