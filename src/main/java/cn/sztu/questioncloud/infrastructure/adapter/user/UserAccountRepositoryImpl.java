package cn.sztu.questioncloud.infrastructure.adapter.user;

import cn.sztu.questioncloud.application.user.port.UserAccountRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.user.UserAccountEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.user.UserAccountMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Repository
public class UserAccountRepositoryImpl implements UserAccountRepository {
    private final UserAccountMapper userAccountMapper;

    public UserAccountRepositoryImpl(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    /**
     * 根据用户名查找用户实体
     *
     * @param username 用户名
     * @return 用户实体的Optional封装
     */
    @Override
    public Optional<UserAccountEntity> findByUsername(String username) {
        UserAccountEntity user = QueryChain.of(userAccountMapper)
                .eq(UserAccountEntity::getUsername, username)
                .limit(1)
                .get();
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<UserAccountEntity> findById(Long id) {
        UserAccountEntity user = QueryChain.of(userAccountMapper)
                .eq(UserAccountEntity::getId, id)
                .limit(1)
                .get();
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<UserAccountEntity> findByEmail(String email) {
        UserAccountEntity user = QueryChain.of(userAccountMapper)
                .eq(UserAccountEntity::getEmail, email)
                .limit(1)
                .get();
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<UserAccountEntity> findByAccount(String account) {
        UserAccountEntity user = QueryChain.of(userAccountMapper)
                .eq(UserAccountEntity::getUsername, account)
                .or().eq(UserAccountEntity::getEmail, account)
                .limit(1)
                .get();
        return Optional.ofNullable(user);
    }

    @Override
    public void save(UserAccountEntity userAccount) {
        if (userAccount.getId() == null) {
            userAccount.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        userAccountMapper.save(userAccount);
    }

    @Override
    public void updateAvatar(Long userId, String url) {
        UserAccountEntity user = QueryChain.of(userAccountMapper)
                .eq(UserAccountEntity::getId, userId)
                .limit(1)
                .get();
        user.setAvatarUrl(url);
        userAccountMapper.update(user);
    }
}
