package cn.sztu.questioncloud.web.rest.v1.user.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.user.UserAccountEntity;

public record UserBasicInfoVO(
        Long userId,
        String username,
        String email,
        String phone,
        Integer status
) {
    public static UserBasicInfoVO fromEntity(UserAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserBasicInfoVO(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPhoneNumber(),
                entity.getStatus()
        );
    }
}
