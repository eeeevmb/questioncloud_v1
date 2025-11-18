package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.AssetCheckerPort;
import cn.sztu.questioncloud.infrastructure.common.file.validator.FileValidatorRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AssetCheckerPortImpl implements AssetCheckerPort {
    private final FileValidatorRegistry fileValidatorRegistry;

    public AssetCheckerPortImpl(FileValidatorRegistry fileValidatorRegistry) {
        this.fileValidatorRegistry = fileValidatorRegistry;
    }

    /**
     * 检查上传的题目附件是否为图片
     *
     * @param file 文件
     * @return 检查结果
     */
    @Override
    public boolean validateImage(MultipartFile file) {
        return fileValidatorRegistry.get("image").validate(file);
    }
}
