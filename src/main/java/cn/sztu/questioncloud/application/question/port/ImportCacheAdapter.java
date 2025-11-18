package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.adapter.question.model.ImportSession;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDraftVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 上传缓存适配器
 */
public interface ImportCacheAdapter {
    /**
     * 创建上传会话
     *
     * @param userId 用户ID
     * @param drafts 题目草稿
     * @return 会话id
     */
    String createImportSession(Long userId, List<QuestionDraftVO> drafts);

    /**
     * 获取上传会话
     *
     * @param sessionId 会话ID
     * @return 会话
     */
    ImportSession getImportSession(String sessionId);

    /**
     * 上传附件并刷新会话
     *
     * @param session 会话
     * @param slotId 附件ID
     * @param asset 附件文件
     */
    void uploadAsset(ImportSession session, Long userId, String slotId, MultipartFile asset);
}
