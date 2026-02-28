package cn.sztu.questioncloud.web.rest.v1.importer;

import cn.sztu.questioncloud.application.importer.service.ImportAppService;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.importer.query.ImportItemPageQuery;
import cn.sztu.questioncloud.web.rest.v1.importer.req.ImportItemBatchUpdateReq;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportItemVO;
import cn.sztu.questioncloud.web.rest.v1.importer.vo.ImportSessionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    private final ImportAppService importAppService;

    public ImportController(ImportAppService importAppService) {
        this.importAppService = importAppService;
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
}
