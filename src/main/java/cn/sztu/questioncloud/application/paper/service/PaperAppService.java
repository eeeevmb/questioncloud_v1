package cn.sztu.questioncloud.application.paper.service;

import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperPageQuery;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperBasicVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperCreatedVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;

/**
 * 题目相关服务
 *
 * @author Saler1y
 */
public interface PaperAppService {
    /**
     * 创建试卷
     *
     * @param req 创建试卷请求
     * @return 试卷ID
     */
    PaperCreatedVO createPaper(PaperSaveReq req);


    /**
     * 复制试卷
     * 
     * @param paperId 试卷ID
     */
    // PaperCreatedVO copyPaper(Long paperId);


    /**
     * 分页查询试卷列表
     *
     * @param req 查询请求参数
     * @return 试卷视图列表
     */
    PageResult<PaperBasicVO> searchPapers(PaperPageQuery req);


    /**
     * 根据试卷ID查询题目详情
     *
     * @param paperId 试卷ID
     * @return 试卷详情视图
     */
    PaperDetailVO getPaperDetailById(Long paperId);


    /**
     * 硬删除 paper、paperItem
     *
     * @param paperId 试卷ID
     */
    void deletePaperById(Long paperId);


    /**
     * 修改草稿试卷信息(重命名、修改描述)
     *
     * @param paperId 试卷ID
     * @return 试卷详情视图
     */
    PaperBasicVO updatePaperInfo(Long paperId, PaperSaveReq req);
}