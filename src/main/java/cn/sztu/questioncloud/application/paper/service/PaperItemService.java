package cn.sztu.questioncloud.application.paper.service;

import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperItemSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.req.RandomBuildReq;
import cn.sztu.questioncloud.web.rest.v1.paper.req.RandomReplaceReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemDetailVO;

import java.util.List;

/**
 * 题目相关服务
 *
 * @author Saler1y
 */
public interface PaperItemService {
    // =================== 人工组卷 ===================
    /**
     * 存储试卷题目
     * 若要新增、更改部分题目，请调用此方法
     *
     * @param paperId 试卷ID
     * @param reqs 存储试卷题目请求
     */
    List<PaperItemVO> savePaperItems(Long paperId, List<PaperItemSaveReq> reqs);

    /**
     * 删除试卷所有题目
     * 若要删除试卷题目，请调用此方法
     * 若要删除部分题目，请调用save方法间接删除
     *
     * @param paperId 试卷ID
     */
    void deleteItemsByPaperId(Long paperId);

    /**
     * 根据试卷ID获取试卷题目详情列表
     *
     * @param paperId 试卷ID
     * @return 试卷题目列表
     */
    List<PaperItemDetailVO> getItemsByPaperId(Long paperId);


    // ===================== 随机组卷 ===================
    /**
     * 根据规则抽取题目
     * 仅返回题目列表，不存储试卷题目，供前端预览使用
     *
     * @param req 随机组卷请求
     * @return 试卷题目列表
     */
    List<PaperItemDetailVO> previewRandomItems(RandomBuildReq req);

    /**
     * 根据规则重新抽取一道题目
     *
     * @param req 随机组卷请求
     * @return 试卷题目列表
     */
    PaperItemDetailVO randomReplaceItem(RandomReplaceReq req);


    // ===================== 其他 ==================
    /*
      更改指定试题分数 (暂时废弃)

      param paperId 试卷ID
      param req 试题分数更改请求

      void updateItemScore(Long paperId, PaperItemSaveReq req);
     */
}
