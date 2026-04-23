CREATE DATABASE IF NOT EXISTS demand_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE demand_db;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `email` VARCHAR(100) COMMENT '邮箱',
    `phone` VARCHAR(20) COMMENT '手机号',
    `real_name` VARCHAR(50) COMMENT '真实姓名',
    `avatar` VARCHAR(500) COMMENT '头像URL',
    `role` INT DEFAULT 0 COMMENT '角色: 0-只读用户, 1-普通用户, 2-管理员, 3-项目经理',
    `status` INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (`username`),
    INDEX idx_status (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 需求表
CREATE TABLE IF NOT EXISTS `demand` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `title` VARCHAR(200) NOT NULL COMMENT '需求标题',
    `description` TEXT COMMENT '需求描述',
    `type` INT DEFAULT 0 COMMENT '类型: 0-功能需求, 1-优化需求, 2-Bug修复',
    `priority` INT DEFAULT 1 COMMENT '优先级: 0-低, 1-中, 2-高, 3-紧急',
    `status` INT DEFAULT 0 COMMENT '状态: 0-待审批, 1-审批通过, 2-开发中, 3-测试中, 4-已完成, 5-已拒绝, 6-草稿',
    `proposer_id` BIGINT NOT NULL COMMENT '提出人ID',
    `assignee_id` BIGINT COMMENT '负责人ID',
    `approver_id` BIGINT COMMENT '审批人ID（项目经理）',
    `approve_time` DATETIME COMMENT '审批时间',
    `approve_comment` VARCHAR(1000) COMMENT '审批意见',
    `module` VARCHAR(100) COMMENT '所属模块',
    `expected_date` DATETIME COMMENT '期望完成时间',
    `actual_date` DATETIME COMMENT '实际完成时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (`status`),
    INDEX idx_proposer (`proposer_id`),
    INDEX idx_assignee (`assignee_id`),
    INDEX idx_approver (`approver_id`),
    INDEX idx_create_time (`create_time`)
    INDEX idx_demand_status_time (`status`, `create_time` DESC)
    INDEX idx_demand_proposer_time (`proposer_id`, `create_time` DESC)
    INDEX idx_demand_assignee_status (`assignee_id`, `status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求表';

-- 需求操作动态表
CREATE TABLE IF NOT EXISTS `demand_activity` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `demand_id` BIGINT NOT NULL COMMENT '需求ID',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) NOT NULL COMMENT '操作人姓名',
    `action_type` VARCHAR(30) NOT NULL COMMENT '动作类型: STATUS_CHANGE, ASSIGNEE_CHANGE, ATTACHMENT, COMMENT, CREATE',
    `content` VARCHAR(255) NOT NULL COMMENT '操作描述',
    `extra_data` JSON DEFAULT NULL COMMENT '扩展数据(如旧值/新值JSON)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_demand_id` (`demand_id`),
    KEY `idx_create_time` (`create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求操作动态/审计表';

-- 需求模板表
CREATE TABLE IF NOT EXISTS `demand_template` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name` VARCHAR(100) NOT NULL COMMENT '模板名称',
    `category` VARCHAR(50) NOT NULL COMMENT '模板分类：bug/feature/optimization/other',
    `content` TEXT NOT NULL COMMENT '模板内容',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求模板表';

-- 模块表
CREATE TABLE IF NOT EXISTS `module` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `name` VARCHAR(100) NOT NULL UNIQUE COMMENT '模块名称',
    `code` VARCHAR(50) NOT NULL UNIQUE COMMENT '模块编码',
    `description` VARCHAR(500) COMMENT '模块描述',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT `code`
    UNIQUE (`code`),
    CONSTRAINT `name`
    UNIQUE (`name`)
    INDEX idx_status (`status`),
    INDEX idx_sort (`sort`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块表';

-- 需求状态历史表
CREATE TABLE IF NOT EXISTS `demand_status_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `demand_id` BIGINT NOT NULL COMMENT '需求ID',
    `old_status` INT COMMENT '变更前状态',
    `new_status` INT NOT NULL COMMENT '变更后状态',
    `remark` VARCHAR(500) COMMENT '变更原因',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_demand_id (`demand_id`),
    INDEX idx_create_time (`create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求状态历史表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `demand_id` BIGINT NOT NULL COMMENT '需求ID',
    `user_id` BIGINT NOT NULL COMMENT '评论人ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parent_id` BIGINT COMMENT '父评论ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_demand_id (`demand_id`),
    INDEX idx_user_id (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 附件表
CREATE TABLE IF NOT EXISTS `attachment` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `demand_id` BIGINT NOT NULL COMMENT '需求ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
    `file_type` VARCHAR(50) COMMENT '文件类型',
    `file_size` BIGINT COMMENT '文件大小(字节)',
    `uploader_id` BIGINT NOT NULL COMMENT '上传者ID',
    `file_hash` VARCHAR(32) COMMENT '文件 MD5 哈希值',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    INDEX idx_demand_id (`demand_id`),
    INDEX idx_uploader (`uploader_id`),
    INDEX idx_file_hash (`file_hash`);
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '发送用户ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收人ID',
    `title` VARCHAR(200) NOT NULL COMMENT '标题',
    `content` TEXT COMMENT '内容',
    `type` INT DEFAULT 1 COMMENT '类型: 1-需求通知, 2-评论通知',
    `related_id` BIGINT COMMENT '相关ID',
    `is_read` INT DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_receiver (`receiver_id`),
    INDEX idx_user (`user_id`),
    INDEX idx_is_read (`is_read`),
    INDEX idx_create_time (`create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `user_id` BIGINT COMMENT '用户 ID',
    `username` VARCHAR(50) COMMENT '用户名',
    `operation` VARCHAR(200) COMMENT '操作描述',
    `method` VARCHAR(100) COMMENT '请求方法',
    `uri` VARCHAR(200) COMMENT '请求 URI',
    `params` TEXT COMMENT '请求参数',
    `ip` VARCHAR(50) COMMENT 'IP 地址',
    `status` INT COMMENT '响应状态',
    `error_msg` TEXT COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (`user_id`),
    INDEX idx_create_time (`create_time`)
    INDEX idx_log_uri_time (`uri`, `create_time`);
    INDEX idx_log_username_time (`username`, `create_time`);
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 数据字典表
CREATE TABLE IF NOT EXISTS `dict` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `type` VARCHAR(50) NOT NULL COMMENT '字典类型(demand_type/demand_priority/demand_status)',
    `code` INT NOT NULL COMMENT '字典码',
    `name` VARCHAR(100) NOT NULL COMMENT '字典名称',
    `sort` INT DEFAULT 0 COMMENT '排序',
    `status` INT DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_type_code (`type`, `code`),
    INDEX idx_type (`type`),
    INDEX idx_status (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- ==========================================
-- 初始化数据
-- ==========================================

-- 插入默认管理员账号 (密码: admin123)
INSERT INTO `user` (`username`, `password`, `real_name`, `role`) VALUES
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 2);

-- 插入默认模块
INSERT INTO `module` (`name`, `code`, `description`, `sort`, `status`)
VALUES
    ('用户管理', 'user', '用户相关的功能模块', 1, 1),
    ('需求管理', 'demand', '需求相关的功能模块', 2, 1),
    ('消息通知', 'notification', '消息通知相关的功能模块', 3, 1);

-- 初始化字典数据
INSERT INTO `dict` (`type`, `code`, `name`, `sort`, `status`, `remark`)
VALUES
    ('demand_type', 0, '功能需求', 1, 1, '新增功能的需求'),
    ('demand_type', 1, '优化需求', 2, 1, '现有功能优化'),
    ('demand_type', 2, 'Bug修复', 3, 1, '修复已知问题'),
    ('demand_priority', 0, '低', 1, 1, '低优先级'),
    ('demand_priority', 1, '中', 2, 1, '中优先级'),
    ('demand_priority', 2, '高', 3, 1, '高优先级'),
    ('demand_priority', 3, '紧急', 4, 1, '紧急处理'),
    ('demand_status', 0, '待审批', 1, 1, '等待项目经理审批'),
    ('demand_status', 1, '审批通过', 2, 1, '已审批通过'),
    ('demand_status', 2, '开发中', 3, 1, '正在开发'),
    ('demand_status', 3, '测试中', 4, 1, '正在测试'),
    ('demand_status', 4, '已完成', 5, 1, '已完成'),
    ('demand_status', 5, '已拒绝', 6, 1, '审批被拒绝'),
    ('demand_status', 6, '草稿', 0, 1, '未提交的草稿状态'),
    ('user_role', 0, '只读用户', 1, 1, '只能查看'),
    ('user_role', 1, '普通用户', 2, 1, '可创建需求'),
    ('user_role', 2, '管理员', 3, 1, '所有权限'),
    ('user_role', 3, '项目经理', 4, 1, '可审批和分配'),
    ('file_type', 0, '图片', 1, 1, 'jpg/png/gif等'),
    ('file_type', 1, '文档', 2, 1, 'doc/pdf等'),
    ('file_type', 2, '压缩包', 3, 1, 'zip/rar等'),
    ('file_type', 3, '其他', 4, 1, '其他类型');

-- 插入默认需求模板
INSERT INTO `demand_template` (`name`, `category`, `content`, `sort`) VALUES
    ('Bug 修复模板', 'bug', '## 问题描述\n[描述遇到的 Bug 现象]\n\n## 复现步骤\n1. \n2. \n3. \n\n## 期望结果\n[应该是什么样的]\n\n## 实际结果\n[现在是什么样的]\n\n## 环境信息\n- 浏览器：\n- 操作系统：', 1),
    ('新功能开发模板', 'feature', '## 功能概述\n[一句话描述功能]\n\n## 用户故事\n作为 [角色]，我希望 [功能]，以便 [价值]\n\n## 验收标准\n- [ ] \n- [ ] \n- [ ] \n\n## UI 要求\n[如果有设计稿或原型]\n\n## 接口依赖\n[需要哪些后端接口]', 2),
    ('性能优化模板', 'optimization', '## 优化目标\n[要优化的指标，如页面加载时间从 3s 降到 1s]\n\n## 当前瓶颈\n[分析出的性能瓶颈]\n\n## 优化方案\n1. \n2. \n\n## 预期收益\n[优化后的效果]', 3);
