package cn.sztu.questioncloud.application.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.user.messaging.UserEventPublisher;
import cn.sztu.questioncloud.application.user.port.UserAccountRepository;
import cn.sztu.questioncloud.application.user.port.UserPresenceCheckerPort;
import cn.sztu.questioncloud.application.user.service.UserAccountService;
import cn.sztu.questioncloud.application.user.dto.AvatarDTO;
import cn.sztu.questioncloud.application.user.enums.UserStatusEnum;
import cn.sztu.questioncloud.web.rest.v1.user.vo.LoginVO;
import cn.sztu.questioncloud.web.rest.v1.user.vo.UserBasicInfoVO;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.util.PasswordEncryptionUtil;
import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import cn.sztu.questioncloud.infrastructure.common.file.validator.FileValidatorRegistry;
import cn.sztu.questioncloud.infrastructure.common.file.enums.AccessLevel;
import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.infrastructure.common.file.service.FileStorageService;
import cn.sztu.questioncloud.infrastructure.common.file.util.MediaTypeResolver;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.user.UserAccountEntity;
import cn.sztu.questioncloud.web.rest.v1.user.req.LoginReq;
import cn.sztu.questioncloud.web.rest.v1.user.req.RegisterReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Service
public class UserAccountServiceImpl implements UserAccountService {
    private final UserAccountRepository userAccountRepository;
    private final UserPresenceCheckerPort userPresenceCheckerPort;
    private final FileStorageService fileStorageService;
    private final FileValidatorRegistry fileValidatorRegistry;
    private final MediaTypeResolver mediaTypeResolver;
    private final UserEventPublisher userEventPublisher;

