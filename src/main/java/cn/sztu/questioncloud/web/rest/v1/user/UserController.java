package cn.sztu.questioncloud.web.rest.v1.user;


import cn.sztu.questioncloud.application.user.service.UserAccountService;
import cn.sztu.questioncloud.application.user.vo.LoginVO;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.user.req.LoginReq;
import cn.sztu.questioncloud.web.rest.v1.user.req.RegisterReq;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关接口
 */

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
}
