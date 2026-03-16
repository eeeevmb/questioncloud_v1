package cn.sztu.questioncloud.application.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchFilter {
    private Long ownerId;

    private List<Long> collectionIds;

    private Double difficultyMin;

    private Double difficultyMax;

    private String typeCode;
}
