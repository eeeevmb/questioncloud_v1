package cn.sztu.questioncloud.application.question.port;

import org.springframework.web.multipart.MultipartFile;

public interface AssetCheckerPort {
    /**
     * 检查上传的题目附件是否为图片
     *
     * @param file 文件
     * @return 检查结果
     */
    boolean validateImage(MultipartFile file);

    /**
     * 检查上传的题目是否为文档文件（doc、docx、pdf、tex）
     *
     * @param file 文件
     * @return 检查结果
     */
    boolean validateDocument(MultipartFile file);
}
