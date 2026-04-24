# 需求管理平台 - 后端（Demand Platform）

## 项目简介

需求管理平台是一个基于 **Spring Boot 3.5 + MyBatis** 的企业级需求管理系统，提供完整的需求生命周期管理、审批流程、权限控制和用户认证功能。该平台采用前后端分离架构设计，后端提供 RESTful API 接口，支持多种认证方式、细粒度的权限控制、实时通知推送以及完善的审计日志系统。

### 核心价值

- **全流程管理**：覆盖需求从创建、审批、分配到完成的全生命周期
- **安全可靠**：JWT 认证、RBAC 权限控制、数据脱敏、操作审计
- **高效协作**：审批流程自动化、WebSocket 实时通知、评论互动
- **可扩展性**：模块化设计、数据字典驱动、Redis 缓存优化
- **可观测性**：Actuator 监控、审计日志、健康检查
- **文档完善**：SpringDoc OpenAPI 自动生成接口文档

## 技术栈

### 核心框架
- **Java 17** - LTS 长期支持版本
- **Spring Boot 3.5.13** - 企业级应用开发框架
- **MyBatis 3.0.5** - 持久层框架
- **MySQL 8.0** - 关系型数据库

### 安全认证
- **Spring Security** - 安全框架
- **JWT (jjwt 0.12.6)** - 无状态认证
- **BCrypt** - 密码加密

### 缓存 & 消息
- **Redis 6.0+** - 缓存、验证码存储、会话管理
- **WebSocket** - 实时通知推送
- **STOMP** - WebSocket 子协议

### 邮件 & 文件
- **Spring Mail** - 邮件服务（SMTP/SSL，126邮箱）
- **EasyExcel 3.3.2** - Excel 导入导出
- **iText7 8.0.2** - PDF 生成

### API 文档 & 监控
- **SpringDoc OpenAPI 2.8.13** - API 文档（Swagger UI）
- **Spring Boot Actuator** - 应用监控

### 工具库
- **Lombok** - 简化代码
- **Apache Commons Lang3** - 工具类
- **HikariCP** - 高性能连接池

### 开发工具
- **Maven 3.8+** - 项目构建
- **Git** - 版本控制
- **IDEA** - IDE

## 核心功能

### 1. 用户认证系统
- ✅ 用户名密码登录
- ✅ 邮箱验证码登录（真实邮件发送）
- 📱 手机验证码登录（⚠️ **模拟模式**，未接入真实短信服务）
- ✅ 邮箱验证码注册
- ✅ JWT Token 认证
- ✅ 四种角色：只读用户(0)、普通用户(1)、管理员(2)、项目经理(3)
- ✅ 数据脱敏（手机号、邮箱自动脱敏）
- ✅ 账户禁用/启用管理
- ✅ 密码修改
- ✅ 头像上传

### 2. 权限控制系统（RBAC）
- ✅ 基于角色的访问控制
- ✅ 注解式权限校验 `@RequirePermission`
- ✅ AOP 拦截器自动验证
- ✅ 资源所有权验证
- ✅ 权限上下文管理
- ✅ 精细化权限粒度（角色 + 所有者）

### 3. 需求管理
- ✅ 需求创建、查询、更新、删除
- ✅ 分页查询与多条件过滤（状态、类型、优先级、模块等）
- ✅ 需求状态流转（草稿→待审批→审批通过→开发中→测试中→已完成）
- ✅ 需求附件管理（文件类型校验、大小限制）
- ✅ 需求评论功能（支持多级回复）
- ✅ 数据字典支持（需求类型、优先级、状态）
- ✅ 需求撤回功能
- ✅ 需求提交审核
- ✅ 需求状态历史追踪
- ✅ 需求变更审计
- ✅ 批量分配需求
- ✅ 需求模板管理
- ✅ Dashboard 统计数据
- ✅ 需求趋势分析

### 4. 审批流程
- ✅ 需求提交审批
- ✅ 项目经理审批（通过/拒绝）
- ✅ 审批意见记录
- ✅ 需求分配负责人
- ✅ 审批状态历史追踪
- ✅ 审批通知推送
- ✅ 定时审批提醒

