package cn.sztu.questioncloud.application.paper.service;

import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperItemSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemSaveVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemVO;

import java.util.List;

/**
 * 题目相关服务
 *
 * @author Saler1y
 */
public interface PaperItemService {
    /**
     * 存储试卷题目
     *
     * @param paperId 试卷ID
     * @param reqs 存储试卷题目请求
     */
    List<PaperItemSaveVO> savePaperItems(Long paperId,List<PaperItemSaveReq> reqs);

    /**
     * 删除试卷所有题目
     * 若要删除试卷题目，请调用此方法
     * 若要删除部分题目，请调用save方法间接删除
     *
     * @param paperId 试卷ID
     */
    void deleteItemsByPaperId(Long paperId);

    /**
     * 根据试卷ID获取试卷题目详情 列表
     *
     * @param paperId 试卷ID
     * @return 试卷题目列表
     */
    List<PaperItemVO> getItemsByPaperId(Long paperId);
}
