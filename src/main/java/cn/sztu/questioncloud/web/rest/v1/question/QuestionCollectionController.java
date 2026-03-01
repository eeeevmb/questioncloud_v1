package cn.sztu.questioncloud.web.rest.v1.question;

import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.application.question.service.QuestionCollectionService;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.question.req.BatchCreateCollectionsReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.QuestionInCollectionPageQuery;
import cn.sztu.questioncloud.web.rest.v1.question.req.UpdateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.CollectionVO;

import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionSummaryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 题集相关接口
 *
 * @author eeeevmb
 */
@RestController
@RequestMapping("/api/v1/collection")
public class QuestionCollectionController {
    private final QuestionCollectionService questionCollectionService;
    private final QuestionAppService questionAppService;

    public QuestionCollectionController(QuestionCollectionService questionCollectionService, QuestionAppService questionAppService) {
        this.questionCollectionService = questionCollectionService;
        this.questionAppService = questionAppService;
    }

    /**
     * 创建新题集
     *
     * @param req 创建题集请求
     * @return 题集视图响应
     */
    @PostMapping
    public ResultVO<CollectionVO> createCollection(@Valid @RequestBody CreateCollectionReq req) {
        CollectionVO vo = questionCollectionService.createCollection(req);
        return ResultVO.success(vo);
    }

    /**
     * 批量创建题集
     *
     * @param req 批量创建请求
     * @return 批量创建结果
     */
    @PostMapping("/batch")
    public ResultVO<List<CollectionVO>> batchCreateCollections(@Valid @RequestBody BatchCreateCollectionsReq req) {
        return ResultVO.success(questionCollectionService.createCollections(req.collections()));
    }

    /**
     * 更新题集
     *
     * @param collectionId 题集ID
     * @param req 更新题集请求
     * @return 题集视图响应
     */
    @PutMapping("/{collectionId}")
    public ResultVO<CollectionVO> updateCollection(@PathVariable Long collectionId,
                                            @Valid @RequestBody UpdateCollectionReq req) {
        CollectionVO vo = questionCollectionService.updateCollection(collectionId, req);
        return ResultVO.success(vo);
    }

    /**
     * 删除题集
     *
     * @param collectionId 题集ID
     * @return 删除响应
     */
    @DeleteMapping("/{collectionId}")
    public ResultVO<Void> deleteCollection(@PathVariable Long collectionId) {
        questionCollectionService.deleteCollection(collectionId);
        return ResultVO.success();
    }

    /**
     * 获取当前用户的所有题集
     *
     * @return 题集列表视图
     */
    @GetMapping
    public ResultVO<List<CollectionVO>> getCollections() {
        return ResultVO.success(questionCollectionService.getCollections());
    }

    /**
     * 分页查询题集内题目概要
     *
     * @param collectionId 题集ID
     * @param query        分页与筛选条件
     * @return 题目概要分页结果
     */
    @GetMapping("/{collectionId}/questions")
    public ResultVO<PageResult<QuestionSummaryVO>> pageCollectionQuestions(@PathVariable Long collectionId,
                                                                  @Valid QuestionInCollectionPageQuery query) {
        return ResultVO.success(questionAppService.getQuestionSummariesByCollectionId(collectionId, query));
    }
}
