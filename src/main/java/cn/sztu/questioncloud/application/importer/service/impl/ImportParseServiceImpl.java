package cn.sztu.questioncloud.application.importer.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.sztu.questioncloud.application.common.port.FilePort;
import cn.sztu.questioncloud.application.importer.dto.ImportExcelRow;
import cn.sztu.questioncloud.application.importer.port.ImportItemRepository;
import cn.sztu.questioncloud.application.importer.port.ImportSessionRepository;
import cn.sztu.questioncloud.application.importer.service.CollectionAutoCreator;
import cn.sztu.questioncloud.application.importer.service.ImportExcelListener;
import cn.sztu.questioncloud.application.importer.service.ImportParseService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.file.config.FileStorageProperties;
import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.infrastructure.common.file.util.FileHandlingUtil;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.ImportSession;
import com.alibaba.excel.EasyExcel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 导入会话解析服务实现。
 *
 * @author Codex
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ImportParseServiceImpl implements ImportParseService {
    private final ImportSessionRepository importSessionRepository;
    private final ImportItemRepository importItemRepository;
    private final FilePort filePort;
    private final FileStorageProperties fileStorageProperties;
    private final CollectionAutoCreator collectionAutoCreator;

    @Override
    @Async("importTaskExecutor")
    public void parseAsync(Long sessionId) {
        ImportSession session = importSessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            return;
        }
        try {
            InfraFileMetadata metadata = filePort.getFileMetadata(session.getFileId());
            if (metadata == null) {
                throw new ApplicationException(CommonResultCodeEnum.NOT_FOUND, "导入文件不存在");
            }
            if (!StrUtil.equalsIgnoreCase("local", metadata.getFmProvider())) {
                throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "当前文件存储类型暂不支持导入");
            }
            Path basePath = FileHandlingUtil.getPhysicalBasePath(fileStorageProperties.getLocal().getBasePath(), false);
            Path filePath = basePath.resolve(metadata.getFmStoragePath());
            log.info("开始解析导入会话，sessionId={}，file={}", sessionId, filePath);
            ImportExcelListener listener = new ImportExcelListener(
                    importItemRepository,
                    collectionAutoCreator,
                    sessionId,
                    session.getUserId(),
                    200
            );
            try (InputStream is = Files.newInputStream(filePath)) {
                EasyExcel.read(is, ImportExcelRow.class, listener)
                        .autoCloseStream(true)
                        .sheet()
                        .doRead();
            }
            listener.flush();
            ImportSession patch = new ImportSession();
            patch.setId(sessionId);
            patch.setStatus(1);
            patch.setTotal(listener.getTotal());
            patch.setValidCnt(listener.getValid());
            patch.setInvalidCnt(listener.getInvalid());
            importSessionRepository.update(patch);
            log.info("导入会话解析完成，sessionId={}, total={}, valid={}, invalid={}",
                    sessionId, listener.getTotal(), listener.getValid(), listener.getInvalid());
        } catch (Exception ex) {
            log.error("解析导入会话失败，sessionId={}", sessionId, ex);
            ImportSession patch = new ImportSession();
            patch.setId(sessionId);
            patch.setStatus(5);
            importSessionRepository.update(patch);
        }
    }
}
