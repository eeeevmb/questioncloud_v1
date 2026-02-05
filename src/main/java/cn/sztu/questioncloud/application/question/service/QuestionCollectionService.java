package cn.sztu.questioncloud.application.question.service;

import cn.sztu.questioncloud.web.rest.v1.question.req.CreateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.UpdateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.CollectionVO;

import java.util.List;

/**
 * 题集相关应用服务
 *
 * @author eeeevmb
 */
public interface QuestionCollectionService {

    // ===== 题集相关 =====
    /**
     * 创建默认题集
     * 用户注册后调用
     *
     * @param userId 用户id
     */
    void createDefaultCollection(Long userId);

    /**
     * 创建题集
     *
     * @param req 创建题集请求
     * @return 题集视图对象
     */
    CollectionVO createCollection(CreateCollectionReq req);

    /**
     * 更新题集
     *
     * @param req 更新题集请求
     * @return 题集视图对象
     */
    CollectionVO updateCollection(Long collectionId, UpdateCollectionReq req);

    /**
     * 删除题集
     *
     * @param collectionId 题集ID
     */
    void deleteCollection(Long collectionId);

    /**
     * 获取当前登录用户的题集
     *
     * @return 题集列表视图
     */
    List<CollectionVO> getCollections();
}