### 5. 验证码系统
- ✅ 邮箱验证码（真实邮件发送，SSL 加密，126邮箱）
- 📱 短信验证码（⚠️ **模拟模式**，未接入真实 SMS 服务）
- ✅ Redis 存储，5分钟过期
- ✅ 发送频率限制（1分钟1次，每天5次）
- ✅ 验证码长度可配置（默认6位）

### 6. 实时通知系统
- ✅ WebSocket 实时推送
- ✅ 审批结果通知
- ✅ 需求分配通知
- ✅ 定时提醒通知
- ✅ 通知已读/未读状态管理
- ✅ 个人通知中心
- ✅ WebSocket 会话管理
- ✅ JWT 鉴权拦截

### 7. 监控与审计
- ✅ 操作审计日志（持久化到数据库）
- ✅ IP 地址记录
- ✅ 用户行为追踪
- ✅ 失败操作告警
- ✅ Spring Boot Actuator 监控端点（health、info、metrics）
- ✅ 定时任务（清理验证码、日志清理、审批提醒）
- ✅ Redis/MySQL/Mail 健康检查
- ✅ 操作日志导出（Excel）

### 8. 数据字典
- ✅ 支持需求类型、优先级、状态等字典管理
- ✅ Redis 缓存，24小时过期
- ✅ 字典类型可扩展
- ✅ 管理员可刷新缓存
- ✅ 前端动态加载字典数据

### 9. 文件上传优化
- ✅ 文件类型白名单校验（jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip,rar）
- ✅ 文件大小限制（默认10MB，可配置）
- ✅ 安全路径校验（防止目录遍历攻击）
- ✅ 附件与需求关联管理
- ✅ 自定义上传路径配置

### 10. 全局搜索
- ✅ 全局搜索功能
- ✅ 支持多模块搜索
- ✅ 搜索结果分类展示

### 11. 角色管理
- ✅ 角色列表查询
- ✅ 角色权限配置
- ✅ 角色创建/编辑/删除

### 12. 模块管理
- ✅ 模块列表管理
- ✅ 模块创建/编辑/删除
- ✅ 模块与需求关联

## 项目结构

demand/ 
├── common/ # 公共类 
│ ├── ErrorCode.java # 错误码枚举 
│ ├── PageResult.java # 分页结果 
│ ├── Result.java # 统一返回结果 
│ ├── ResultCode.java # 状态码枚举 
│ └── RoleEnum.java # 角色枚举 
├── config/ # 配置类 
│ ├── AuditLogAspect.java # 审计日志切面 
│ ├── CorsConfig.java # 跨域配置 
│ ├── JacksonConfig.java # JSON 序列化配置 
│ ├── JwtInterceptor.java # JWT 拦截器 
│ ├── MybatisConfig.java # MyBatis 配置 
│ ├── NotificationWebSocketHandler.java # 通知 WebSocket 处理器 
│ ├── OpenApiConfig.java # Swagger 文档配置 
│ ├── PermissionContext.java # 权限上下文 
│ ├── PermissionInterceptor.java # 权限拦截器 
│ ├── RateLimit.java # 限流注解 
│ ├── RateLimitInterceptor.java # 限流拦截器 
│ ├── RedisConfig.java # Redis 配置 
│ ├── RequirePermission.java # 权限注解 
│ ├── ScheduledTasks.java # 定时任务 
│ ├── SecurityConfig.java # 安全配置 
│ ├── WebMvcConfig.java # Web 配置 
│ ├── WebSocketAuthInterceptor.java # WebSocket 认证拦截器 
│ ├── WebSocketConfig.java # WebSocket 配置 
│ ├── WebSocketHandlerImpl.java # WebSocket 处理器 
│ └── WebSocketHandshakeInterceptor.java # WebSocket 握手拦截器 
├── controller/ # 全局控制器 
│ └── GlobalSearchController.java # 全局搜索 
├── exception/ # 异常处理 
│ ├── BusinessException.java # 业务异常 
│ └── GlobalExceptionHandler.java # 全局异常处理器 
├── module/ # 业务模块 
│ ├── attachment/ # 附件管理 
│ │ ├── controller/ 
│ │ ├── entity/ 
│ │ ├── mapper/ 
│ │ └── service/ 
│ ├── comment/ # 评论管理 
│ │ ├── controller/ 
│ │ ├── entity/ 
│ │ ├── mapper/ 
│ │ └── service/ 
│ ├── demand/ # 需求管理 
│ │ ├── controller/ 
│ │ ├── dto/ # 数据传输对象 
│ │ ├── entity/ # 实体类 
│ │ ├── mapper/ 
│ │ └── service/ 
│ ├── dict/ # 数据字典 
│ │ ├── controller/ 
│ │ ├── entity/ 
│ │ ├── mapper/ 
│ │ └── service/ 
│ ├── module/ # 模块管理 
│ │ ├── controller/ 
│ │ ├── entity/ 
│ │ ├── mapper/ 
│ │ └── service/ 
│ ├── notification/ # 通知管理 
│ │ ├── controller/ 
│ │ ├── entity/ 
│ │ ├── mapper/ 
│ │ └── service/ 
│ └── user/ # 用户管理 
│ ├── controller/ 
│ ├── dto/ 
│ ├── entity/ 
│ ├── mapper/ 
│ └── service/ 
└── util/ # 工具类 
├── EmailSerializer.java # 邮箱脱敏 
├── IpUtil.java # IP 工具 
├── JwtUtil.java # JWT 工具 
└── PhoneSerializer.java # 手机号脱敏

