package cn.sztu.questioncloud.infrastructure.common.file.util;



import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import cn.sztu.questioncloud.infrastructure.common.file.exception.FileExceptionCodeEnum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件处理工具类
 * 提供路径生成、解析、校验等功能
 *
 * @author LinSanQi
 */
public final class FileHandlingUtil {

    private FileHandlingUtil() {
    }

    /**
     * 根据所有者和日期策略生成存储路径。
     * 路径格式为：{ownerIdentifier}/{year}-{weekOfYear}
     * 例如，用户 "lin037" 在2025年第28周上传文件，路径为 "lin037/2025-28"。
     * 这种分级结构有助于管理文件系统，避免单个目录下文件过多。
     *
     * @param ownerIdentifier 文件所有者标识
     * @return 生成的相对路径，例如 "lin037/2025-28"
     */
    public static Path generateStoragePath(String ownerIdentifier) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        // 使用ISO 8601周定义，确保跨平台和区域设置的一致性
        int week = now.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        return Paths.get(ownerIdentifier, String.format("%d-%02d", year, week));
    }

    /**
     * 生成一个带扩展名的唯一文件名。
     * 使用 UUID 确保文件名在系统中的唯一性，同时保留原始文件的扩展名。
     * 例如："example.jpg" -> "xxxxxxxx-xxxx-xxxx-xxxx-XXXTentacion.jpg"
     *
     * @param originalFilename 原始文件名
     * @return 唯一文件名
     */
    public static String generateUniqueFilename(String originalFilename) {
        String extension = getFileExtension(originalFilename);
        return UUID.randomUUID() + "." + extension;
    }

    /**
     * 从完整文件名中获取文件扩展名。
     *
     * @param filename 文件名
     * @return 文件扩展名（小写），不带"."
     * @throws InfrastructureException 如果文件名无效（为null或不包含"."）
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new InfrastructureException(FileExceptionCodeEnum.INVALID_FILE_NAME);
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 获取并按需创建物理基础路径。
     * 该方法处理相对路径和绝对路径：
     * - 如果 `basePath` 是绝对路径，则直接使用。
     * - 如果是相对路径，则解析为相对于应用程序工作目录 (`user.dir`) 的路径。
     *
     * @param basePath          配置中的基础路径 (e.g., "uploads" or "/var/data/uploads")
     * @param createIfNotExists 如果路径不存在，是否自动创建所有必需的父目录
     * @return 代表物理基础路径的 Path 对象
     * @throws InfrastructureException 如果创建目录失败
     */
    public static Path getPhysicalBasePath(String basePath, boolean createIfNotExists) {
        Path physicalPath;
        Path path = Paths.get(basePath);

        if (path.isAbsolute()) {
            // 如果是绝对路径，直接使用
            physicalPath = path;
        } else {
            // 如果是相对路径，则基于当前应用工作目录进行解析
            String appHome = System.getProperty("user.dir");
            physicalPath = Paths.get(appHome, basePath);
        }

        if (createIfNotExists) {
            try {
                // 如果路径不存在，则创建目录（包括所有不存在的父目录）
                if (!Files.exists(physicalPath)) {
                    Files.createDirectories(physicalPath);
                }
            } catch (IOException e) {
                throw new InfrastructureException(FileExceptionCodeEnum.STORAGE_ERROR, "创建基础存储目录失败: " + physicalPath, e);
            }
        }
        return physicalPath;
    }
}
