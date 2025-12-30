package cn.sztu.questioncloud.application.paper.port;

import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;

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
}
