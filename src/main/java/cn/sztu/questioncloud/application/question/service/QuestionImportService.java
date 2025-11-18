package cn.sztu.questioncloud.application.question.service;

import cn.sztu.questioncloud.infrastructure.adapter.question.model.ImportSession;
import cn.sztu.questioncloud.web.rest.v1.question.req.ConfirmDraftReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.ParseResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * 题目批量导入服务
 */
public interface QuestionImportService {
    /**
     * 解析latex文件，提取题目信息
     *
     * @param latexFile tex文件
     * @return 解析结果视图
     */
    ParseResultVO parseLatex(MultipartFile latexFile);

    /**
     * 上传题目附件
     * @param assetFile 附件文件
     * @param slotId 位置ID
     * @param sessionId 会话ID
     */
    void uploadAsset(MultipartFile assetFile, String slotId, String sessionId);

    void confirmUpload(String sessionId, ConfirmDraftReq req);

    ImportSession getSession(String sessionId);
}
