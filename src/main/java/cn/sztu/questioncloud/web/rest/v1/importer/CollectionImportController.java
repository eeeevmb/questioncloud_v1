package cn.sztu.questioncloud.web.rest.v1.importer;

import cn.sztu.questioncloud.application.importer.service.ImportAppService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.importer.req.ImportSessionCreateReq;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportCreateVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/question/imports")
public class CollectionImportController {
    private final ImportAppService importAppService;

    public CollectionImportController(ImportAppService importAppService) {
        this.importAppService = importAppService;
    }

    @PostMapping
    public ResultVO<ImportCreateVO> createImport(@Valid @RequestBody ImportSessionCreateReq req) {
        return ResultVO.success(importAppService.createImportSession(req));
    }
}