## 核心业务流程

### 用户注册登录流程

开始 → {注册或登录?} 
→ 注册 → 发送邮箱验证码 → 提交注册信息 → 创建用户 
→ 登录 → 输入账号密码/邮箱验证码/手机验证码 → 生成 JWT Token → 返回 Token

**流程说明：**
- **注册**：发送邮箱验证码 → 验证通过后创建账户
- **登录**：支持三种方式（用户名密码/邮箱验证码/手机验证码）
- **认证**：后续请求携带 Token，系统自动验证身份和权限

### 需求审批流程

创建需求 → 待审批 → {项目经理审批} 
→ 通过 → 审批通过 → 分配负责人 → 开发中 → 测试中 → 已完成 
→ 拒绝 → 已拒绝 → {提出人操作: 修改重提/删除}

**状态流转：**
- **草稿(0)** → **待审批(1)** → **审批通过(2)** → **开发中(3)** → **测试中(4)** → **已完成(5)**
- **待审批(1)** → **已拒绝(6)** → 可修改后重新提交或删除

### 权限验证流程

用户请求 → {是否公开接口?} 
→ 是 → 直接放行
→ 否 → 验证 JWT Token → {Token 有效?} 
→ 否 → 返回 401 未授权 
→ 是 → {有权限注解?} 
→ 否 → 执行业务逻辑 
→ 是 → {角色匹配?} 
→ 否 → 返回 403 禁止访问 
→ 是 → {需验证所有者?} 
→ 是 → {是资源所有者?} 
→ 否 → 返回 403 
→ 是 → 执行业务逻辑 
→ 否 → 执行业务逻辑

## 权限设计

### 角色定义

| 角色代码 | 角色名称 | 权限说明 |
|---------|---------|---------|
| 0 | 只读用户 | 只能查看数据，不能执行写操作 |
| 1 | 普通用户 | 创建、管理自己的需求 |
| 2 | 管理员 | 拥有所有权限 |
| 3 | 项目经理 | 审批需求、分配任务 |

### 权限注解使用

java 
// 仅管理员可访问 
@RequirePermission(roles = {2}) 
public Result<?> adminAction() { }
// 管理员或项目经理可访问
@RequirePermission(roles = {2, 3}) 
public Result<?> approveDemand() { }
// 需要验证资源所有者
@RequirePermission(requireOwner = true, resourceIdParam = "id") 
public Result<?> updateResource(@PathVariable Long id) { }
// 管理员或资源所有者可访问
@RequirePermission(roles = {2}, requireOwner = true, resourceIdParam = "id") 
public Result<?> deleteResource(@PathVariable Long id) { }

## API 接口文档

