package cn.sztu.questioncloud.web.rest.v1.user;


import cn.sztu.questioncloud.application.user.service.UserAccountService;
import cn.sztu.questioncloud.application.user.dto.AvatarDTO;
import cn.sztu.questioncloud.web.rest.v1.user.req.SendRegisterCodeReq;
import cn.sztu.questioncloud.web.rest.v1.user.vo.AvatarVO;
import cn.sztu.questioncloud.web.rest.v1.user.vo.LoginVO;
import cn.sztu.questioncloud.web.rest.v1.user.vo.RegisterVO;
import cn.sztu.questioncloud.web.rest.v1.user.vo.UserBasicInfoVO;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.user.req.LoginReq;
import cn.sztu.questioncloud.web.rest.v1.user.req.RegisterReq;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;

/**
 * 用户相关接口
 *
 * @author eeeevmb
 */

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserAccountService userAccountService;

    public UserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户id
     */
    @PostMapping("/register")
    public ResultVO<RegisterVO> registerUser(@Valid @RequestBody RegisterReq request) {
        return ResultVO.success(userAccountService.register(request));
    }

    /**
     * 发送注册验证码
     *
     * @param request 发送验证码请求
     * @return 无内容响应
     */
    @PostMapping("/send-register-code")
    public ResultVO<Void> sendRegisterCode(@Valid @RequestBody SendRegisterCodeReq request) {
        userAccountService.sendRegisterCode(request);
        return ResultVO.success();
    }


    /**
     * 用户登陆
     *
     * @param request 登陆请求
     * @return 登陆响应
     */
    @PostMapping("/login")
    public ResultVO<LoginVO> login(@Valid @RequestBody LoginReq request) {
        return ResultVO.success(userAccountService.login(request));
    }

    /**
     * 用户登出
     *
     * @return 退出登陆响应
     */
    @PostMapping("/logout")
    public ResultVO<Void> logout() {
        userAccountService.logout();
        return ResultVO.success();
    }

    /**
     * 头像上传
     *
     * @param file 头像文件
     * @return 头像存储相对路径
     */
    @PostMapping(value = "/avatar", consumes = {"multipart/form-data"})
    public ResultVO<AvatarVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String url = userAccountService.uploadAvatar(file);
        AvatarVO vo = new AvatarVO(url);
        return ResultVO.success(vo);
    }

    /**
     * 查看头像
     *
     * @param userId 用户Id
     * @return 头像数据传输对象
     */
    @GetMapping("/avatar/{userId}")
    public ResponseEntity<Resource> getAvatar(@PathVariable Long userId) {
        AvatarDTO avatar = userAccountService.getAvatar(userId);

        return ResponseEntity.ok()
                .contentType(avatar.mediaType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(avatar.resource());
    }

    /**
     * 获取用户基本信息
     *
     * @return 用户基本信息
     */
    @GetMapping("/basicInfo")
    public ResultVO<UserBasicInfoVO> getBasicInfo() {
        return ResultVO.success(userAccountService.getUserBasicInfo());
    }
}
