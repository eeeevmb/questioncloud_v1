package cn.sztu.questioncloud.web.rest.v1.question;

import cn.sztu.questioncloud.application.question.service.QuestionCollectionService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.UpdateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.CollectionVO;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 题集相关接口
 *
 * @author eeeevmb
 */
@RestController
@RequestMapping("/api/v1/question/collection")
public class QuestionCollectionController {
    private final QuestionCollectionService questionCollectionService;

    public QuestionCollectionController(QuestionCollectionService questionCollectionService) {
        this.questionCollectionService = questionCollectionService;
    }

    /**
     * 创建新题集
     *
     * @param req 创建题集请求
     * @return 题集视图响应
     */
    @PostMapping
    ResultVO<CollectionVO> createCollection(@Valid @RequestBody CreateCollectionReq req) {
        CollectionVO vo = questionCollectionService.createCollection(req);
        return ResultVO.success(vo);
    }

    /**
     * 更新题集
     *
     * @param collectionId 题集ID
     * @param req 更新题集请求
     * @return 题集视图响应
     */
    @PutMapping("/{collectionId}")
    ResultVO<CollectionVO> updateCollection(@PathVariable Long collectionId,
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
    ResultVO<Void> deleteCollection(@PathVariable Long collectionId) {
        questionCollectionService.deleteCollection(collectionId);
        return ResultVO.success();
    }
}
