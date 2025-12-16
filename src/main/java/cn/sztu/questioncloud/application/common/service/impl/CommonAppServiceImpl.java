package cn.sztu.questioncloud.application.common.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.common.port.FilePort;
import cn.sztu.questioncloud.application.common.service.CommonAppService;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import cn.sztu.questioncloud.infrastructure.common.file.exception.FileExceptionCodeEnum;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CommonAppServiceImpl implements CommonAppService {
    private final FilePort filePort;

    public CommonAppServiceImpl(FilePort filePort) {
        this.filePort = filePort;
    }

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 文件ID
     */
    @Override
    public Long uploadFile(MultipartFile file) {
        Long userId = StpUtil.getLoginIdAsLong();
        return filePort.uploadFile(file, userId).getFmId();
    }

    /**
     * 查看图片文件
     *
     * @param fileId 文件ID
     * @return 图片资源
     */
    @Override
    public Resource viewImageFile(Long fileId) {
        try {
            return filePort.getResource(fileId);
        } catch (InfrastructureException e) {
            String code = e.getCode();
            if (FileExceptionCodeEnum.FILE_NOT_FOUND.getCode().equals(code)) {
                throw new ApplicationException(
                        QuestionErrorCodeEnum.QUESTION_ASSET_NOT_FOUND,
                        "题目附件不存在或已被删除");
            }

            throw new ApplicationException(
                    CommonResultCodeEnum.FUTURE_ERROR,
                    "附件加载失败，请稍后再试");
        }
    }
}
