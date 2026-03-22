package cn.sztu.questioncloud.application.paper.service;

import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperPageQuery;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperBasicVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperCreatedVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperListItemVO;

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

    /**
     * 分页查询当前用户试卷列表
     *
     * @param query 分页与筛选参数
     * @return 试卷分页列表
     */
    PageResult<PaperListItemVO> pagePapers(PaperPageQuery query);
}
