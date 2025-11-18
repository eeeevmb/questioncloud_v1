package cn.sztu.questioncloud.web.rest.v1.question;

import cn.sztu.questioncloud.application.question.service.QuestionImportService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.infrastructure.adapter.question.model.ImportSession;
import cn.sztu.questioncloud.web.rest.v1.question.req.ConfirmDraftReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.ParseResultVO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 题目批量导入模块，实现 .tex 文件分段导入
 *
 * @author eeeevmb
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/question/import")
public class QuestionImportController {
    private final QuestionImportService questionImportService;

    public QuestionImportController(QuestionImportService questionImportService) {
        this.questionImportService = questionImportService;
    }

    /**
     * 解析上传的.tex文件
     *
     * @param latexFile latex文件
     * @return 解析结果响应
     */
    @PostMapping("/parse")
    ResultVO<ParseResultVO> parseLatex(@RequestParam("file") MultipartFile latexFile) {
        ParseResultVO parseResult = questionImportService.parseLatex(latexFile);
        return ResultVO.success(parseResult);
    }

    /**
     * 上传题目附件（图片等）
     * @param sessionId 会话ID
     * @param slotId 附件位置ID
     * @param assetFile 附件文件
     * @return 上传结果响应
     */
    @PostMapping("/{sessionId}/{slotId}")
    ResultVO<Void> uploadQuestionAsset(@PathVariable String sessionId,
                                       @PathVariable String slotId,
                                       @RequestParam("file") MultipartFile assetFile) {
        questionImportService.uploadAsset(assetFile, slotId, sessionId);
        return ResultVO.success();
    }

    /**
     * 确认上传草稿
     *
     * @param sessionId 会话ID
     * @param req 确认请求
     * @return 确认结果响应
     */
    @PostMapping("/{sessionId}")
    ResultVO<Void> confirmDrafts(@PathVariable String sessionId,
                                 @Valid @RequestBody ConfirmDraftReq req) {
        questionImportService.confirmUpload(sessionId, req);
        return ResultVO.success();
    }


    @GetMapping("/{sessionId}")//temptest
    ResultVO<ImportSession> getSession(@PathVariable String sessionId) {
        ImportSession session = questionImportService.getSession(sessionId);
        return ResultVO.success(session);
    };
}
