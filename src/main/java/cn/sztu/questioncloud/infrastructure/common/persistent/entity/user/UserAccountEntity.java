package cn.sztu.questioncloud.infrastructure.common.persistent.entity.user;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("user")
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountEntity implements Serializable {
    @TableId
    private Long id;

    @NotNull
    private String username;

    @NotNull
    @TableField("password")
    private String password;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    private String avatarUrl;

    private Integer status; // 1-正常，0-锁定

    @NotNull
    private Integer failedLoginCount; // 连续登录失败次数

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private LocalDateTime updatedAt;

}
