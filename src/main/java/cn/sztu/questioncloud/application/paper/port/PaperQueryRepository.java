package cn.sztu.questioncloud.application.paper.port;

import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;

import java.util.Optional;

public interface PaperQueryRepository {
    /**
     * 根据试卷ID查询题目详情
     *
     * @param paperId 试卷ID
     * @return 查询结果
     */
    Optional<PaperDetailVO> getPaperDetailById(Long paperId);

}
