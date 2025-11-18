DROP TABLE IF EXISTS user;
CREATE TABLE `user`
(
    `id` bigint NOT NULL COMMENT '用户ID',
    `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
    `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密后）',
    `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
    `phone_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '电话号码',
    `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像URL',
    `status` tinyint NOT NULL DEFAULT '1' COMMENT '账号状态（1=正常, 0=锁定）',
    `failed_login_count` int NOT NULL DEFAULT '0' COMMENT '失败登陆次数',
    `locked_until` datetime DEFAULT NULL COMMENT '账号解锁时间',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `username` (`username`) USING BTREE
);