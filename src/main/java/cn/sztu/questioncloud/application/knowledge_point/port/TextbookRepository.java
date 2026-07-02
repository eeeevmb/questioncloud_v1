package cn.sztu.questioncloud.application.knowledge_point.port;

import cn.sztu.questioncloud.application.knowledge_point.dto.TextbookCheckDTO;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.TextbookEntity;

public interface TextbookRepository {
    /**
     * 根据教材查重条件获取已存在的重复教材实体
     *
     * @param query 包含教材信息的查询对象
     * @return 已存在的教材实体，不存在则返回 null
     */
    TextbookEntity getDuplicate(TextbookCheckDTO query);

    // === CRUD Operations ===
    TextbookEntity getById(Long id);

    void save(TextbookEntity entity);

    void update(TextbookEntity entity);

    void delete(Long id);

}
