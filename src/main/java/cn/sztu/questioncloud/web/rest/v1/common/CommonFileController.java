package cn.sztu.questioncloud.web.rest.v1.common;

import cn.sztu.questioncloud.application.common.service.CommonAppService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.infrastructure.common.file.util.MediaTypeResolver;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/common")
public class CommonFileController {
    private final CommonAppService commonAppService;
    private final MediaTypeResolver mediaTypeResolver;

    public CommonFileController(CommonAppService commonAppService, MediaTypeResolver mediaTypeResolver) {
        this.commonAppService = commonAppService;
        this.mediaTypeResolver = mediaTypeResolver;
    }

    // TODO:文件VO
    @PostMapping(value = "/file", consumes = {"multipart/form-data"})
    public ResultVO<Long> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResultVO.success(commonAppService.uploadFile(file));
    }

    /**
     * 预览图片文件
     *
     * @param fileId 文件ID
     * @return 图片预览
     */
    @GetMapping("/file/{fileId}/view")
    public ResponseEntity<Resource> viewImage(@PathVariable Long fileId) {
        Resource resource = commonAppService.viewImageFile(fileId);
        MediaType mediaType = mediaTypeResolver.resolve(resource.getFilename());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(resource);
    }
}
