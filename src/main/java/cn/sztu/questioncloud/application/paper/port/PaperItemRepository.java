package cn.sztu.questioncloud.application.paper.port;

public interface PaperItemRepository {
    /**
     * 根据试卷ID删除试卷下所有试题关联
     *
     * @param paperId 试卷ID
     */
    void deleteByPaperId(Long paperId);
}
