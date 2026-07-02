package cn.sztu.questioncloud.web.rest.v1.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgeImportService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.req.KnowledgeImportConfirmReq;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.req.KnowledgeImportPreviewReq;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgeImportConfirmVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgeImportPreviewVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/knowledge/import")
public class KnowledgeImportController {

    private final KnowledgeImportService knowledgeImportService;

    /*
    传入的json往往会因为过长，在传给LLM过程中被截断；
     */
    @PostMapping("/preview")
    public ResultVO<KnowledgeImportPreviewVO> preview(@Valid @RequestBody KnowledgeImportPreviewReq req) {
        return ResultVO.success(knowledgeImportService.preview(req));
    }

    @PostMapping("/confirm")
    public ResultVO<KnowledgeImportConfirmVO> confirm(@Valid @RequestBody KnowledgeImportConfirmReq req) {
        return ResultVO.success(knowledgeImportService.confirm(req, 0L));
    }
}
