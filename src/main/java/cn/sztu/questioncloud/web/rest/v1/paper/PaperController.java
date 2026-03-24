package cn.sztu.questioncloud.web.rest.v1.paper;

import cn.sztu.questioncloud.application.paper.service.PaperAppService;
import cn.sztu.questioncloud.application.paper.service.PaperItemService;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.paper.req.*;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.*;

import java.util.List;

import cn.xbatis.core.mybatis.mapper.context.Pager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 对试卷本体元操作相关接口
 *
 * @author Saler1y
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/paper")
public class PaperController {

    private final PaperAppService paperAppService;
    private final PaperItemService paperItemService;

    // === 自由组卷 ===

    /**
     * 创建空试卷
     *
     * @param req 创建请求参数
     * @return 试卷ID
     */
    @PostMapping
    public ResultVO<PaperCreatedVO> createPaper(@Valid @RequestBody PaperSaveReq req) {
        return ResultVO.success(paperAppService.createPaper(req));
    }

    /**
     * 全量保存试卷题目
     * 编辑页面点击"保存"时调用，会覆盖之前的题目列表(delete+save)
     *
     * @param paperId     试卷ID
     * @param batchReq    题目列表 (试题顺序由题目列表题目顺序决定)
     */
    @PostMapping("/{paperId}/items")
    public ResultVO<List<PaperItemVO>> savePaperItems(@Valid @RequestBody PaperItemBatchSaveReq batchReq,
                                                      @PathVariable Long paperId) {
        return ResultVO.success(paperItemService.savePaperItems(paperId, batchReq.getItems()));
    }

    /**
     * 新增试卷题目
     * 不覆盖原有题目列表，在原有题目列表末尾追加新题目
     *
     * @param paperId 试卷ID
     * @param batchReq 题目列表 (试题顺序由题目列表题目顺序决定)
     */
    @PostMapping("/{paperId}/items/add")
    public ResultVO<List<PaperItemVO>> addPaperItems(@Valid @RequestBody PaperItemBatchSaveReq batchReq,
                                                      @PathVariable Long paperId) {
        return ResultVO.success(paperItemService.addPaperItems(paperId, batchReq.getItems()));
    }

    // === 随机组卷 ===

    /**
     * 接口 1: 随机组卷 - 纯随机返回
     * (仅返回随机生成的题目列表，不保存到数据库，不覆盖原有试卷)
     */
    @PostMapping("/actions/random-preview")
    public ResultVO<List<PaperItemDetailVO>> previewRandomBuild(@RequestBody @Valid RandomBuildReq req) {
        return ResultVO.success(paperItemService.previewRandomItems(req));
    }

    /**
     * 接口 2: 随机换题
     */
    @PostMapping("/actions/replace-item")
    public ResultVO<PaperItemDetailVO> randomReplaceItem(@RequestBody @Valid RandomReplaceReq req) {
        return ResultVO.success(paperItemService.randomReplaceItem(req));
    }

    // =================== 公共接口 ===================

    /**
     * 分页搜索登录用户试卷列表
     *
     * @param query 查询请求参数
     * @return 试卷视图列表
     */
    @GetMapping
    public ResultVO<PageResult<PaperBasicVO>> searchPapers(@Valid PaperPageQuery query) {
        return ResultVO.success(paperAppService.searchPapers(query));
    }

    /**
     * 获取试卷详情 (包含题目列表)
     *
     * @param paperId 试卷ID
     * @return 试卷详情
     */
    @GetMapping("/{paperId}")
    public ResultVO<PaperDetailVO> getPaperDetail(@PathVariable Long paperId) {
        return ResultVO.success(paperAppService.getPaperDetailById(paperId));
    }

    /**
     * 修改试卷基础信息 (重命名、改描述)
     *
     * @param paperId 试卷ID
     * @param req     修改请求
     * @return 更新后的基础信息
     */
    @PutMapping("/{paperId}")
    public ResultVO<PaperBasicVO> updatePaperInfo(@Valid @RequestBody PaperSaveReq req,
                                                  @PathVariable Long paperId) {
        return ResultVO.success(paperAppService.updatePaperInfo(paperId, req));
    }

    /**
     * 硬删除试卷
     *
     * @param paperId 试卷ID
     * @return 成功状态
     */
    @DeleteMapping("/{paperId}")
    public ResultVO<Void> deletePaper(@PathVariable("paperId") Long paperId) {
        paperAppService.deletePaperById(paperId);
        return ResultVO.success();
    }

    /**
     * 清空试卷题目
     *
     * @param paperId 试卷ID
     */
    @DeleteMapping("/{paperId}/items")
    public ResultVO<Void> clearPaperItems(@PathVariable Long paperId) {
        paperItemService.deleteItemsByPaperId(paperId);
        return ResultVO.success();
    }
}