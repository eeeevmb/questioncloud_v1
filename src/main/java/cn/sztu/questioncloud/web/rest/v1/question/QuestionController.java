package cn.sztu.questioncloud.web.rest.v1.question;

import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
     * 创建题目（Latex文本导入）
     *
     * @param req 创建题目请求
     * @return 题目视图响应
     */
    @PostMapping("/latex")
    ResultVO<QuestionVO> createQuestion(@RequestBody CreateQuestionReq req) {
        QuestionVO vo = questionAppService.createQuestion(req);
        return ResultVO.success(vo);
    }


}
