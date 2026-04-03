package cn.sztu.questioncloud.application.user.service;

import cn.sztu.questioncloud.application.user.dto.AvatarDTO;
import cn.sztu.questioncloud.web.rest.v1.user.req.SendRegisterCodeReq;
import cn.sztu.questioncloud.web.rest.v1.user.vo.LoginVO;
import cn.sztu.questioncloud.web.rest.v1.user.vo.RegisterVO;
import cn.sztu.questioncloud.web.rest.v1.user.vo.UserBasicInfoVO;
import cn.sztu.questioncloud.web.rest.v1.user.req.LoginReq;
import cn.sztu.questioncloud.web.rest.v1.user.req.RegisterReq;
import org.springframework.web.multipart.MultipartFile;

public interface UserAccountService {
    /**
     * 注册新用户
     * @return 注册响应
     */
    RegisterVO register(RegisterReq request);

    /**
     * 发送注册验证码
     *
     * @param request 发送注册验证码请求
     */
    void sendRegisterCode(SendRegisterCodeReq request);

    /**
     * 用户登录
     * @return 登录成功的用户信息
     */
    LoginVO login(LoginReq request);

    /**
     * 登出用户
     */
    void logout();

    /**
     * 上传头像
     * @return 头像存储路径
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 获取用户头像
     * @return 头像数据传输对象
     */
    AvatarDTO getAvatar(Long id);

    /**
     * 获取用户基本信息
     * @return 用户基础信息
     */
    UserBasicInfoVO getUserBasicInfo();
}
