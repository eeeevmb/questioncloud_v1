package cn.sztu.questioncloud.web.rest.v1.importer;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.importer.service.ImportAppService;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.importer.query.ImportItemPageQuery;
import cn.sztu.questioncloud.web.rest.v1.importer.req.ImportCommitReq;
import cn.sztu.questioncloud.web.rest.v1.importer.req.ImportItemBatchUpdateReq;
import cn.sztu.questioncloud.web.rest.v1.importer.req.ImportSessionCreateReq;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportCreateVO;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportItemVO;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportSessionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {
    private final ImportAppService importAppService;

    public ImportController(ImportAppService importAppService) {
        this.importAppService = importAppService;
    }

    @PostMapping
    public ResultVO<ImportCreateVO> createImport(@Valid @RequestBody ImportSessionCreateReq req) {
        return ResultVO.success(importAppService.createImportSession(req));
    }

    @GetMapping("/{importId}")
    public ResultVO<ImportSessionVO> getImportSession(@PathVariable Long importId) {
        return ResultVO.success(importAppService.getSession(importId));
    }

    @GetMapping("/{importId}/items")
    public ResultVO<PageResult<ImportItemVO>> pageImportItems(@PathVariable Long importId,
                                                              @Valid ImportItemPageQuery query) {
        return ResultVO.success(importAppService.pageItems(importId, query));
    }

    @PutMapping("/{importId}/items")
    public ResultVO<List<ImportItemVO>> updateImportItems(@PathVariable Long importId,
                                                          @Valid @RequestBody ImportItemBatchUpdateReq req) {
        return ResultVO.success(importAppService.updateItems(importId, req));
    }

    @PostMapping("/{importId}/commit")
    public ResultVO<Void> commitImport(@PathVariable Long importId,
                                       @Valid @RequestBody(required = false) ImportCommitReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean ignore = req != null && Boolean.TRUE.equals(req.getIgnoreInvalidDraft());
        importAppService.commitImport(userId, importId, ignore);
        return ResultVO.success();
    }

    @PostMapping("/{importId}/cancel")
    public ResultVO<Void> cancelImport(@PathVariable Long importId) {
        Long userId = StpUtil.getLoginIdAsLong();
        importAppService.cancelImport(userId, importId);
        return ResultVO.success();
    }
}
