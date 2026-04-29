CREATE DATABASE IF NOT EXISTS demand_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE demand_db;

-- 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名/工号',
    password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    real_name VARCHAR(50) COMMENT '真实姓名',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(500) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '账号状态(1:正常 0:禁用)',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    delete_time DATETIME DEFAULT NULL COMMENT '软删除标记'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

DROP TABLE IF EXISTS sys_role;
CREATE TABLE IF NOT EXISTS sys_role
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_key    VARCHAR(50) NOT NULL UNIQUE COMMENT '角色标识(如: SUPER_ADMIN)',
    description VARCHAR(255) COMMENT '角色描述',
    is_system   TINYINT DEFAULT 1 COMMENT '是否系统内置(1:是 0:否)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT ='系统角色表';

DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
   user_id BIGINT NOT NULL COMMENT '用户ID',
   role_id BIGINT NOT NULL COMMENT '角色ID',
   PRIMARY KEY (user_id, role_id),
   INDEX idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关系表';

DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
   name VARCHAR(50) NOT NULL COMMENT '权限名称',
   type TINYINT NOT NULL COMMENT '类型(1:目录 2:菜单 3:按钮/接口)',
   path VARCHAR(200) COMMENT '路由路径或接口URL',
   perms VARCHAR(100) COMMENT '权限标识(如: demand:add)',
   icon VARCHAR(100) COMMENT '图标',
   sort INT DEFAULT 0 COMMENT '排序',
   create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
   role_id BIGINT NOT NULL COMMENT '角色ID',
   permission_id BIGINT NOT NULL COMMENT '权限ID',
   PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限表';

DROP TABLE IF EXISTS sys_project;
CREATE TABLE sys_project
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL COMMENT '项目名称',
    code        VARCHAR(50) UNIQUE COMMENT '项目编码',
    description VARCHAR(500) COMMENT '项目描述',
    owner_id    BIGINT COMMENT '项目负责人',
    visibility  TINYINT  DEFAULT 1 COMMENT '可见性(1:公开 2:私有)',
    status      TINYINT  DEFAULT 1 COMMENT '状态(1:进行中 0:已归档)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统项目表';

DROP TABLE IF EXISTS sys_project_member;
CREATE TABLE sys_project_member
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL COMMENT '项目ID',
    user_id    BIGINT NOT NULL COMMENT '成员ID',
    role_code  VARCHAR(50) COMMENT '项目内角色(如: DEVELOPER)',
    join_time  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员表';

DROP TABLE IF EXISTS sys_iteration;
CREATE TABLE sys_iteration
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL COMMENT '项目ID',
    name        VARCHAR(100) NOT NULL COMMENT '迭代名称(如: V1.0 Sprint1)',
    start_time  DATETIME COMMENT '开始时间',
    end_time    DATETIME COMMENT '结束时间',
    status      TINYINT  DEFAULT 0 COMMENT '状态(0:未开始 1:进行中 2:已结束)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目迭代表';