    public UserAccountServiceImpl(UserAccountRepository userAccountRepository, UserPresenceCheckerPort userPresenceCheckerPort, FileStorageService fileStorageService, FileValidatorRegistry fileValidatorRegistry, MediaTypeResolver mediaTypeResolver, UserEventPublisher userEventPublisher) {
        this.userAccountRepository = userAccountRepository;
        this.userPresenceCheckerPort = userPresenceCheckerPort;
        this.fileValidatorRegistry = fileValidatorRegistry;
        this.fileStorageService = fileStorageService;
        this.mediaTypeResolver = mediaTypeResolver;
        this.userEventPublisher = userEventPublisher;
    }

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return 用户id
     */
    @Override
    @Transactional
    public Long register(RegisterReq request) {
        if (userPresenceCheckerPort.existsByUsername(request.username())) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_VALIDATION_ERROR, "该用户名已被使用");
        }

        if (userPresenceCheckerPort.existsByEmail(request.email())) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_VALIDATION_ERROR, "该邮箱已被使用");
        }
        UserAccountEntity newUser = new UserAccountEntity();
        newUser.setUsername(request.username());
        newUser.setEmail(request.email());
        newUser.setPassword(PasswordEncryptionUtil.encrypt(request.password()));
        newUser.setStatus(UserStatusEnum.ACTIVE.getCode());

        userAccountRepository.save(newUser);

        userEventPublisher.publishUserRegistered(newUser.getId());

        return newUser.getId();
    }

    /**
     * 用户登陆
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
                .build();
    }

    /**
     * 用户登出
     */
    @Override
    public void logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout(userId);
    }

    /**
     * 用户上传头像
     * @param file 头像文件
     * @return 文件所在url
     */
    @Override
    public String uploadAvatar(MultipartFile file) {
        String userId = StpUtil.getLoginIdAsString();
        log.info("收到头像上传请求，用户ID: {}, 文件名: {}", userId, file.getOriginalFilename());

        try {
            fileValidatorRegistry.get("image").validate(file);
            InfraFileMetadata fileMetadata = fileStorageService.upload(file, userId, AccessLevel.PRIVATE);
            String url = fileMetadata.getFmStoragePath();
            userAccountRepository.updateAvatar(Long.parseLong(userId), url);
            log.info("头像上传成功：{}", url);

            return url;
        } catch (InfrastructureException e) {
            log.error("头像上传失败：code={}，message={}",e.getCode(), e.getMessage(), e);

            throw mapToApplicationException(e);
        }
    }

    /**
     * 获取用户头像
     * @param userId 用户id
     * @return 用户头像DTO
     */
    @Override
    public AvatarDTO getAvatar(Long userId) {
        log.info("获取头像资源：userId={}", userId);

        Optional<UserAccountEntity> userOpt = userAccountRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.info("用户不存在，返回默认头像：userId={}", userId);
            return getDefaultAvatar();
        }

        UserAccountEntity user = userOpt.get();
        String relativePath = user.getAvatarUrl();

        if (relativePath == null || relativePath.isEmpty()) {
            log.info("用户未设置头像，返回默认头像：userId={}", userId);
            return getDefaultAvatar();
        }

        Path filePath = fileStorageService.getAbsolutePath(relativePath);
        log.info("头像文件路径：{}", filePath);

        if (!Files.exists(filePath)) {
            log.warn("头像文件不存在：userId={}，path={}", userId, filePath);
            return getDefaultAvatar();
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.info("头像资源加载成功：userId={}", userId);
                MediaType type = mediaTypeResolver.resolve(resource.getFilename());
                return AvatarDTO.builder()
                        .resource(resource)
                        .mediaType(type)
                        .build();
            } else {
                log.warn("头像文件不可读：userId={}", userId);
                return getDefaultAvatar();
            }

        } catch (MalformedURLException e) {
            throw new ApplicationException(CommonResultCodeEnum.FUTURE_ERROR, "头像加载失败");
        }
    }

    /**
     * 获取用户基本信息
     * @return 用户基础信息
     */
    @Override
    public UserBasicInfoVO getUserBasicInfo() {
        Optional<UserAccountEntity> userOptional = userAccountRepository.findById(StpUtil.getLoginIdAsLong());
        UserAccountEntity user = userOptional.orElseThrow(() ->
                new ApplicationException(CommonResultCodeEnum.NOT_FOUND, "用户不存在"));

        return UserBasicInfoVO.fromEntity(user);
    }

    /**
     * 获取默认头像
     * @return 默认头像DTO
     */
    private AvatarDTO getDefaultAvatar() {
        Resource resource = new ClassPathResource("static/teacher.png");
        MediaType type = mediaTypeResolver.resolve(resource.getFilename());
        return AvatarDTO.builder()
                .resource(resource)
                .mediaType(type)
                .build();
    }

    private ApplicationException mapToApplicationException(InfrastructureException e) {
        return switch (e.getCode()) {
            case "INFRA_FILE_EMPTY" ->
                    new ApplicationException(CommonResultCodeEnum.PARAM_VALIDATION_ERROR, "文件不能为空");

            case "INFRA_FILE_EXTENSION_NOT_ALLOWED" ->
                    new ApplicationException(CommonResultCodeEnum.PARAM_VALIDATION_ERROR, "图片格式错误，仅支持 jpg/png/jpeg");

            case "INFRA_FILE_TYPE_INVALID" ->
                    new ApplicationException(CommonResultCodeEnum.PARAM_VALIDATION_ERROR, "无效的图片文件");

            case "INFRA_FILE_TYPE_MISMATCH" ->
                    new ApplicationException(CommonResultCodeEnum.PARAM_VALIDATION_ERROR, "文件内容与扩展名不符");

            case "INFRA_FILE_SIZE_EXCEEDED" ->
                    new ApplicationException(CommonResultCodeEnum.PARAM_VALIDATION_ERROR, "图片文件过大，最大支持 5MB");

            case "INFRA_FILE_READ_ERROR",
                 "INFRA_FILE_STORAGE_ERROR" ->
                    new ApplicationException(CommonResultCodeEnum.FUTURE_ERROR, "文件处理失败，请稍后重试");

            default -> new ApplicationException(CommonResultCodeEnum.FUTURE_ERROR, "头像上传失败");
        };
    }
}
