package cn.sztu.questioncloud.application.paper.port;

import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperPageQuery;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperListItemVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;

import java.math.BigDecimal;
import java.util.Optional;

public interface PaperQueryRepository {
    /**
     * 根据试卷ID查询题目详情
     *
     * @param paperId 试卷ID
     * @return 查询结果
     */
    Optional<PaperDetailVO> getPaperDetailById(Long paperId);

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

    /**
     * 分页查询当前用户试卷列表
     *
     * @param ownerId 当前用户ID
     * @param query   分页与筛选参数
     * @return 分页结果
     */
    Pager<PaperListItemVO> pageByOwnerId(Long ownerId, PaperPageQuery query);
}
