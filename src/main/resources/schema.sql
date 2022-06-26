create database ecsheet;
CREATE TABLE `user_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_name` varchar(256) NOT NULL DEFAULT '' COMMENT '登录名',
  `password` varchar(128) NOT NULL DEFAULT '' COMMENT '密码',
  `nick_name` varchar(256) DEFAULT NULL COMMENT '昵称',
  `email` varchar(128) DEFAULT NULL COMMENT '邮箱',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '审核状态，0-未审核，1-审核通过',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_info_user_name_IDX` (`user_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `workbook` (
  `id` varchar(128) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'workbook逻辑主键',
  `name` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '名称',
  `options` json DEFAULT NULL COMMENT '配置数据',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='excle workbook';

CREATE TABLE `worksheet` (
  `id` varchar(128) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '逻辑主键',
  `workbook_id` varchar(128) COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT 'workbook逻辑主键',
  `data` json DEFAULT NULL COMMENT 'sheet数据',
  `delete_status` tinyint NOT NULL DEFAULT '0' COMMENT '删除标记,0是未删除，1是删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='work sheet';