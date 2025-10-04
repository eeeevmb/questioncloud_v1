package cn.sztu.questioncloud.application.user.service;

import cn.sztu.questioncloud.application.user.vo.LoginVO;
import cn.sztu.questioncloud.web.rest.v1.user.req.LoginReq;
import cn.sztu.questioncloud.web.rest.v1.user.req.RegisterReq;

public interface UserAccountService {
    /**
     * 注册新用户
     * @return 用户id
     */
    Long register(RegisterReq request);

    /**
     * 用户登录
     * @return 登录成功的用户信息
     */
    LoginVO login(LoginReq request);

    /**
     * 登出用户
     */
    void logout();
}