### 用户管理接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/user/register | POST | 用户注册 | 公开 |
| /api/user/login | POST | 用户名密码登录 | 公开 |
| /api/user/login/email | POST | 邮箱验证码登录 | 公开 |
| /api/user/login/phone | POST | 手机验证码登录（模拟） | 公开 |
| /api/user/current | GET | 获取当前用户信息 | 登录用户 |
| /api/user/list | GET | 查询所有用户 | 管理员 |
| /api/user/{id} | GET | 获取用户详情 | 管理员 |
| /api/user/create | POST | 创建用户 | 管理员 |
| /api/user/{id} | PUT | 更新用户信息 | 管理员 |
| /api/user/{id}/role | PUT | 更新用户角色 | 管理员 |
| /api/user/{id}/status | PUT | 禁用/启用用户 | 管理员 |
| /api/user/profile | PUT | 更新个人信息 | 登录用户 |
| /api/user/password | PUT | 修改密码 | 登录用户 |
| /api/avatar/upload | POST | 上传头像 | 登录用户 |

### 验证码接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/verification/email | POST | 发送邮箱验证码 | 公开 |
| /api/verification/phone | POST | 发送手机验证码（⚠️ 模拟模式） | 公开 |

### 需求管理接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/demand | POST | 创建需求 | 普通用户、管理员 |
| /api/demand/{id} | GET | 查询需求详情 | 所有角色 |
| /api/demand/{id} | PUT | 更新需求 | 所有者或管理员 |
| /api/demand/{id} | DELETE | 删除需求 | 所有者 |
| /api/demand/query | POST | 分页查询需求 | 所有角色 |
| /api/demand/approve | POST | 审批需求 | 项目经理、管理员 |
| /api/demand/assign | POST | 分配需求 | 项目经理、管理员 |
| /api/demand/batch-assign | PUT | 批量分配需求 | 项目经理、管理员 |
| /api/demand/{id}/withdraw | POST | 撤回需求 | 所有者 |
| /api/demand/{id}/submit | POST | 提交审核 | 所有者 |
| /api/demand/{id}/status-history | GET | 状态历史 | 所有角色 |
| /api/demand/{id}/activities | GET | 变更审计 | 所有角色 |
| /api/demand/pending-approval | GET | 待审批需求 | 项目经理、管理员 |
| /api/demand/dashboard/stats | GET | Dashboard 统计 | 登录用户 |
| /api/demand/dashboard/trend | GET | 需求趋势 | 登录用户 |
| /api/demand/export | POST | 导出 Excel | 管理员 |

### 需求模板接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/template | POST | 创建模板 | 管理员 |
| /api/template/list | GET | 模板列表 | 所有角色 |
| /api/template/{id} | GET | 模板详情 | 所有角色 |
| /api/template/{id} | PUT | 更新模板 | 管理员 |
| /api/template/{id} | DELETE | 删除模板 | 管理员 |

### 数据字典接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/dict/{type} | GET | 查询字典列表 | 所有角色 |
| /api/dict/types | GET | 查询所有字典类型 | 所有角色 |
| /api/dict/refresh/{type} | POST | 刷新字典缓存 | 管理员 |

### 附件管理接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/attachment/upload/{demandId} | POST | 上传附件 | 所有者 |
| /api/attachment/demand/{demandId} | GET | 查询附件列表 | 所有角色 |
| /api/attachment/download/{id} | GET | 下载附件 | 所有角色 |
| /api/attachment/{id} | DELETE | 删除附件 | 所有者 |

### 评论管理接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/comment | POST | 发表评论 | 登录用户 |
| /api/comment/demand/{demandId} | GET | 查询需求评论 | 所有角色 |
| /api/comment/{id} | PUT | 更新评论 | 评论作者 |
| /api/comment/{id} | DELETE | 删除评论 | 评论作者或管理员 |

### 通知管理接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/notification | GET | 查询我的通知 | 登录用户 |
| /api/notification/{id}/read | PUT | 标记通知已读 | 通知接收者 |
| /api/notification/read-all | PUT | 全部标记已读 | 登录用户 |
| /api/notification/unread-count | GET | 未读通知数量 | 登录用户 |

### 角色管理接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/role/list | GET | 角色列表 | 管理员 |
| /api/role/{id} | GET | 角色详情 | 管理员 |
| /api/role | POST | 创建角色 | 管理员 |
| /api/role/{id} | PUT | 更新角色 | 管理员 |
| /api/role/{id} | DELETE | 删除角色 | 管理员 |

### 模块管理接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/module/list | GET | 模块列表 | 所有角色 |
| /api/module/{id} | GET | 模块详情 | 所有角色 |
| /api/module | POST | 创建模块 | 管理员 |
| /api/module/{id} | PUT | 更新模块 | 管理员 |
| /api/module/{id} | DELETE | 删除模块 | 管理员 |

