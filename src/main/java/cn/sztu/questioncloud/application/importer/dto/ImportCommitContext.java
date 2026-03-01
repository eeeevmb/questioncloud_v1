package cn.sztu.questioncloud.application.importer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class ImportCommitContext {
    private Long userId;
    private Map<String, Long> collectionIdByName;
    /** collectionId -> nextOrdinal */
    private Map<Long, Integer> nextOrdinalByCollectionId;
    private LocalDateTime now;
}
