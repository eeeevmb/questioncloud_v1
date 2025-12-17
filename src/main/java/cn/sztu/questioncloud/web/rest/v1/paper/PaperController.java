package cn.sztu.questioncloud.web.rest.v1.paper;

import cn.sztu.questioncloud.application.paper.service.PaperAppService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperBasicVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperCreatedVO;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 试卷相关接口
 *
 * @author Saler1y
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/paper")
public class PaperController {

    private final PaperAppService paperAppService;

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
    public ResultVO<PaperBasicVO> updatePaperInfo(@PathVariable("paperId") Long paperId,
                                                  @Valid @RequestBody PaperSaveReq req) {
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
}