DROP TABLE IF EXISTS project_module;
CREATE TABLE IF NOT EXISTS `project_module`
(
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `project_id`  BIGINT       NOT NULL COMMENT '所属项目ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '模块名称',
    `code`        VARCHAR(50)  NOT NULL COMMENT '模块编码',
    `description` VARCHAR(500) COMMENT '模块描述',
    `parent_id`   BIGINT   DEFAULT 0 COMMENT '父模块ID（支持层级结构）',
    `sort`        INT      DEFAULT 0 COMMENT '排序',
    `status`      INT      DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_project_code (`project_id`, `code`),
    INDEX idx_project_id (`project_id`),
    INDEX idx_parent_id (`parent_id`),
    INDEX idx_status (`status`),
    INDEX idx_sort (`sort`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='项目模块表';

DROP TABLE IF EXISTS demand;
CREATE TABLE demand
(
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '需求ID',
    -- ========== 基础信息 ==========
    title               VARCHAR(200) NOT NULL COMMENT '需求标题',
    description         LONGTEXT COMMENT '需求详细描述',
    type                VARCHAR(50) DEFAULT 'FEATURE' COMMENT '需求类型: FEATURE-功能需求, BUG-Bug修复, OPTIMIZATION-优化需求, TASK-任务',
    priority            VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '优先级: URGENT-紧急, HIGH-高, MEDIUM-中, LOW-低',
    status              VARCHAR(30) DEFAULT 'DRAFT' COMMENT '状态: DRAFT-草稿, PENDING_REVIEW-待审批, APPROVED-审批通过, REJECTED-已拒绝, PLANNED-已规划, IN_DEVELOPMENT-开发中, IN_TEST-测试中, ACCEPTED-已验收, COMPLETED-已完成, CANCELLED-已取消',
    -- ========== 关联关系 ==========
    project_id          BIGINT       NOT NULL COMMENT '所属项目ID',
    module_id           BIGINT COMMENT '所属模块ID',
    iteration_id        BIGINT COMMENT '所属迭代ID',
    parent_id           BIGINT      DEFAULT 0 COMMENT '父需求ID（支持层级结构/子需求）',
    creator_id          BIGINT       NOT NULL COMMENT '提出人ID',
    assignee_id         BIGINT COMMENT '当前负责人ID',
    reviewer_id         BIGINT COMMENT '评审人/验收人ID',
    approver_id         BIGINT COMMENT '审批人ID',
    -- ========== 工作量管理 ==========
    estimated_hours     DECIMAL(10, 2) COMMENT '预估工时（小时）',
    actual_hours        DECIMAL(10, 2) COMMENT '实际工时（小时）',
    story_points        INT COMMENT '故事点（敏捷开发）',
    -- ========== 时间管理 ==========
    expected_start_date DATE COMMENT '期望开始日期',
    expected_end_date   DATE COMMENT '期望完成日期',
    actual_start_date   DATE COMMENT '实际开始日期',
    actual_end_date     DATE COMMENT '实际完成日期',
    approve_time        DATETIME COMMENT '审批时间',
    start_develop_time  DATETIME COMMENT '开始开发时间',
    start_test_time     DATETIME COMMENT '开始测试时间',
    complete_time       DATETIME COMMENT '完成时间',
    -- ========== 审批与验收 ==========
    approve_comment     VARCHAR(1000) COMMENT '审批意见',
    reject_reason       VARCHAR(1000) COMMENT '驳回原因',
    acceptance_criteria TEXT COMMENT '验收标准',
    acceptance_result   VARCHAR(500) COMMENT '验收结果',
    -- ========== 扩展字段 ==========
    version             VARCHAR(50) COMMENT '版本号',
    source              VARCHAR(100) COMMENT '需求来源（用户反馈/市场调研/内部优化等）',
    business_value      TEXT COMMENT '业务价值说明',
    technical_solution  TEXT COMMENT '技术方案概要',
    risk_description    TEXT COMMENT '风险说明',
    -- ========== 审计字段 ==========
    create_time         DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time         DATETIME    DEFAULT NULL COMMENT '删除时间（软删除）',
    -- ========== 索引优化 ==========
    INDEX idx_project_status (project_id, status),
    INDEX idx_module (module_id),
    INDEX idx_iteration (iteration_id),
    INDEX idx_creator (creator_id),
    INDEX idx_assignee (assignee_id),
    INDEX idx_reviewer (reviewer_id),
    INDEX idx_approver (approver_id),
    INDEX idx_parent (parent_id),
    INDEX idx_type (type),
    INDEX idx_priority (priority),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time DESC),
    INDEX idx_expected_end_date (expected_end_date),
    INDEX idx_delete_time (delete_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='需求表';

DROP TABLE IF EXISTS demand_tag;
CREATE TABLE demand_tag
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '标签ID',
    name        VARCHAR(50) NOT NULL UNIQUE COMMENT '标签名称',
    color       VARCHAR(20) DEFAULT '#409EFF' COMMENT '标签颜色（十六进制）',
    category    VARCHAR(50) DEFAULT 'TECHNICAL' COMMENT '标签分类: TECHNICAL-技术, BUSINESS-业务, PRIORITY-优先级, OTHER-其他',
    use_count   INT         DEFAULT 0 COMMENT '使用次数',
    is_system   TINYINT     DEFAULT 0 COMMENT '是否系统内置: 1-是 0-否',
    create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_category (category),
    INDEX idx_use_count (use_count DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='需求标签表';

DROP TABLE IF EXISTS demand_tag_relation;
CREATE TABLE demand_tag_relation
(
    demand_id   BIGINT NOT NULL COMMENT '需求ID',
    tag_id      BIGINT NOT NULL COMMENT '标签ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联时间',
    PRIMARY KEY (demand_id, tag_id),
    INDEX idx_tag_id (tag_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='需求标签关联表';

DROP TABLE IF EXISTS demand_dependency;
CREATE TABLE demand_dependency
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '依赖ID',
    demand_id       BIGINT NOT NULL COMMENT '当前需求ID',
    depends_on_id   BIGINT NOT NULL COMMENT '依赖的需求ID',
    dependency_type VARCHAR(50) DEFAULT 'BLOCKS' COMMENT '依赖类型: BLOCKS-阻塞, RELATED-相关, DUPLICATE-重复',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_dependency (demand_id, depends_on_id),
    INDEX idx_depends_on (depends_on_id),
    INDEX idx_demand_id (demand_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='需求依赖关系表';

DROP TABLE IF EXISTS comment_like;
CREATE TABLE comment_like
(
    comment_id  BIGINT NOT NULL COMMENT '评论ID',
    user_id     BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (comment_id, user_id),
    INDEX idx_user_id (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='评论点赞表';

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

-- 需求状态历史表
CREATE TABLE IF NOT EXISTS `demand_status_history` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `demand_id` BIGINT NOT NULL COMMENT '需求ID',
    `old_status` VARCHAR(30) COMMENT '变更前状态',
    `new_status` VARCHAR(30) NOT NULL COMMENT '变更后状态',
    `remark` VARCHAR(500) COMMENT '变更原因',
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_demand_id (`demand_id`),
    INDEX idx_create_time (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求状态历史表';

-- 需求版本表
DROP TABLE IF EXISTS demand_version;
CREATE TABLE demand_version
(
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    demand_id           BIGINT       NOT NULL COMMENT '需求ID',
    version_number      INT          NOT NULL COMMENT '版本号（从1开始递增）',
    title               VARCHAR(200) NOT NULL COMMENT '需求标题快照',
    description         TEXT COMMENT '需求描述快照',
    type                VARCHAR(50) COMMENT '需求类型快照',
    priority            VARCHAR(50) COMMENT '优先级快照',
    status              VARCHAR(50) COMMENT '状态快照',
    module_id           BIGINT COMMENT '模块ID快照',
    iteration_id        BIGINT COMMENT '迭代ID快照',
    estimated_hours     DECIMAL(10, 2) COMMENT '预估工时快照',
    story_points        INT COMMENT '故事点快照',
    expected_start_date DATE COMMENT '期望开始日期快照',
    expected_end_date   DATE COMMENT '期望完成日期快照',
    acceptance_criteria TEXT COMMENT '验收标准快照',
    snapshot_data       JSON COMMENT '完整快照数据（JSON格式）',
    change_summary      VARCHAR(500) COMMENT '变更说明',
    operator_id         BIGINT COMMENT '操作人ID',
    operator_name       VARCHAR(100) COMMENT '操作人姓名',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_demand_id (demand_id),
    INDEX idx_version_number (demand_id, version_number),
    FOREIGN KEY (demand_id) REFERENCES demand (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='需求版本表';

DROP TABLE IF EXISTS demand_quality_score;
CREATE TABLE demand_quality_score
(
    id                        BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    demand_id                 BIGINT      NOT NULL COMMENT '需求ID',
    total_score               INT         NOT NULL COMMENT '总分（0-100）',
    rating                    VARCHAR(20) NOT NULL COMMENT '评级：EXCELLENT/GOOD/FAIR/POOR',
    required_fields_score     INT        DEFAULT 0 COMMENT '必填字段得分（0-30）',
    description_score         INT        DEFAULT 0 COMMENT '描述质量得分（0-25）',
    acceptance_criteria_score INT        DEFAULT 0 COMMENT '验收标准得分（0-20）',
    technical_details_score   INT        DEFAULT 0 COMMENT '技术细节得分（0-15）',
    business_value_score      INT        DEFAULT 0 COMMENT '业务价值得分（0-10）',
    score_details             JSON COMMENT '评分详情',
    suggestions               JSON COMMENT '建议改进项',
    score_time                DATETIME   DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
    notified                  TINYINT(1) DEFAULT 0 COMMENT '是否已提醒',

    UNIQUE INDEX idx_demand_id (demand_id),
    FOREIGN KEY (demand_id) REFERENCES demand (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='需求质量评分表';

DROP TABLE IF EXISTS demand_document;
CREATE TABLE demand_document
(
    id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    demand_id      BIGINT       NOT NULL COMMENT '需求ID',
    doc_type       VARCHAR(50)  NOT NULL COMMENT '文档类型：PRD-产品需求文档, UI_DESIGN-UI设计稿, API_DOC-API文档, TECH_DESIGN-技术设计, TEST_CASE-测试用例, USER_MANUAL-用户手册, OTHER-其他',
    title          VARCHAR(200) NOT NULL COMMENT '文档标题',
    description    TEXT COMMENT '文档描述',
    file_url       VARCHAR(500) COMMENT '文件URL（在线文档链接或附件地址）',
    file_name      VARCHAR(255) COMMENT '文件名',
    file_size      BIGINT COMMENT '文件大小（字节）',
    file_type      VARCHAR(50) COMMENT '文件类型：pdf, docx, xlsx, pptx, md, link等',
    version_number INT         DEFAULT 1 COMMENT '版本号',
    version_note   VARCHAR(500) COMMENT '版本说明',
    is_latest      TINYINT(1)  DEFAULT 1 COMMENT '是否最新版本',
    author_id      BIGINT COMMENT '作者ID',
    author_name    VARCHAR(100) COMMENT '作者姓名',
    reviewer_id    BIGINT COMMENT '评审人ID',
    reviewer_name  VARCHAR(100) COMMENT '评审人姓名',
    review_status  VARCHAR(20) DEFAULT 'PENDING' COMMENT '评审状态：PENDING-待评审, APPROVED-已通过, REJECTED-需修改',
    review_comment TEXT COMMENT '评审意见',
    review_time    DATETIME COMMENT '评审时间',
    access_level   VARCHAR(20) DEFAULT 'PUBLIC' COMMENT '访问级别：PUBLIC-公开, INTERNAL-内部, CONFIDENTIAL-机密',
    download_count INT         DEFAULT 0 COMMENT '下载次数',
    view_count     INT         DEFAULT 0 COMMENT '查看次数',
    create_time    DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time    DATETIME    DEFAULT NULL COMMENT '删除时间',

    INDEX idx_demand_id (demand_id),
    INDEX idx_doc_type (doc_type),
    INDEX idx_version (demand_id, version_number),
    FOREIGN KEY (demand_id) REFERENCES demand (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='需求文档表';

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
    INDEX idx_file_hash (`file_hash`)
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
    INDEX idx_create_time (`create_time`),
    INDEX idx_log_uri_time (`uri`, `create_time`),
    INDEX idx_log_username_time (`username`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 数据字典表
DROP TABLE IF EXISTS `dict`;
CREATE TABLE IF NOT EXISTS `dict` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    `type` VARCHAR(50) NOT NULL COMMENT '字典类型(demand_type/demand_priority/demand_status)',
    `code` VARCHAR(50) NOT NULL COMMENT '字典码',
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

DROP TABLE IF EXISTS sys_config;
CREATE TABLE sys_config
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key   VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键（唯一标识）',
    config_value TEXT COMMENT '配置值（支持长文本）',
    config_type  VARCHAR(50) DEFAULT 'string' COMMENT '值类型: string/number/boolean/json',
    config_group VARCHAR(50) DEFAULT 'system' COMMENT '配置分组: system/file/email/sms/security/websocket',
    description  VARCHAR(500) COMMENT '配置描述',
    is_system    TINYINT     DEFAULT 1 COMMENT '是否系统内置: 1-是(不可删除) 0-否',
    is_visible   TINYINT     DEFAULT 1 COMMENT '是否前端可见: 1-是 0-否(敏感配置)',
    sort         INT         DEFAULT 0 COMMENT '排序号',
    create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_group (`config_group`),
    INDEX idx_system (`is_system`),
    INDEX idx_visible (`is_visible`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置表';

-- ==========================================
-- 初始化数据
-- ==========================================

-- 系统角色
INSERT INTO sys_role (role_name, role_key, description)
VALUES ('超级管理员', 'SUPER_ADMIN', '系统最高权限，管理所有资源'),
       ('普通用户', 'USER', '可创建和管理自己的需求'),
       ('产品经理', 'PRODUCT_MANAGER', '可创建、查看、管理需求，参与需求评审'),
       ('项目经理', 'PROJECT_MANAGER', '可审批、分配需求，管理项目进度'),
       ('开发工程师', 'DEVELOPER', '可接收分配的需求，进行开发工作'),
       ('实施/测试', 'IMPLEMENTER', '可进行需求测试、验收'),
       ('只读用户', 'GUEST', '仅可查看信息，无编辑权限');

-- 默认管理员账号 (密码: admin123)
INSERT INTO sys_user (username, password, real_name, email, status)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '系统管理员', 'admin@demand.com', 1);

-- 绑定管理员角色
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u,
     sys_role r
WHERE u.username = 'admin'
  AND r.role_key = 'SUPER_ADMIN';

-- 系统权限
INSERT INTO sys_permission (id, parent_id, name, type, path, perms, icon, sort)
VALUES (1, 0, '需求管理', 1, '/demand', NULL, 'el-icon-document', 1),
       (2, 1, '查看需求', 3, NULL, 'demand:view', NULL, 1),
       (3, 1, '创建需求', 3, NULL, 'demand:create', NULL, 2),
       (4, 1, '编辑需求', 3, NULL, 'demand:update', NULL, 3),
       (5, 1, '删除需求', 3, NULL, 'demand:delete', NULL, 4),
       (6, 1, '提交审核', 3, NULL, 'demand:submit', NULL, 5),
       (7, 1, '审批需求', 3, NULL, 'demand:approve', NULL, 6),
       (8, 1, '分配需求', 3, NULL, 'demand:assign', NULL, 7),
       (9, 1, '变更状态', 3, NULL, 'demand:status_change', NULL, 8),
       (10, 0, '用户管理', 1, '/user', NULL, 'el-icon-user', 2),
       (11, 10, '查看用户', 3, NULL, 'user:view', NULL, 1),
       (12, 10, '创建用户', 3, NULL, 'user:create', NULL, 2),
       (13, 10, '编辑用户', 3, NULL, 'user:update', NULL, 3),
       (14, 10, '删除用户', 3, NULL, 'user:delete', NULL, 4),
       (15, 10, '分配角色', 3, NULL, 'user:assign_role', NULL, 5),
       (16, 0, '角色管理', 1, '/role', NULL, 'el-icon-s-custom', 3),
       (17, 16, '查看角色', 3, NULL, 'role:view', NULL, 1),
       (18, 16, '编辑角色', 3, NULL, 'role:update', NULL, 2),
       (19, 16, '分配权限', 3, NULL, 'role:assign_permission', NULL, 3),
       (20, 0, '系统管理', 1, '/system', NULL, 'el-icon-setting', 4),
       (21, 20, '查看日志', 3, NULL, 'system:log_view', NULL, 1),
       (22, 20, '导出Excel', 3, NULL, 'system:export_excel', NULL, 2),
       (23, 20, '导出PDF', 3, NULL, 'system:export_pdf', NULL, 3),
       (24, 20, '系统配置', 3, NULL, 'system:config', NULL, 4);

-- 角色权限关联
INSERT INTO sys_role_permission (role_id, permission_id)
VALUES (1, 1),
       (1, 2),
       (1, 3),
       (1, 4),
       (1, 5),
       (1, 6),
       (1, 7),
       (1, 8),
       (1, 9),
       (1, 10),
       (1, 11),
       (1, 12),
       (1, 13),
       (1, 14),
       (1, 15),
       (1, 16),
       (1, 17),
       (1, 18),
       (1, 19),
       (1, 20),
       (1, 21),
       (1, 22),
       (1, 23),
       (1, 24),
       (2, 1),
       (2, 2),
       (2, 3),
       (2, 4),
       (2, 5),
       (2, 6),
       (3, 1),
       (3, 2),
       (3, 3),
       (3, 4),
       (3, 5),
       (3, 6),
       (3, 7),
       (4, 1),
       (4, 2),
       (4, 3),
       (4, 6),
       (4, 7),
       (4, 8),
       (4, 9),
       (4, 10),
       (4, 11),
       (5, 1),
       (5, 2),
       (5, 3),
       (5, 4),
       (5, 6),
       (5, 9),
       (6, 1),
       (6, 2),
       (6, 3),
       (6, 6),
       (6, 9),
       (7, 1),
       (7, 2);

-- 默认项目
INSERT INTO sys_project (id, name, code, description, owner_id, visibility, status)
VALUES (1, '需求管理平台', 'DEMAND_PLATFORM', '企业级需求管理平台项目', 1, 1, 1);

-- 项目模块
INSERT INTO project_module (project_id, name, code, parent_id, sort, status)
VALUES (1, '用户中心', 'user-center', 0, 1, 1),
       (1, '用户管理', 'user-manage', 1, 1, 1),
       (1, '角色管理', 'role-manage', 1, 2, 1),
       (1, '权限管理', 'permission-manage', 1, 3, 1),
       (1, '需求管理', 'demand-manage', 0, 2, 1),
       (1, '需求池', 'demand-pool', 5, 1, 1),
       (1, '迭代规划', 'iteration-plan', 5, 2, 1),
       (1, '需求评审', 'demand-review', 5, 3, 1),
       (1, '开发管理', 'dev-manage', 0, 3, 1),
       (1, '任务分配', 'task-assign', 9, 1, 1),
       (1, '代码管理', 'code-manage', 9, 2, 1),
       (1, '进度跟踪', 'progress-track', 9, 3, 1),
       (1, '测试管理', 'test-manage', 0, 4, 1),
       (1, '测试用例', 'test-case', 13, 1, 1),
       (1, '缺陷管理', 'bug-manage', 13, 2, 1),
       (1, '测试报告', 'test-report', 13, 3, 1);

-- 项目标签
INSERT INTO demand_tag (name, color, category, is_system, use_count)
VALUES ('前端', '#409EFF', 'TECHNICAL', 1, 0),
       ('后端', '#67C23A', 'TECHNICAL', 1, 0),
       ('移动端', '#E6A23C', 'TECHNICAL', 1, 0),
       ('API', '#F56C6C', 'TECHNICAL', 1, 0),
       ('数据库', '#909399', 'TECHNICAL', 1, 0),
       ('UI优化', '#409EFF', 'BUSINESS', 1, 0),
       ('用户体验', '#67C23A', 'BUSINESS', 1, 0),
       ('性能优化', '#E6A23C', 'TECHNICAL', 1, 0),
       ('安全', '#F56C6C', 'TECHNICAL', 1, 0),
       ('紧急', '#F56C6C', 'PRIORITY', 1, 0),
       ('高优', '#E6A23C', 'PRIORITY', 1, 0),
       ('低优', '#909399', 'PRIORITY', 1, 0);

-- 系统配置
INSERT INTO sys_config (config_key, config_value, config_type, config_group, description, is_system, is_visible, sort)
VALUES
-- 系统基础配置
('system.name', '需求管理平台', 'string', 'system', '系统名称', 1, 1, 1),
('system.version', '1.0.0', 'string', 'system', '系统版本号', 1, 1, 2),
('system.description', '企业级需求管理平台', 'string', 'system', '系统描述', 1, 1, 3),
-- 文件上传配置
('file.upload.path', 'E:/demand_platform/upload_file', 'string', 'file', '文件上传根路径', 1, 0, 1),
('file.upload.max-size', '10485760', 'number', 'file', '最大文件大小（字节），默认10MB', 1, 1, 2),
('file.upload.allowed-types', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip,rar', 'string', 'file',
 '允许的文件类型（逗号分隔）', 1, 1, 3),
('file.upload.avatar-max-size', '2097152', 'number', 'file', '头像最大文件大小（字节），默认2MB', 1, 1, 4),
-- 邮件配置
('email.mock.enabled', 'true', 'boolean', 'email', '邮件模拟模式：true-打印到日志，false-真实发送', 1, 0, 1),
('email.smtp.host', 'smtp.126.com', 'string', 'email', 'SMTP服务器地址', 1, 0, 2),
('email.smtp.port', '465', 'number', 'email', 'SMTP服务器端口', 1, 0, 3),
('email.smtp.username', 'test@126.com', 'string', 'email', 'SMTP用户名', 1, 0, 4),
('email.smtp.password', 'test', 'string', 'email', 'SMTP密码/授权码', 1, 0, 5),
-- 短信配置
('sms.mock.enabled', 'true', 'boolean', 'sms', '短信模拟模式：true-返回到前端，false-真实发送', 1, 0, 1),
-- 验证码配置
('verification.code.expiration', '300', 'number', 'security', '验证码有效期（秒），默认300秒', 1, 1, 1),
('verification.code.length', '6', 'number', 'security', '验证码长度，默认6位', 1, 1, 2),
('verification.code.send-limit', '60', 'number', 'security', '发送间隔限制（秒），默认60秒', 1, 1, 3),
('verification.code.daily-limit', '50', 'number', 'security', '每日发送上限，默认50次', 1, 1, 4),
-- WebSocket配置
('websocket.anonymous.enabled', 'true', 'boolean', 'websocket', '允许匿名WebSocket连接：true-允许，false-需要认证', 1, 0,
 1),
('websocket.push.enabled', 'true', 'boolean', 'websocket', 'WebSocket推送验证码：true-启用，false-禁用', 1, 0, 2),
-- JWT配置
('jwt.expiration', '86400000', 'number', 'security', 'JWT Token有效期（毫秒），默认24小时', 1, 0, 1),
-- 分页配置
('page.default-size', '10', 'number', 'system', '默认每页显示数量', 1, 1, 1),
('page.max-size', '100', 'number', 'system', '每页最大显示数量', 1, 1, 2);

-- 字典数据
INSERT INTO dict (type, code, name, sort, status, remark)
VALUES
    -- 需求类型
    ('demand_type', 0, '功能需求', 1, 1, '新增功能的需求'),
    ('demand_type', 1, '优化需求', 2, 1, '现有功能优化'),
    ('demand_type', 2, 'Bug修复', 3, 1, '修复已知问题'),
    ('demand_type', 3, '任务', 4, 1, '日常任务'),
    -- 优先级（使用字符串值匹配新设计）
    ('priority', 'LOW', '低', 1, 1, '低优先级'),
    ('priority', 'MEDIUM', '中', 2, 1, '中优先级'),
    ('priority', 'HIGH', '高', 3, 1, '高优先级'),
    ('priority', 'URGENT', '紧急', 4, 1, '紧急处理'),
    -- 需求状态（使用字符串值匹配新设计）
    ('demand_status', 'DRAFT', '草稿', 1, 1, '未提交的草稿状态'),
    ('demand_status', 'PENDING_REVIEW', '待审批', 2, 1, '等待项目经理审批'),
    ('demand_status', 'APPROVED', '审批通过', 3, 1, '已审批通过'),
    ('demand_status', 'REJECTED', '已拒绝', 4, 1, '审批被拒绝'),
    ('demand_status', 'PLANNED', '已规划', 5, 1, '已纳入迭代计划'),
    ('demand_status', 'IN_DEVELOPMENT', '开发中', 6, 1, '正在开发'),
    ('demand_status', 'IN_TEST', '测试中', 7, 1, '正在测试'),
    ('demand_status', 'ACCEPTED', '已验收', 8, 1, '已通过验收'),
    ('demand_status', 'COMPLETED', '已完成', 9, 1, '已完成'),
    ('demand_status', 'CANCELLED', '已取消', 10, 1, '已取消'),
    -- 文件类型
    ('file_type', 0, '图片', 1, 1, 'jpg/png/gif等'),
    ('file_type', 1, '文档', 2, 1, 'doc/pdf等'),
    ('file_type', 2, '压缩包', 3, 1, 'zip/rar等'),
    ('file_type', 3, '其他', 4, 1, '其他类型');

-- 插入默认需求模板
INSERT INTO `demand_template` (`name`, `category`, `content`, `sort`)
VALUES ('Bug 修复模板', 'bug',
        '## 问题描述\n[描述遇到的 Bug 现象]\n\n## 复现步骤\n1. \n2. \n3. \n\n## 期望结果\n[应该是什么样的]\n\n## 实际结果\n[现在是什么样的]\n\n## 环境信息\n- 浏览器：\n- 操作系统：',
        1),
       ('新功能开发模板', 'feature',
        '## 功能概述\n[一句话描述功能]\n\n## 用户故事\n作为 [角色]，我希望 [功能]，以便 [价值]\n\n## 验收标准\n- [ ] \n- [ ] \n- [ ] \n\n## UI 要求\n[如果有设计稿或原型]\n\n## 接口依赖\n[需要哪些后端接口]',
        2),
       ('性能优化模板', 'optimization',
        '## 优化目标\n[要优化的指标，如页面加载时间从 3s 降到 1s]\n\n## 当前瓶颈\n[分析出的性能瓶颈]\n\n## 优化方案\n1. \n2. \n\n## 预期收益\n[优化后的效果]',
        3);
