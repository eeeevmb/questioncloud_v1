package cn.sztu.questioncloud.web.rest.v1.question;

import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.UpdateQuestionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionCreatedVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 题目相关接口
 *
 * @author eeeevmb
 */
@RestController
@RequestMapping("/api/v1/question")
public class QuestionController {
    private final QuestionAppService questionAppService;

    public QuestionController(QuestionAppService questionAppService) {
        this.questionAppService = questionAppService;
    }

    /**
     * 创建单题
     * @return 题目ID
     */
    @PostMapping
    public ResultVO<QuestionCreatedVO> createQuestion(@Valid @RequestBody CreateQuestionReq req) {
        return ResultVO.success(questionAppService.createQuestion(req));
    }

    /**
     * 更新单题
     *
     * @param req 更新题目请求
     * @param questionId 题目ID
     * @return 无
     */
    @PutMapping("{questionId}")
    public ResultVO<Void> updateQuestion(@Valid @RequestBody UpdateQuestionReq req,
                                         @PathVariable Long questionId) {
        questionAppService.updateQuestionById(req, questionId);
        return ResultVO.success();
    }

    /**
     * 删除题目
     *
     * @param questionId 题目ID
     * @return 无
     */
    @DeleteMapping("{questionId}")
    public ResultVO<Void> deleteQuestion(@PathVariable Long questionId) {
        questionAppService.deleteQuestionById(questionId);
        return ResultVO.success();
    }

    /**
     * 查询单题详情
     * @return 题目详情视图
     */
    @GetMapping("{questionId}")
    public ResultVO<QuestionDetailVO> getQuestionDetail(@PathVariable Long questionId) {
        return ResultVO.success(questionAppService.getQuestionDetailById(questionId));
    }

}
