package cn.sztu.questioncloud.application.importer.service;

/**
 * 导入会话解析服务。
 *
 * @author Codex
 */
public interface ImportParseService {

    /**
     * 异步解析指定导入会话。
     *
     * @param sessionId 导入会话ID
     */
    void parseAsync(Long sessionId);
}
