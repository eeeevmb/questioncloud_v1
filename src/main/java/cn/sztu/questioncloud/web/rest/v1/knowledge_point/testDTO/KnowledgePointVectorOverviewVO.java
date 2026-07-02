package cn.sztu.questioncloud.web.rest.v1.knowledge_point.testDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointVectorOverviewVO {

    private Integer totalCount;

    private List<KnowledgePointVectorOverviewItemVO> latestTenItems;
}
