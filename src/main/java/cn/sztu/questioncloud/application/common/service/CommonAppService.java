package cn.sztu.questioncloud.application.common.service;

import cn.sztu.questioncloud.web.rest.v1.common.vo.FileUploadVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface CommonAppService {
    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件ID
     */
    FileUploadVO uploadFile(MultipartFile file);

    /**
     * 查看图片文件
     *
     * @param fileId 文件ID
     * @return 图片资源
     */
    Resource viewImageFile(Long fileId);
}
