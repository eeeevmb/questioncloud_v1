package cn.sztu.questioncloud.infrastructure.adapter.user;

import cn.sztu.questioncloud.application.user.port.UserPresenceCheckerPort;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.user.UserAccountEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.user.UserAccountMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Slf4j
@Component
public class UserPresenceCheckerAdapter implements UserPresenceCheckerPort {

    private final UserAccountMapper userAccountMapper;

    public UserPresenceCheckerAdapter(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public boolean existsByUsername(String username) {
        return QueryChain.of(userAccountMapper)
                .eq(UserAccountEntity::getUsername, username)
                .exists();
    }

    @Override
    public boolean existsByEmail(String email) {
        return QueryChain.of(userAccountMapper)
                .eq(UserAccountEntity::getEmail, email)
                .exists();
    }
}
