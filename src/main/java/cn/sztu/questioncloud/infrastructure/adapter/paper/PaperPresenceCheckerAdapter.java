package cn.sztu.questioncloud.infrastructure.adapter.paper;

import cn.sztu.questioncloud.application.paper.port.PaperPresenceCheckerRepository;
import cn.sztu.questioncloud.application.paper.port.PaperRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.paper.PaperMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaperPresenceCheckerAdapter implements PaperPresenceCheckerRepository {

    private final PaperMapper paperMapper;

    /**
     * 根据ID检查试卷是否存在
     *
     * @param title 试卷ID
     * @return 是否存在
     */
    @Override
    public boolean existsByTitle(String title, Long userId) {
        Integer count = QueryChain.of(paperMapper)
                .eq(PaperEntity::getTitle, title)
                .eq(PaperEntity::getOwnerId, userId)
                .count();
        return count != null && count > 0;
    }
}