### 操作日志接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/log/list | GET | 日志列表 | 管理员 |
| /api/log/export | POST | 导出日志 | 管理员 |

### 全局搜索接口

| 接口 | 方法 | 说明 | 权限 |
|------|------|------|------|
| /api/search | GET | 全局搜索 | 登录用户 |

## 快速开始

### 环境要求

- **JDK** 17+
- **MySQL** 8.0+
- **Redis** 6.0+
- **Maven** 3.8+

### 数据库初始化

sql 
-- 创建数据库 
CREATE DATABASE demand_db DEFAULT CHARSET utf8mb4;
-- 执行初始化脚本 
source /path/to/init.sql

### 配置环境变量（可选）

bash
#### Windows PowerShell
$env:DB_USERNAME="root" 
$env:DB_PASSWORD="your-password" 
$env:JWT_SECRET="your-secret-key" 
$env:MAIL_USERNAME="your-email@126.com" 
$env:MAIL_PASSWORD="your-email-password"
#### Linux/Mac
export DB_USERNAME="root" 
export DB_PASSWORD="your-password" 
export JWT_SECRET="your-secret-key" 
export MAIL_USERNAME="your-email@126.com" 
export MAIL_PASSWORD="your-email-password"

### 或者复制配置文件模板：
bash 
cp src/main/resources/application.properties.template src/main/resources/application.properties
#### 然后编辑 application.properties，修改默认值

### 启动项目

bash
#### 编译项目
mvn clean package
#### 运行项目
java -jar target/demand-0.0.1-SNAPSHOT.jar
#### 或者开发模式
mvn spring-boot:run

### 访问应用

- **应用地址**: http://localhost:8080
- **API 文档**: http://localhost:8080/swagger-ui.html
- **健康检查**: http://localhost:8080/actuator/health

## 配置说明

### application.properties 核心配置

properties
#### 服务端口
server.port=8080
#### 数据库配置
spring.datasource.url=jdbc:mysql://localhost:3306/demand_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true 
spring.datasource.username=your-username 
spring.datasource.password=your-password
#### HikariCP 连接池
spring.datasource.hikari.minimum-idle=5 
spring.datasource.hikari.maximum-pool-size=20
#### Redis 配置
spring.data.redis.host=localhost 
spring.data.redis.port=6379
#### JWT 配置
jwt.secret=${JWT_SECRET:your-secret-key-here}
jwt.expiration=${JWT_EXPIRATION:86400000}
#### 文件上传配置
file.upload.path=/path/to/upload/directory
file.upload.allowed-types=jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip,rar 
file.upload.max-size=10485760
#### 邮箱配置（126邮箱 SMTP）
spring.mail.host=smtp.126.com 
spring.mail.port=465 
spring.mail.username=${MAIL_USERNAME:your-email@126.com}
spring.mail.password=${MAIL_PASSWORD:your-email-password} 
spring.mail.properties.mail.smtp.ssl.enable=true
#### 短信配置（⚠️ 模拟模式）
sms.mock.enabled=true
#### 验证码配置
verification.code.expiration=300 
verification.code.length=6 
verification.code.send-limit=60 
verification.code.daily-limit=5
#### Swagger 配置
springdoc.api-docs.path=/api-docs 
springdoc.swagger-ui.path=/swagger-ui.html
#### Actuator 配置
management.endpoints.web.exposure.include=health,info,metrics

## 使用示例

### 1. 注册用户

bash
#### 发送邮箱验证码
curl -X POST http://localhost:8080/api/verification/email 
-H "Content-Type: application/json" 
-d '{"email":"test@example.com"}'
#### 注册
curl -X POST http://localhost:8080/api/user/register 
-H "Content-Type: application/json" 
-d '{ 
  "username":"testuser", 
  "password":"123456", 
  "email":"test@example.com", 
  "emailCode":"123456", 
  "realName":"测试用户" 
}'

### 2. 登录获取 Token

