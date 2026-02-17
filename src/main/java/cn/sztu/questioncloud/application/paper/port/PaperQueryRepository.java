package cn.sztu.questioncloud.application.paper.port;

import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperQueryReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperBasicVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemDetailVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;

import java.math.BigDecimal;
import java.util.List;

public interface PaperQueryRepository {
    /**
     * 查询试卷信息 (不含题目列表)
     * 对应 PaperDetailVO 中的 items 为 null
     *
     * @param paperId 试卷ID
     * @return 查询结果  后续应该改成返回 PaperBasicVO
     */
    PaperDetailVO getBasicPaperById(Long paperId);

    /**
     * 分页搜索试卷列表 (只返回基础信息)
     */
    Pager<PaperBasicVO> searchPapers(PaperQueryReq req, Long userId);

    /**
     * 查询试卷关联的题目详情列表
     *
     * @param paperId 试卷ID
     * @return 查询结果
     */
    List<PaperItemDetailVO> listPaperItemsByPaperId(Long paperId);

    /**
     * 根据试卷ID查询题目总数
     *
     * @param paperId 试卷ID
     * @return 题目总数
     */
    Integer countItemsByPaperId(Long paperId);

    /**
     * 根据试卷ID查询试卷总分
     *
     * @param paperId 试卷ID
     * @return 试卷总分
     */
    BigDecimal sumScoreByPaperId(Long paperId);
}
