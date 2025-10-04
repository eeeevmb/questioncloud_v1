package cn.sztu.questioncloud.application.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.user.port.UserAccountRepository;
import cn.sztu.questioncloud.application.user.port.UserPresenceCheckerPort;
import cn.sztu.questioncloud.application.user.service.UserAccountService;
import cn.sztu.questioncloud.application.user.vo.LoginVO;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.util.PasswordEncryptionUtil;
import cn.sztu.questioncloud.infrastructure.adapter.user.UserPresenceCheckerAdapter;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.user.UserAccountEntity;
import cn.sztu.questioncloud.web.rest.v1.user.req.LoginReq;
import cn.sztu.questioncloud.web.rest.v1.user.req.RegisterReq;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserAccountServiceImpl implements UserAccountService {
    private final UserAccountRepository userAccountRepository;
    private final UserPresenceCheckerPort userPresenceCheckerPort;

    public UserAccountServiceImpl(UserAccountRepository userAccountRepository, UserPresenceCheckerPort userPresenceCheckerPort) {
        this.userAccountRepository = userAccountRepository;
        this.userPresenceCheckerPort = userPresenceCheckerPort;
    }

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户id
     */
    @Override
    public Long register(RegisterReq request) {
        if (userPresenceCheckerPort.existsByUsername(request.username())) {
            throw new RuntimeException("测试" + request.username());
        }
        UserAccountEntity newUser = new UserAccountEntity();
        newUser.setUsername(request.username());
        newUser.setEmail(request.email());
        newUser.setPassword(PasswordEncryptionUtil.encrypt(request.password()));

        userAccountRepository.save(newUser);

        return newUser.getId();
    }

    /**
     * 用户注册
     *
     * @param request 登陆请求
     * @return 登陆响应
     */
    @Override
    public LoginVO login(LoginReq request) {
        Optional<UserAccountEntity> userOptional = userAccountRepository.findByAccount(request.account());

        UserAccountEntity user = userOptional.orElseThrow(() ->
                new ApplicationException(CommonResultCodeEnum.NOT_FOUND, "用户名或密码错误"));
        if (!PasswordEncryptionUtil.verify(request.password(), user.getPassword())) {
            throw new ApplicationException(CommonResultCodeEnum.NOT_FOUND, "用户名或密码错误");
        }

        StpUtil.login(user.getId());
        return LoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .tokenName(StpUtil.getTokenName())
                .tokenValue(StpUtil.getTokenValue())
                .build()    ;
    }

    @Override
    public void logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout(userId);
    }
}