bash
#### 用户名密码登录
curl -X POST http://localhost:8080/api/user/login 
-H "Content-Type: application/json" 
-d '{ 
  "username":"testuser", 
  "password":"123456" 
}'
#### 邮箱验证码登录
curl -X POST http://localhost:8080/api/user/login/email 
-H "Content-Type: application/json" 
-d '{ 
  "email":"test@example.com", 
  "emailCode":"123456" 
}'
#### 手机验证码登录（⚠️ 模拟模式）
curl -X POST http://localhost:8080/api/user/login/phone 
-H "Content-Type: application/json" 
-d '{ 
  "phone":"13800138000", 
  "phoneCode":"123456" 
}'

### 3. 创建需求

bash 
curl -X POST http://localhost:8080/api/demand 
-H "Authorization: Bearer YOUR_TOKEN" 
-H "Content-Type: application/json" 
-d '{ 
  "title":"测试需求", 
  "type":0, 
  "priority":1,
  "module":"用户管理", 
  "description":"这是一个测试需求" 
}'

### 4. 审批需求

bash 
curl -X POST http://localhost:8080/api/demand/approve 
-H "Authorization: Bearer YOUR_TOKEN" 
-H "Content-Type: application/json" 
-d '{ 
  "demandId":1, 
  "approved":true, 
  "comment":"需求合理，同意开发" 
}'

### 5. 查询数据字典

bash
#### 查询需求类型字典
curl -X GET http://localhost:8080/api/dict/demand_type
#### 查询所有字典类型
curl -X GET http://localhost:8080/api/dict/types

## 监控端点

bash
#### 健康检查
GET http://localhost:8080/actuator/health
#### 应用信息
GET http://localhost:8080/actuator/info
#### 指标数据
GET http://localhost:8080/actuator/metrics

## 定时任务

| 任务 | 执行时间 | 说明 |
|------|----------|------|
| 清理验证码 | 每天凌晨 2 点 | 清理 Redis 中的过期验证码 |
| 清理操作日志 | 每天凌晨 3 点 | 删除 90 天前的日志 |
| 审批提醒 | 每小时 | 检查待审批需求并发送通知 |

## 数据脱敏说明

返回用户信息时自动脱敏敏感字段：
- **手机号**：138****8000
- **邮箱**：te***@qq.com

## 安全特性

### 1. 认证安全
- JWT Token 签名验证
- Token 过期机制（默认24小时）
- 密码加密存储（BCrypt）
- 验证码防暴力破解

### 2. 授权安全
- RBAC 角色权限控制
- 资源所有权验证
- AOP 切面统一拦截
- 最小权限原则

### 3. 数据安全
- 敏感数据脱敏（手机号、邮箱）
- SQL 注入防护（MyBatis 参数化查询）
- XSS 防护
- CSRF 防护

### 4. 文件安全
- 文件类型白名单校验
- 文件大小限制
- 安全路径校验（防止目录遍历）

### 5. API 安全
- CORS 跨域配置
- 接口限流（`@RateLimit` 注解）
- 参数校验（JSR-303）
- 统一异常处理

## 性能优化

### 1. 数据库优化
- HikariCP 高性能连接池
- MyBatis 结果集映射优化
- 索引优化
- 分页查询

### 2. 缓存优化
- Redis 缓存验证码
- 数据字典缓存（24小时）
- 热点数据缓存

### 3. 异步处理
- 邮件发送异步化
- 通知推送异步化
- 日志记录异步化

## ⚠️ 注意事项

### 重要说明

1. **手机验证码功能**：当前为**模拟模式**（`sms.mock.enabled=true`），未接入真实短信服务（如阿里云、腾讯云 SMS）。开发测试时验证码会直接返回给前端，生产环境需接入真实短信服务。

2. **启动前准备**：确保 MySQL 和 Redis 服务已运行
3. **邮箱配置**：验证码需要正确配置 SMTP 信息（默认使用126邮箱）
4. **生产环境**：建议使用环境变量管理敏感配置（JWT Secret、邮箱密码等）
5. **验证码限制**：验证码错误5次后会被锁定10分钟
6. **只读用户**：只能查看数据，不能执行写操作
7. **文件上传**：支持类型：jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx,txt,zip,rar
8. **文件大小**：限制 10MB，可通过配置修改
9. **数据字典**：初始化后会自动缓存到 Redis
10. **JWT Secret**：生产环境务必修改默认值
11. **定期清理**：定时任务会自动清理操作日志和过期数据

## 许可证

Apache 2.0

## 联系方式

如有问题或建议，欢迎联系开发者。
