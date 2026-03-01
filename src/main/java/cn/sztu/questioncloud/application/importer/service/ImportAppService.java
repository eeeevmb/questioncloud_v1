package cn.sztu.questioncloud.application.importer.service;

import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.web.rest.v1.importer.query.ImportItemPageQuery;
import cn.sztu.questioncloud.web.rest.v1.importer.req.ImportItemBatchUpdateReq;
import cn.sztu.questioncloud.web.rest.v1.importer.req.ImportSessionCreateReq;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportCreateVO;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportItemVO;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportSessionVO;

import java.util.List;

/**
 * 批量导入题目应用服务。
 * 负责导入会话创建、草稿分页与编辑等应用流程。
 *
 * @author Codex
 */
public interface ImportAppService {

    /**
     * 创建导入会话并异步启动解析任务。
     *
     * @param req          创建请求
     * @return 导入会话结果
     */
    ImportCreateVO createImportSession(ImportSessionCreateReq req);

    /**
     * 查询导入会话详情。
     *
     * @param importId 导入会话ID
     * @return 导入会话视图
     */
    ImportSessionVO getSession(Long importId);

    /**
     * 分页查询导入草稿。
     *
     * @param importId 导入会话ID
     * @param query    分页与状态筛选条件
     * @return 草稿分页结果
     */
    PageResult<ImportItemVO> pageItems(Long importId, ImportItemPageQuery query);

    /**
     * 批量编辑导入草稿。
     *
     * @param importId 导入会话ID
     * @param req      编辑请求
     * @return 更新后的草稿视图列表
     */
    List<ImportItemVO> updateItems(Long importId, ImportItemBatchUpdateReq req);

    /**
     * 提交题目草稿。
     *
     * @param importId           导入会话ID
     * @param userId             用户ID
     * @param ignoreInvalidDraft 是否忽略非法题目草稿
     */
    void commitImport(Long userId, Long importId, boolean ignoreInvalidDraft);

    /**
     * 取消批量导入
     *
     * @param userId   用户ID
     * @param importId 导入会话ID
     */
    void cancelImport(Long userId, Long importId);
}
