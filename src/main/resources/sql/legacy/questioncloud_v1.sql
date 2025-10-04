/*
 Navicat Premium Dump SQL

 Source Server         : localMYSQL
 Source Server Type    : MySQL
 Source Server Version : 80404 (8.4.4)
 Source Host           : localhost:3306
 Source Schema         : questioncloud_demo_db

 Target Server Type    : MySQL
 Target Server Version : 80404 (8.4.4)
 File Encoding         : 65001

 Date: 04/10/2025 03:16:03
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for bank_question_mapping
-- ----------------------------
DROP TABLE IF EXISTS `bank_question_mapping`;
CREATE TABLE `bank_question_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '目前使用主键id为题目排序，后续考虑维护pos字段',
  `bank_id` bigint NOT NULL COMMENT '题库id',
  `question_id` bigint NOT NULL COMMENT '题目id',
  `difficulty` decimal(3,2) unsigned NOT NULL DEFAULT '0.50' COMMENT '题目难度',
  `exposure_factor` decimal(3,2) NOT NULL DEFAULT '1.00' COMMENT '曝光系数',
  `added_by` bigint NOT NULL COMMENT '添加者用户id',
  `added_at` datetime NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `bank_id` (`bank_id`,`question_id`) USING BTREE,
  KEY `question_id` (`question_id`),
  CONSTRAINT `bank_question_mapping_ibfk_1` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`),
  CONSTRAINT `bank_question_mapping_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for class
-- ----------------------------
DROP TABLE IF EXISTS `class`;
CREATE TABLE `class` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '班级名',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=ACTIVE,1=ARCHIVED',
  `code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邀请码',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '介绍',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `code` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for class_member
-- ----------------------------
DROP TABLE IF EXISTS `class_member`;
CREATE TABLE `class_member` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL COMMENT '班级id',
  `user_id` bigint NOT NULL COMMENT '班级中的用户id',
  `role` tinyint NOT NULL DEFAULT '0' COMMENT '0=STUDENT,1=TEACHER',
  `joined_at` datetime DEFAULT NULL,
  `last_active_at` datetime DEFAULT NULL,
  `left_at` datetime DEFAULT NULL,
  `is_left` tinyint(1) GENERATED ALWAYS AS ((case when (`left_at` is null) then 0 else 1 end)) VIRTUAL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `class_id` (`class_id`,`user_id`) USING BTREE,
  KEY `user_id` (`user_id`),
  CONSTRAINT `class_member_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `class_member_ibfk_2` FOREIGN KEY (`class_id`) REFERENCES `class` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for class_subject_mapping
-- ----------------------------
DROP TABLE IF EXISTS `class_subject_mapping`;
CREATE TABLE `class_subject_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `class_id` bigint NOT NULL,
  `subject_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `class_id` (`class_id`,`subject_id`) USING BTREE,
  KEY `subject_id` (`subject_id`) USING BTREE,
  CONSTRAINT `class_subject_mapping_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `class` (`id`),
  CONSTRAINT `class_subject_mapping_ibfk_2` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for knowledge_point
-- ----------------------------
DROP TABLE IF EXISTS `knowledge_point`;
CREATE TABLE `knowledge_point` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci,
  `source_type` tinyint NOT NULL DEFAULT '1' COMMENT '0=OTHERS,1=TEXTBOOK',
  `example` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '知识点示例题目（考虑关联questions表id还是写为text类型）',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for paper
-- ----------------------------
DROP TABLE IF EXISTS `paper`;
CREATE TABLE `paper` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '试卷名',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '备注',
  `duration_minutes` int unsigned NOT NULL COMMENT '考试时间',
  `total_score` decimal(6,2) unsigned NOT NULL DEFAULT '0.00' COMMENT '总分数',
  `total_items` int unsigned NOT NULL DEFAULT '0' COMMENT '总题数',
  `owner_id` bigint NOT NULL COMMENT '创建者id',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '试卷状态：0=草稿，1=发布中，2=已截止，3=已撤销',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `owner_id` (`owner_id`),
  CONSTRAINT `paper_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for paper_item
-- ----------------------------
DROP TABLE IF EXISTS `paper_item`;
CREATE TABLE `paper_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL COMMENT '试卷id',
  `question_id` bigint NOT NULL COMMENT '题目id',
  `seq_no` int unsigned NOT NULL COMMENT '题目顺序',
  `score` decimal(6,2) NOT NULL COMMENT '题目分数',
  `source_bank_id` bigint NOT NULL COMMENT '题目所在题库id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `paper_id` (`paper_id`,`seq_no`) USING BTREE,
  UNIQUE KEY `paper_id_2` (`paper_id`,`question_id`) USING BTREE,
  KEY `question_id` (`question_id`),
  KEY `source_bank_id` (`source_bank_id`),
  CONSTRAINT `paper_item_ibfk_1` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`),
  CONSTRAINT `paper_item_ibfk_2` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`),
  CONSTRAINT `paper_item_ibfk_3` FOREIGN KEY (`source_bank_id`) REFERENCES `question_bank` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for paper_item_snapshot
-- ----------------------------
DROP TABLE IF EXISTS `paper_item_snapshot`;
CREATE TABLE `paper_item_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `release_id` bigint NOT NULL COMMENT '发布实例id',
  `paper_id` bigint NOT NULL COMMENT '试卷id',
  `seq_no` int unsigned NOT NULL COMMENT '题号',
  `question_id` bigint NOT NULL COMMENT '原始题目id',
  `question_type` tinyint NOT NULL COMMENT '题目类型：0单选，1多选，2判断，3填空，4简答',
  `stem` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '题干',
  `options_json` json DEFAULT NULL COMMENT '选择题选项',
  `answer_json` json DEFAULT NULL,
  `analysis` text COLLATE utf8mb4_unicode_ci COMMENT '题目解析',
  `score` decimal(6,2) NOT NULL COMMENT '分数',
  `source_bank_id` bigint NOT NULL COMMENT '来源题库id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `release_id` (`release_id`,`seq_no`) USING BTREE,
  KEY `paper_id` (`paper_id`),
  KEY `question_id` (`question_id`),
  CONSTRAINT `paper_item_snapshot_ibfk_2` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`),
  CONSTRAINT `paper_item_snapshot_ibfk_4` FOREIGN KEY (`release_id`) REFERENCES `paper_release` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for paper_release
-- ----------------------------
DROP TABLE IF EXISTS `paper_release`;
CREATE TABLE `paper_release` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `paper_id` bigint NOT NULL COMMENT '关联试卷id',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '试卷标题',
  `start_at` datetime NOT NULL COMMENT '开始时间',
  `end_at` datetime NOT NULL COMMENT '结束时间',
  `time_limit_minutes` int unsigned NOT NULL COMMENT '限时（分钟）',
  `attempt_limit` int unsigned NOT NULL DEFAULT '1' COMMENT '可答卷次数（默认1）',
  `status` tinyint unsigned NOT NULL COMMENT '试卷状态：0=未开始 / 1=进行中 / 2=已结束 / 3=已撤销',
  `created_by` bigint NOT NULL COMMENT '发布者id',
  `created_at` datetime NOT NULL COMMENT '发布时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `paper_id` (`paper_id`),
  KEY `created_by` (`created_by`),
  KEY `idx_release_time` (`start_at`,`end_at`),
  CONSTRAINT `paper_release_ibfk_1` FOREIGN KEY (`paper_id`) REFERENCES `paper` (`id`),
  CONSTRAINT `paper_release_ibfk_2` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for permission
-- ----------------------------
DROP TABLE IF EXISTS `permission`;
CREATE TABLE `permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限代码',
  `description` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '权限描述',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1009 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for question
-- ----------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题目id',
  `created_by` bigint NOT NULL COMMENT '创建者用户id',
  `title` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '题目标题',
  `type` tinyint unsigned NOT NULL DEFAULT '5' COMMENT '题目类型：0单选，1多选，2判断，3填空，4简答，5其他',
  `stem` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '题干',
  `options_json` json DEFAULT NULL COMMENT '选项',
  `answer_json` json NOT NULL COMMENT '标准答案',
  `analysis` text COMMENT '答案解析',
  `difficulty` decimal(3,1) unsigned DEFAULT '5.0' COMMENT '题目难度',
  `exposure_factor` decimal(3,1) DEFAULT '1.0' COMMENT '曝光系数',
  `status` tinyint unsigned NOT NULL DEFAULT '0' COMMENT '题目状态：0草稿，1启用，2归档，3已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '题目创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '题目更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `created_by` (`created_by`),
  CONSTRAINT `question_ibfk_1` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=28 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for question_bank
-- ----------------------------
DROP TABLE IF EXISTS `question_bank`;
CREATE TABLE `question_bank` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `owner_user_id` bigint NOT NULL COMMENT '题库所有者id',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0=ACTIVE,1=INACTIVE',
  `visibility` tinyint NOT NULL DEFAULT '0' COMMENT '0=PRIVATE,1=PUBLIC,2=ORG',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `owner_user_id` (`owner_user_id`),
  CONSTRAINT `question_bank_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for question_knowledge_mapping
-- ----------------------------
DROP TABLE IF EXISTS `question_knowledge_mapping`;
CREATE TABLE `question_knowledge_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `knowledge_point_id` bigint NOT NULL,
  `relation_strength` float(3,2) NOT NULL COMMENT '关系强度',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id` DESC) USING BTREE,
  KEY `fk_questionKnowledgeMapping_knowledgePointIdint` (`knowledge_point_id`) USING BTREE,
  KEY `fk_questionKnowledgeMapping_questionId` (`question_id`) USING BTREE,
  CONSTRAINT `fk_questionKnowledgeMapping_knowledgePointIdint` FOREIGN KEY (`knowledge_point_id`) REFERENCES `knowledge_point` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_questionKnowledgeMapping_questionId` FOREIGN KEY (`question_id`) REFERENCES `question` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for role
-- ----------------------------
DROP TABLE IF EXISTS `role`;
CREATE TABLE `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色代码',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '角色描述',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `code` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for role_permission
-- ----------------------------
DROP TABLE IF EXISTS `role_permission`;
CREATE TABLE `role_permission` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `perm_id` bigint NOT NULL COMMENT '权限ID',
  PRIMARY KEY (`role_id`,`perm_id`) USING BTREE,
  KEY `perm_id` (`perm_id`) USING BTREE,
  CONSTRAINT `role_permission_ibfk_1` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `role_permission_ibfk_2` FOREIGN KEY (`perm_id`) REFERENCES `permission` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for subject
-- ----------------------------
DROP TABLE IF EXISTS `subject`;
CREATE TABLE `subject` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `code` varchar(255) NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id` DESC) USING BTREE,
  UNIQUE KEY `code` (`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for subject_knowledge_mapping
-- ----------------------------
DROP TABLE IF EXISTS `subject_knowledge_mapping`;
CREATE TABLE `subject_knowledge_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_id` bigint NOT NULL,
  `knowledge_point_id` bigint NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `fk_subjectKnowledgeMapping_knowledgePointId` (`knowledge_point_id`) USING BTREE,
  KEY `fk_subjectKnowledgeMapping_subjectId` (`subject_id`) USING BTREE,
  CONSTRAINT `fk_subjectKnowledgeMapping_knowledgePointId` FOREIGN KEY (`knowledge_point_id`) REFERENCES `knowledge_point` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_subjectKnowledgeMapping_subjectId` FOREIGN KEY (`subject_id`) REFERENCES `subject` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password_hash` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密后）',
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
) ENGINE=InnoDB AUTO_INCREMENT=124 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Table structure for user_role_mapping
-- ----------------------------
DROP TABLE IF EXISTS `user_role_mapping`;
CREATE TABLE `user_role_mapping` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `granted_by` bigint DEFAULT NULL COMMENT '授予者id，为NULL则是系统授予',
  `granted_at` datetime NOT NULL COMMENT '授予时间',
  `expires_at` datetime DEFAULT NULL COMMENT '到期时间',
  `is_active` tinyint NOT NULL DEFAULT '1' COMMENT '是否启用 1=启用 0=停用',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `user_id` (`user_id`,`role_id`) USING BTREE,
  KEY `role_id` (`role_id`) USING BTREE,
  KEY `user_role_ibfk_1` (`user_id`) USING BTREE,
  CONSTRAINT `user_role_mapping_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `user_role_mapping_ibfk_2` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
