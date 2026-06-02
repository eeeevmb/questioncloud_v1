package cn.sztu.questioncloud.application.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchFilter {
    private Long ownerId;

    private String docType;

    private String subject;

    private Long collectionId;

    private Double difficultyMin;

    private Double difficultyMax;

    private String typeCode;
}
