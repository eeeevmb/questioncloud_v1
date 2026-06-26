package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.web.rest.v1.knowledge_point.req.KnowledgeImportConfirmReq;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.req.KnowledgeImportPreviewReq;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgeImportConfirmVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgeImportPreviewVO;

public interface KnowledgeImportService {
    /**
     * 预览目录导入后提取知识点的结果
     *
     * @param req 预览请求
     * @return 预览结果
     */
    public KnowledgeImportPreviewVO preview(KnowledgeImportPreviewReq req);

    /**
     * 确认导入知识点
     *
     * @param req    确认请求
     * @param userId 用户ID
     * @return 确认结果
     */
    KnowledgeImportConfirmVO confirm(KnowledgeImportConfirmReq req, Long userId);
}
