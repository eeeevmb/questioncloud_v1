package cn.sztu.questioncloud.infrastructure.common.file.detector;

import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import cn.sztu.questioncloud.infrastructure.common.file.exception.FileExceptionCodeEnum;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文件类型检测器（基于魔数/文件头）
 *
 * @author LinSanQi
 */
@Component
public class FileTypeDetector {

    /**
     * 检测文件真实类型（基于魔数）
     * @param file 待检测文件
     * @return 文件类型：jpeg/png/pdf/docx/doc 等，未识别返回 null
     * @throws IOException 文件读取失败
     */
    public String detect(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(12);

            if (header.length == 0) {
                throw InfrastructureException.of(
                        FileExceptionCodeEnum.FILE_CONTENT_DAMAGED,
                        "文件内容为空"
                );
            }

            // JPEG: FF D8 FF
            if (startsWith(header, 0xFF, 0xD8, 0xFF)) {
                return "jpeg";
            }

            // PNG: 89 50 4E 47 0D 0A 1A 0A
            if (startsWith(header, 0x89, 0x50, 0x4E, 0x47)) {
                return "png";
            }

            // PDF: 25 50 44 46 (%PDF)
            if (startsWith(header, 0x25, 0x50, 0x44, 0x46)) {
                return "pdf";
            }

            // DOCX/XLSX/PPTX (ZIP 格式): 50 4B 03 04
            if (startsWith(header, 0x50, 0x4B, 0x03, 0x04)) {
                return "docx"; // 简化处理，实际需读取 ZIP 内容区分
            }

            // DOC (老式 Word): D0 CF 11 E0
            if (startsWith(header, 0xD0, 0xCF, 0x11, 0xE0)) {
                return "doc";
            }

            return null;
        }
    }

    private boolean startsWith(byte[] data, int... expected) {
        if (data.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((data[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }
}