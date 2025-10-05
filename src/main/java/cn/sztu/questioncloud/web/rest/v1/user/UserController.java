package cn.sztu.questioncloud.web.rest.v1.user;


import cn.sztu.questioncloud.application.user.service.UserAccountService;
import cn.sztu.questioncloud.application.user.dto.AvatarDTO;
import cn.sztu.questioncloud.application.user.vo.LoginVO;
import cn.sztu.questioncloud.application.user.vo.UserBasicInfoVO;
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
     */
    @PostMapping("/register")
    public ResultVO<Long> registerUser(@Valid @RequestBody RegisterReq request) {
        return ResultVO.success(userAccountService.register(request));
    }

    /**
     * 用户登陆
     */
    @PostMapping("/login")
    public ResultVO<LoginVO> login(@Valid @RequestBody LoginReq request) {
        return ResultVO.success(userAccountService.login(request));
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResultVO<Void> logout() {
        userAccountService.logout();
        return ResultVO.success();
    }

    /**
     * 头像上传
     */
    @PostMapping(value = "/upload-avatar", consumes = {"multipart/form-data"})
    public ResultVO<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        String url = userAccountService.uploadAvatar(file);

        return ResultVO.success(url);
    }

    /**
     * 查看头像
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
     */
    @GetMapping("/query/basic")
    public ResultVO<UserBasicInfoVO> getBasicInfo() {
        return ResultVO.success(userAccountService.getUserBasicInfo());
    }
}
