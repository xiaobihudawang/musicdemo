# 音乐分享平台 — 课程设计答辩要点

> **课程**：Web 开发与数据库联合课程设计
> **项目**：`musicdemo`（基于 Spring Boot 3.3.6 + MyBatis + MySQL 8.0 + 纯前端）
> **答辩时长建议**：10–15 分钟陈述 + 5–10 分钟问答
> **文档定位**：本文件不讲"怎么写代码"，讲"为什么这样设计、答辩时被问到怎么答"。

---

## 目录

1. [一、3 分钟自述稿](#一3-分钟自述稿)
2. [二、需求分析](#二需求分析)
3. [三、技术选型与依据](#三技术选型与依据)
4. [四、系统架构](#四系统架构)
5. [五、数据库设计（重点）](#五数据库设计重点)
6. [六、Web 后端设计](#六web-后端设计)
7. [七、前端设计](#七前端设计)
8. [八、关键技术与亮点](#八关键技术与亮点)
9. [九、系统测试](#九系统测试)
10. [十、部署与运行](#十部署与运行)
11. [十一、答辩常见问题（FAQ）](#十一答辩常见问题faq)
12. [十二、答辩现场可能追问的"坑"](#十二答辩现场可能追问的坑)
13. [十三、配套图表](#十三配套图表)

---

## 一、3 分钟自述稿

> 建议照着背或复述。

各位老师好，我做的项目是"音乐分享平台"，一个支持上传、在线播放、点赞、评论、排行榜、第三方音乐搜索、B 站音频下载、AI 简介生成的 Web 应用。

**技术栈**上，后端用 Spring Boot 3.3.6 + Java 17，持久层用 MyBatis 3.0.4，数据库用 MySQL 8.0，认证用 JWT + Spring Security。前端是没有用任何框架的纯 HTML/CSS/JS，按"基础 + 布局 + 组件 + 详情"四层 CSS 做了模块化。

**核心功能**有 8 块：用户注册登录、音乐上传与在线播放、点赞（Toggle 模式）、评论（带敏感词过滤）、排行榜（点赞/下载/评论三个维度）、管理员后台（用户启停 + 音乐/评论删除）、外部音乐搜索与封面歌词抓取、B 站音频下载（通过 Python 子进程）、AI 自动生成音乐简介（DeepSeek）。

**数据层**有 5 张表：用户、音乐、评论、点赞记录、下载记录。值得注意的是 `music` 表对 `like_count / comment_count / download_count` 三个字段做了**反范式冗余**，避免每次查询排行榜都要 JOIN；一致性由数据库触发器（`trg_like_insert/delete` 等）自动维护。

**亮点**方面，重点说四个：第一是**敏感词 DFA 检测**，使用 DFA 算法，6 万 + 内置词库 + 自定义音乐场景词，对繁简、全角半角、重复字符都做了归一化；第二是**JWT 无状态认证**，无 Session，可水平扩展；第三是**流式音频响应**，通过 `InputStreamResource` 实现边下边播；第四是**外部进程隔离**，Python 脚本以子进程方式调用，主进程崩了不会影响整个应用。

下面我从需求、架构、数据库、Web 后端、前端、关键技术依次展开。

---

## 二、需求分析

### 2.1 用户角色

| 角色 | 权限 |
|---|---|
| **游客** | 浏览、搜索、播放、下载、查看排行榜、查看评论 |
| **普通用户（user）** | 游客所有 + 上传音乐、点赞、评论、删除自己的音乐/评论 |
| **管理员（admin）** | 普通用户所有 + 启用/禁用用户、删除任意音乐/评论、上传封面 |

### 2.2 功能需求（按优先级）

| 优先级 | 功能 | 说明 |
|---|---|---|
| P0 | 注册 / 登录 / JWT 鉴权 | 系统最基本的安全边界 |
| P0 | 音乐上传 / 列表 / 详情 | 核心数据流 |
| P0 | 在线播放（流式） | 用户来这里的根本目的 |
| P1 | 点赞 / 评论 | 互动基础 |
| P1 | 排行榜 | 内容发现 |
| P2 | 多平台外部搜索 | 扩展内容来源 |
| P2 | B 站音频下载 | 站内来源扩充 |
| P2 | AI 简介生成 | 内容运营辅助 |
| P3 | 管理员后台 | 内容治理 |

### 2.3 非功能需求

- **性能**：列表页 100 条以内 P95 < 200ms；播放启动延迟 < 500ms
- **安全**：密码 BCrypt 哈希；SQL 全部走 MyBatis `#{}` 预编译；JWT 过期 72h
- **可用性**：7 × 24，单点故障不影响主流程
- **可扩展**：水平扩展无状态服务（JWT 设计支持）
- **可维护**：Controller → Service → Mapper 三层分明，MyBatis XML 集中管理

---

## 三、技术选型与依据

| 选型 | 替代方案 | 选这个的理由 |
|---|---|---|
| **Spring Boot 3.3.6** | Spring / Spring MVC 裸写 | 起步依赖、自动配置、嵌入式 Tomcat；3.x 全面 Jakarta EE 10，符合未来趋势 |
| **MyBatis 3.0.4** | JPA / Hibernate | SQL 可控、易优化；本项目大量自定义统计查询（排行榜），MyBatis 直写 SQL 更直观 |
| **MySQL 8.0** | PostgreSQL / SQLite | 教学环境普遍安装；UTF8MB4 支持完整；窗口函数可用于排行榜 |
| **JWT (jjwt 0.12.6)** | Session + Redis | 无状态、易横向扩展；RESTful 风格契合 |
| **BCrypt** | MD5 / SHA-1 | 自带盐、抗彩虹表、可调 cost |
| **纯 HTML/CSS/JS 前端** | Vue / React | 课程重点是"前后端交互原理"，框架会掩盖 fetch / DOM / 事件机制的细节；按教学要求 |
| **敏感词检测库（sensitive-word 0.29.3）** | 自己写正则 / 朴素匹配 | DFA 树结构，O(n) 时间复杂度；6W+ 内置词；自动归一化繁简/全角半角/重复字符 |
| **Python 子进程下载 B 站** | Java 直接实现 | bilibili API 经常变，Python 生态 yt-dlp / bilix 跟得最快 |
| **Python 子进程搜索/封面** | Java 直接实现 | 网易云 API 签名复杂，Python 实现更灵活；通过 `ProcessBuilder` 隔离故障域 |
| **DeepSeek API** | 本地 LLaMA | 接入成本低；可换其他兼容 OpenAI 协议的模型 |

---

## 四、系统架构

### 4.1 整体架构

```
┌──────────────────────────────────────────────────────────────┐
│              浏览器（用户）                                    │
│  ┌────────────┐  ┌────────────┐  ┌──────────────┐            │
│  │ index.html │  │ detail.html│  │ admin/*.html │ ...        │
│  │  (列表/搜索)│  │ (播放/评论)│  │   (管理后台)  │            │
│  └─────┬──────┘  └─────┬──────┘  └──────┬───────┘            │
│        │ fetch / JSON    │                │                   │
│        │ (Authorization Bearer xxx)       │                   │
└────────┼─────────────────┼────────────────┼──────────────────┘
         │                 │                │
         ▼                 ▼                ▼
┌──────────────────────────────────────────────────────────────┐
│          Spring Boot 应用（端口 8443）                          │
│  ┌────────────────────────────────────────────────────────┐  │
│  │ Spring Security 过滤器链                                │  │
│  │   JwtAuthFilter → SecurityFilterChain → Controller     │  │
│  └────────────────────────────────────────────────────────┘  │
│  ┌────────────┐  ┌────────────┐  ┌──────────────┐            │
│  │ Controller │→ │  Service   │→ │  MyBatis     │            │
│  │ (9 个)     │  │ (9 个)     │  │  Mapper+XML  │            │
│  └────────────┘  └────────────┘  └──────┬───────┘            │
│                                         │ JDBC                │
└─────────────────────────────────────────┼────────────────────┘
                                          ▼
                              ┌──────────────────┐
                              │   MySQL 8.0      │
                              │   music_platform │
                              │   (5 张表)       │
                              └──────────────────┘

       外部进程：
       ┌──────────────────┐
       │ Python:          │
       │ bilibili_demo.py │
       │ (B 站音频下载)   │
       └──────────────────┘
       ┌──────────────────┐
       │ Python:          │
       │ netease_search.py│
       │ (搜索/封面/歌词) │
       └──────────────────┘
```

### 4.2 模块划分（按包）

```
org.example.musicdemo
├── common/             全局通用
│   ├── Result              统一 API 响应 {code, message, data}
│   ├── ResultCode          状态码枚举
│   └── GlobalExceptionHandler  全局异常 → Result.fail
├── config/             配置类
│   ├── SecurityConfig      Spring Security 规则
│   ├── JwtUtils            JWT 签发/解析
│   ├── JwtAuthFilter       从 Header 提取 token → SecurityContext
│   └── WebConfig           静态资源映射（音乐文件/封面）
├── controller/         REST 控制器
│   ├── AuthController          /api/auth/**
│   ├── MusicController         /api/music/**
│   ├── CommentController       /api/comment/**
│   ├── LikeController          /api/music/**/{id}/like
│   ├── RankingController       /api/ranking/**
│   ├── AdminController         /api/admin/**

│   ├── BilibiliController      /api/bilibili/**
│   └── AiController            /api/ai/**
├── entity/             5 个实体 = 5 张表
├── mapper/             5 个 Mapper 接口（与 XML 一一对应）
├── service/            9 个服务
│   ├── UserService / MusicService / CommentService
│   ├── LikeService / RankingService
│   ├── CoverService           封面自动抓取
│   ├── LyricsService          歌词解析
│   ├── SensitiveWordService   敏感词 DFA 检测
│   ├── BilibiliService        Python 子进程
│   └── AiService              DeepSeek API 调用
└── MusicdemoApplication    启动类
```

---

## 五、数据库设计（重点）

### 5.1 ER 图

```
   ┌─────────────┐                ┌─────────────┐
   │    user     │                │    music    │
   │─────────────│                │─────────────│
   │ id (PK)     │──1:N────┐      │ id (PK)     │
   │ username    │         │      │ title       │
   │ password    │         └─────▶│ artist      │
   │ name        │                │ description │
   │ email       │                │ file_path   │
   │ role        │                │ cover_path  │
   │ enabled     │                │ lyrics      │
   │ totp_*      │                │ like_count  │(冗余)
   │ create_time │                │ comment_cnt │(冗余)
   └─────────────┘                │ download_cnt│(冗余)
         ▲                        │ user_id (FK)│
         │                        │ create_time │
         │                        └──────┬──────┘
         │                               │ 1:N
         │  ┌──────────────┐             │
         ├──│   comment    │◀────────────┤
         │  │──────────────│             │
         │  │ id (PK)      │             │
         │  │ content      │             │
         │  │ user_id (FK) │             │
         │  │ music_id (FK)│             │
         │  │ create_time  │             │
         │  └──────────────┘             │
         │                               │
         │  ┌──────────────┐             │
         │  │ like_record  │◀────────────┤
         │  │──────────────│             │
         └──│ user_id (FK) │             │
            │ music_id(FK) │             │
            │ create_time  │             │
            │ UK(user,music)│            │
            └──────────────┘             │
                                         │
            ┌──────────────┐             │
            │download_record│◀───────────┘
            │──────────────│
            │ id (PK)      │
            │ user_id (FK) │
            │ music_id (FK)│
            │ create_time  │
            └──────────────┘
```

### 5.2 五张表结构

完整 DDL 见 `src/main/resources/schema.sql`。下面给精简版 + 关键注释。

```sql
-- 1. user（用户）
CREATE TABLE `user` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(255) NOT NULL UNIQUE,    -- 登录名
  `password` VARCHAR(255) NOT NULL,          -- BCrypt 哈希
  `name` VARCHAR(255),                       -- 昵称
  `email` VARCHAR(255),
  `role` VARCHAR(50) DEFAULT 'user',         -- 'user' / 'admin'
  `enabled` TINYINT(1) DEFAULT 1,            -- 启用位

  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. music（音乐）
CREATE TABLE `music` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(255) NOT NULL,
  `artist` VARCHAR(255) NOT NULL,
  `description` VARCHAR(2000),
  `file_path` VARCHAR(500) NOT NULL,          -- 相对 music.file-path
  `file_size` BIGINT DEFAULT 0,
  `cover_path` VARCHAR(500),                  -- 封面
  `lyrics` TEXT,                              -- LRC 格式
  `user_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
);

-- 3. comment（评论）— 级联删除
CREATE TABLE `comment` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `content` VARCHAR(2000) NOT NULL,
  `user_id` INT NOT NULL,
  `music_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
);

-- 4. like_record（点赞）— 唯一约束实现 Toggle
CREATE TABLE `like_record` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `music_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_music` (`user_id`, `music_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
);

-- 5. download_record（下载）— 每次一条日志
CREATE TABLE `download_record` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `user_id` INT NOT NULL,
  `music_id` INT NOT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
  FOREIGN KEY (`music_id`) REFERENCES `music`(`id`) ON DELETE CASCADE
);
```

### 5.3 范式分析

| 表 | 满足的最高范式 | 说明 |
|---|---|---|
| `user` | 3NF | 所有非主属性都直接依赖主键 |
| `music` | 3NF | 反范式：`like_count / comment_count / download_count` 是派生冗余 |
| `comment` | 3NF | 纯关系 |
| `like_record` | BCNF | 唯一键 (user_id, music_id) |
| `download_record` | 3NF | 与点赞不同，不设唯一键（同一用户可多次下载） |

**为什么 music 表要反范式？**

排行榜查询是高频操作：

```sql
-- 不冗余的写法（每次要 JOIN + COUNT）
SELECT m.*, COUNT(l.id) AS cnt
FROM music m LEFT JOIN like_record l ON l.music_id = m.id
GROUP BY m.id ORDER BY cnt DESC LIMIT 10;

-- 冗余后的写法（直接读字段）
SELECT * FROM music ORDER BY like_count DESC LIMIT 10;
```

后者走主键索引 + 一个排序，时间从 O(N log N) 退化到 O(K log K)（K=10）。

**一致性如何保证？** 点赞采用 **try-INSERT / catch-DuplicateKey → DELETE** 的 Toggle 模式，`like_count` 由数据库触发器自动维护：

```java
@Transactional
public Map<String, Object> toggle(Integer userId, Integer musicId) {
    Map<String, Object> result = new HashMap<>();
    try {
        likeRecordMapper.insert(record);        // 未赞 → 插入，触发 trg_like_insert +1
        result.put("liked", true);
    } catch (DuplicateKeyException e) {
        likeRecordMapper.delete(userId, musicId); // 已赞 → 删除，触发 trg_like_delete -1
        result.put("liked", false);
    }
    result.put("likeCount", musicMapper.getLikeCountById(musicId));
    return result;
}
```

### 5.4 索引设计

- `user.username` UNIQUE → 自然主索引，登录查得快
- `music.user_id` FK → 隐式索引，加速"我上传的音乐"查询
- `comment.music_id` FK → 加速"某首歌的评论"
- `like_record (user_id, music_id)` UNIQUE → 既保证唯一又支持高效 `WHERE user_id=? AND music_id=?`
- `download_record.music_id` + `user_id` FK → 排行榜和"我的下载"都需要

### 5.5 删除策略

- `comment / like_record / download_record` 都设 `ON DELETE CASCADE` → 删音乐时自动清空
- `music.user_id` **不设 CASCADE** → 删用户由应用层处理（要先停用再清数据，避免误删）

---

## 六、Web 后端设计

### 6.1 三层架构

```
HTTP 请求
   │
   ▼
┌──────────────┐
│  Controller  │  接收参数、调用 Service、返回 Result<T>
│  @RestController │
└──────┬───────┘
       │ 抛 RuntimeException / 正常返回
       ▼
┌──────────────┐
│   Service    │  业务逻辑、事务边界
│   @Service   │  @Transactional 在这里
└──────┬───────┘
       │ 调用 Mapper
       ▼
┌──────────────┐
│   Mapper     │  MyBatis 接口 + XML 写 SQL
│   @Mapper    │
└──────┬───────┘
       │ JDBC
       ▼
   MySQL
```

**为什么 Controller 不写业务？** 复用性、事务粒度、可测试性。Service 才能被 Controller / 定时任务 / 消息消费者复用。

### 6.2 RESTful 风格

所有响应都走 `Result<T>`：

```json
{ "code": 200, "message": "success", "data": { ... } }
{ "code": 401, "message": "未登录", "data": null }
```

| 方法 | 路径 | 含义 | 权限 |
|---|---|---|---|
| POST | `/api/auth/register` | 注册 | 公开 |
| POST | `/api/auth/login` | 登录 | 公开 |
| GET | `/api/music/list` | 列表+分页+搜索 | 公开 |
| GET | `/api/music/{id}` | 详情 | 公开 |
| POST | `/api/music/upload` | 上传（multipart） | 登录 |
| GET | `/api/music/{id}/stream` | 流式播放 | 公开 |
| GET | `/api/music/{id}/download` | 下载 | 公开 |
| DELETE | `/api/music/{id}` | 删除（本人/管理员） | 登录 |
| GET | `/api/music/{id}/comments` | 评论列表 | 公开 |
| POST | `/api/music/{id}/comments` | 发表评论 | 登录 |
| POST | `/api/music/{id}/like` | 点赞/取消点赞（Toggle） | 登录 |
| GET | `/api/music/{id}/like/status` | 查询点赞状态 | 登录 |
| GET | `/api/ranking/likes` | 点赞榜 | 公开 |
| GET | `/api/external/search` | 多平台搜索 | 公开 |
| POST | `/api/bilibili/download` | B 站下载 | 登录 |
| POST | `/api/ai/description` | AI 简介 | 登录 |
| GET/PUT/DELETE | `/api/admin/**` | 管理 | ADMIN |

### 6.3 JWT 鉴权流程

```
┌─────────┐ POST /api/auth/login          ┌─────────────┐
│ Browser │ ───────────────────────────▶ │  Controller │
│         │ {username, password}         │             │
│         │ ◀─────────────────────────── │  → 签发 JWT │
│         │ {code:200, data:{token,      │             │
│         │    userId, username, role}}  └─────────────┘
└─────────┘
    │
    │ 存 localStorage.setItem('music_token', token)
    │
    │  GET /api/music/upload
    │  Header: Authorization: Bearer eyJhbGc...
    ▼
┌────────────────────────────────────────────────────┐
│ JwtAuthFilter.doFilterInternal                    │
│   1. 从 Header 取 token                            │
│   2. JwtUtils.parseToken(token) → Claims           │
│   3. userId = claims.get("sub")                    │
│   4. role = claims.get("role") → "ROLE_" + 大写   │
│   5. UsernamePasswordAuthenticationToken           │
│      → SecurityContextHolder                       │
│   6. chain.doFilter                               │
└────────────────────┬───────────────────────────────┘
                     ▼
              Controller 方法可注入
              Authentication 参数获取 userId
```

**为什么不用 Session？**

| 维度 | Session | JWT |
|---|---|---|
| 状态位置 | 服务端内存 / Redis | 客户端 |
| 横向扩展 | 需要 Sticky Session 或共享 Session 存储 | 天然无状态，任意节点 |
| 跨域 | 需要 CORS + Cookie 配置 | Header 携带即可 |
| 主动失效 | 简单 | 难（需黑名单） |
| 适合场景 | 内部系统 | 公开 API / 多端 |

本项目面向多端（Web / 移动端），用 JWT 更合适。

### 6.4 Spring Security 配置

```java
http
  .csrf().disable()
  .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
  .authorizeHttpRequests(a -> a
      .requestMatchers("/api/auth/**").permitAll()
      .requestMatchers(GET, "/api/music/**").permitAll()
      .requestMatchers(GET, "/api/ranking/**").permitAll()
      .requestMatchers("/api/music/file/**", "/api/music/cover/**").permitAll()
      .requestMatchers("/api/admin/**").hasRole("ADMIN")
      .anyRequest().authenticated()
  )
  .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

**注意细节**：
- `requestMatchers(GET, "/api/music/**")` 只放行 GET，POST `/upload` 仍需登录
- `permitAll()` 和 `authenticated()` 是短路求值，顺序很重要
- `hasRole("ADMIN")` 会自动加 `ROLE_` 前缀，所以 JWT 里也存 `ROLE_ADMIN`

### 6.5 MyBatis 集成

- Mapper 接口：`src/main/java/.../mapper/`
- 映射 XML：`src/main/resources/mapper/*.xml`
- 配置 `mybatis.mapper-locations: classpath:mapper/*.xml`
- 全局开启 `map-underscore-to-camel-case: true`（`like_count` ↔ `likeCount`）

**为什么 XML 而不是注解？**

- 复杂 SQL（动态 SQL、多表 JOIN）XML 更清晰
- 注解 SQL 嵌在 Java 里不利于 DBA 优化

### 6.6 异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handle(RuntimeException e) {
        return Result.fail(e.getMessage());
    }
}
```

业务 Service 抛 `RuntimeException("用户名已存在")` → 全局捕获 → 返回 `Result.fail("用户名已存在")`，前端 `showToast` 显示。

---

## 七、前端设计

### 7.1 整体风格

"Anthropic 纸质感"：米色底 + 暖白卡片 + 琥珀金强调 + 衬线字体 + 大量留白。

- 字体：Cormorant Garamond（英文）+ Noto Serif SC（中文）
- 强调色：`#8b6914`
- 容器：800px 窄版居中（详情页除外）

### 7.2 CSS 四层结构

```
base.css       → 变量/reset/@keyframes   （基础）
layout.css     → navbar/container/分页   （布局）
components.css → btn/card/form/toast     （组件）
player.css     → 详情页播放器+歌词        （特性）
```

后层覆盖前层，详情页额外加载 `player.css`。

### 7.3 JS 三层 + 两工具

```
common.js   →  全局：登录态 / showToast / 格式化 / 渲染导航栏 / loading 淡出
api.js      →  全局：get/post/put/del/postForm（fetch 封装）
forbidden-words.js  →  评论敏感词客户端预检
pages/*.js  →  当前页业务逻辑
app.js      →  启动入口（最后加载）
```

**加载顺序**：

```html
<script src="/js/common.js"></script>   <!-- 1. 工具/导航栏/loading -->
<script src="/js/api.js"></script>     <!-- 2. fetch 封装 -->
<script src="/js/pages/<page>.js"></script>  <!-- 3. 当前页 -->
<script src="/js/app.js"></script>     <!-- 4. 启动 -->
```

**为什么这样？** JS 没有 `import`（除非打包），按顺序同步加载；后置文件可以用前置文件暴露的全局函数。

### 7.4 鉴权与跳转

- `common.js` 加载时把 token 从 localStorage / sessionStorage 读出来
- `api.js` 拦截 401 → 清登录态 → 跳 `/login.html?expired=1`
- 登录/注册页 `<body class="auth-page">` → `app.js` 不插入导航栏

### 7.5 关键交互

- **播放页点赞/评论不刷新整页**：DOM 局部更新（不让 `<audio>` 中断）
- **管理员操作乐观更新**：删除行/切换状态 → 接口返回失败再回滚
- **loading 遮罩至少 1200ms**：`common.js` 记录 `PAGE_LOAD_TS`，`fadeOutLoading` 计算 `Math.max(0, 1200 - 已耗时)`，避免"闪一下就消失"

---

## 八、关键技术与亮点

### 8.1 敏感词 DFA 检测

```java
@Service
public class SensitiveWordService {
    private final SensitiveWordBs wordBs;

    @PostConstruct
    public void init() {
        wordBs = SensitiveWordBs.newInstance()
            .ignoreCase(true)              // 大小写
            .ignoreWidth(true)             // 全角半角
            .ignoreNumStyle(true)          // 数字变体
            .ignoreRepeat(true)            // 重复字符 "操操操"
            .ignoreChineseStyle(true)      // 中英混淆
            .init();
    }

    public boolean containsForbidden(String text) {
        return wordBs.contains(text);
    }
}
```

**为什么 DFA？**

朴素匹配：每个文本字符扫一遍字典 → O(n × m)
DFA（确定性有限自动机）：把字典建成一棵状态树，文本扫一次 → **O(n)**

库内置 6 万+词，添加了 30+ 音乐场景自定义词（"打死你""歌屎"等）。

**双重防御**：客户端预检 60+ 常见词（`forbidden-words.js`）+ 服务端用 DFA 库是权威（防绕过）。

### 8.2 流式音频播放

```java
@GetMapping("/{id}/stream")
public ResponseEntity<InputStreamResource> stream(@PathVariable Integer id,
                                                    @RequestHeader(value = "Range", required = false) String range) {
    Music music = musicService.getById(id);
    File file = new File(music.getFilePath());
    long len = file.length();

    if (range != null) {  // 浏览器请求断点续传
        // 解析 "bytes=0-" → 206 Partial Content
    }

    InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .contentLength(len)
        .body(resource);
}
```

**关键技术点**：

- `InputStreamResource` 不会把整个文件读进内存
- Spring 会用 `StreamUtils.copy()` 一边读一边写响应
- 支持 HTTP `Range` 头实现拖动进度条（断点续传）

### 8.3 文件上传与封面自动抓取

上传流程：

```
multipart/form-data
   │
   ▼ MusicController.upload(@RequestParam MultipartFile file)
   │
   ├─ MusicService.upload()
   │    ├─ 保存文件到 music.file-path
   │    ├─ 读取标题/歌手
   │    ├─ CoverService.fetchCover(title, artist)   ← 同步调用
   │    │    ├─ NetEase CloudSearch API 搜歌
   │    │    └─ 下载封面到 covers/{uuid}.jpg
   │    └─ 写库（音乐 + 封面路径）
```

封面抓取是**同步**的：上传完成时封面已经存好；用户上传时增加 2-3 秒等待，但保证"上传完即有封面"。

### 8.4 外部进程集成

`BilibiliService` 和 `AiService` 用 `ProcessBuilder` 调 Python：

```java
Process p = new ProcessBuilder("python", "script.py", args...)
    .redirectErrorStream(true)
    .start();

BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
String line;
while ((line = r.readLine()) != null) {
    if (line.startsWith("RESULT:")) return parseJson(line.substring(7));
}
int code = p.waitFor();
```

**关键设计**：

- **每请求一进程**：避免状态污染；进程退出即回收
- **约定协议**：Python 端输出 `RESULT: {...JSON...}` 表示成功
- **超时控制**：30 秒 `waitFor` 后强制 `destroyForcibly`
- **故障隔离**：子进程崩了不影响 Spring Boot 主进程

### 8.5 AI 简介生成

`AiService` 用 JDK 11+ `HttpClient` 调 DeepSeek：

```java
HttpRequest req = HttpRequest.newBuilder()
    .uri(URI.create("https://api.deepseek.com/v1/chat/completions"))
    .header("Authorization", "Bearer " + apiKey)
    .POST(BodyPublishers.ofString(jsonBody))
    .build();

HttpResponse<String> resp = client.send(req, BodyHandlers.ofString());
// 解析 choices[0].message.content → 返回给前端
```

**Prompt 设计**：

```
你是一位音乐评论家。请根据以下歌曲信息，写一段 100-200 字的简介，
突出歌曲风格、情感基调和推荐场景，不要剧透歌词。
歌名：{title}
歌手：{artist}
```

---

## 九、系统测试

### 9.1 单元测试

- `MusicdemoApplicationTests.contextLoads()` — Spring 上下文能启动即通过
- `SensitiveWordServiceTest` — 7 个用例：
  - 中文脏话命中
  - 英文脏话命中
  - 暴力词命中
  - 色情暗示命中
  - 正常文本通过
  - 大小写不敏感
  - 重复字符归一化（"操操操"）

```bash
mvnw.cmd test
# Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
```

### 9.2 接口测试（Postman / 浏览器）

| 用例 | 步骤 | 预期 |
|---|---|---|
| 注册 | POST /api/auth/register | code=200 |
| 重复注册 | 同上 | code=400, msg=用户名已存在 |
| 登录成功 | POST /api/auth/login | code=200, data.token 非空 |
| 密码错 | 同上 | code=400 |
| 上传音乐（未登录） | POST /api/music/upload | 401 |
| 上传音乐（登录） | 同上 | code=200, 列表里能看到 |
| 点赞（未赞过） | POST /api/music/1/like | liked=true, like_count +1 |
| 重复点赞（已赞） | 同上 | liked=false, like_count -1（Toggle 取消） |
| 评论含敏感词 | POST /api/comment | 400, msg=评论包含不当内容 |
| 排行榜 | GET /api/ranking/likes | 按 like_count DESC 返回 |
| 管理员删音乐 | DELETE /api/admin/music/1 | 200 |
| 非管理员删 | 同上 | 403 |

---

## 十、部署与运行

### 10.1 本地启动

```bash
# 1. 准备 MySQL
mysql -uroot -p
> CREATE DATABASE music_platform DEFAULT CHARACTER SET utf8mb4;
> SOURCE src/main/resources/schema.sql;

# 2. 改 application.yml 的 datasource 密码

# 4. 启动
mvnw.cmd spring-boot:run
# → http://localhost:8443
```

### 10.2 生产部署

```bash
mvnw.cmd clean package
# → target/musicdemo-0.0.1-SNAPSHOT.jar

java -jar -Dspring.profiles.active=prod target/musicdemo-0.0.1-SNAPSHOT.jar
# 推荐反向代理：Nginx 终止 HTTPS，转发 8443
```

### 10.3 目录结构

```
musicdemo/
├── pom.xml
├── mvnw / mvnw.cmd
├── schema.sql                  ← 5 张表 DDL
├── README.md
├── AGENTS.md                   ← 开发规范（人看 + AI 看）
├── docs/                       ← 文档（答辩要点 / 前端教程）
├── diagrams/                   ← PlantUML + Mermaid 架构图
├── scripts/                   ← Python 外部脚本
└── src/main/
    ├── java/org/example/musicdemo/   ← 6 个包（common/config/controller/entity/mapper/service）
    └── resources/
        ├── application.yml
        ├── mapper/                   ← MyBatis XML
        └── static/                   ← 前端
            ├── css/                   4 个 CSS
            ├── js/                    3 个核心 + pages
            ├── admin/                 2 个管理页
            ├── *.html                 7 个页面
            └── video/                 logo
```

---

## 十一、答辩常见问题（FAQ）

> 这些是**真实答辩中常被问到的问题**，按方向归类。

### Q1. 架构相关

**Q：为什么用 Spring Boot 而不是裸写 Servlet？**
A：自动配置 + 起步依赖 + 嵌入式 Tomcat，几行代码就跑起来。生产级特性（HTTPS、健康检查、外部化配置）开箱即用。

**Q：Spring MVC 的请求处理流程？**
A：DispatcherServlet 接收 → HandlerMapping 找 Handler → HandlerAdapter 调方法 → 返回 ModelAndView / @ResponseBody → 视图解析（RESTful 直接序列化 JSON）。

**Q：MyBatis 和 JPA 怎么选？**
A：MyBatis 是"SQL 优先"，可控可优化；JPA 是"对象优先"，面向聚合根。本项目大量自定义统计查询，MyBatis 更直接。

### Q2. 数据库相关

**Q：为什么 `music` 表要冗余 `like_count` 等字段？**
A：反范式换查询性能。排行榜是高频读路径，省一次 JOIN+COUNT。一致性由数据库触发器自动维护，不需应用层干预。

**Q：`music.like_count` 是怎么更新的？**
A：由数据库触发器自动维护——插入 `like_record` 时 `trg_like_insert` 触发 +1，删除时 `trg_like_delete` 触发 -1。相比应用层手动更新，触发器保证了计数与记录表的绝对一致，不存在漏更新问题。

**Q：JOIN 太多会怎么样？**
A：笛卡尔积爆炸 + 临时表 + 排序在磁盘。本项目最多 2 张表 JOIN（评论带用户名），性能足够。

**Q：MySQL 索引为什么用 B+ 树？**
A：磁盘友好（页 = 16KB）、范围查询快（叶子节点链表）、非叶子节点只存键（矮胖、IO 少）。

**Q：UNIQUE 索引和普通索引的区别？**
A：UNIQUE 在普通索引基础上加了"值不能重复"的约束，触发 DuplicateKey 异常。本项目用 `uk_user_music` 防止重复点赞。

**Q：ON DELETE CASCADE 风险？**
A：删一个用户会级联删大量关联数据，可能误删。本项目 music.user_id 不设 CASCADE，应用层手动控制；评论/点赞/下载设 CASCADE（删音乐时连带清，避免悬空记录）。

### Q3. 安全相关

**Q：JWT 为什么是无状态的？**
A：token 自带用户信息，服务器不用查 session 表。代价是 token 主动失效难（需黑名单或短过期）。

**Q：JWT 怎么防伪造？**
A：签名用 HMAC-SHA256，密钥只服务端知道。客户端无法伪造签名（除非密钥泄露）。

**Q：密码为什么要 BCrypt 而不是 MD5？**
A：MD5 是单向哈希但快（GPU 一秒算 10 亿次），彩虹表秒破。BCrypt 自带盐、可调 cost（默认 10，慢 100ms/次），抗彩虹表 + 暴力破解。

**Q：XSS 怎么防？**
A：所有用户输入在渲染时走 `escapeHtml()` 转义。本项目 `common.js` 暴露 `escapeHtml` 给所有页面用。

**Q：SQL 注入怎么防？**
A：MyBatis `#{}` 是预编译（参数化），`${}` 是字符串拼接。本项目全部用 `#{}`。

### Q4. 前端相关

**Q：为什么不用 Vue/React？**
A：教学目标要求"理解底层"，框架会掩盖 fetch / DOM 事件 / 状态管理的细节。纯 JS 更直观。

**Q：JWT 放 localStorage 安全吗？**
A：放 localStorage 易被 XSS 偷；放 httpOnly cookie 易被 CSRF。本项目放 localStorage 简化，依赖 escapeHtml 防 XSS。生产可改 httpOnly cookie。

**Q：流式播放怎么实现的？**
A：服务端 `InputStreamResource` + `ResponseEntity` 返回 `application/octet-stream`，Spring 用 `StreamUtils.copy` 边读边写；支持 HTTP `Range` 头实现拖动。

### Q5. 工程相关

**Q：事务边界放在哪？**
A：Service 层（`@Transactional`）。Controller 多次调 Service 跨多个事务，粒度太粗；Mapper 单 SQL 自动提交，不合适。

**Q：为什么用 ProcessBuilder 调 Python？**
A：故障隔离、语言独立、可替换实现。HTTP 调第三方 API 更紧耦合且不可控；子进程方式与 Java 解耦，便于独立部署和调试。

**Q：敏感词库怎么维护？**
A：使用 DFA 库 `sensitive-word` 0.29.3，内置 6 万+ 词 + 自定义音乐场景词；通过 `@PostConstruct` 初始化，单例。生产可改成读数据库动态加载。

---

## 十二、答辩现场可能追问的"坑"

> 这些是**容易被老师追问但容易答错**的点，提前准备。

### 12.1 "你这个反范式设计，如果更新失败会不会数据不一致？"

**答**：

点赞的冗余计数不由应用层维护，而是通过 MySQL **触发器**自动同步：

```sql
CREATE TRIGGER trg_like_insert AFTER INSERT ON like_record FOR EACH ROW
    UPDATE music SET like_count = like_count + 1 WHERE id = NEW.music_id;

CREATE TRIGGER trg_like_delete AFTER DELETE ON like_record FOR EACH ROW
    UPDATE music SET like_count = like_count - 1 WHERE id = OLD.music_id;
```

触发器与 INSERT/DELETE 在同一事务内执行，不可能出现"记录插了但计数没 +1"的情况。相比应用层手动 `incrementLikeCount()`，触发器方案更简洁，且保证了 100% 一致性。

### 12.2 "JWT 泄露怎么办？"

**答**：

- 设置合理过期时间（本项目 72h，可改 2h + refresh token）
- HTTPS 传输，防中间人
- 服务端记录已签发的 `jti`，登出时加入黑名单（不破坏"无状态"特性，因为黑名单规模可控）
- 监听异常 IP 调用模式

### 12.3 "Python 子进程卡死怎么办？"

**答**：

- 30 秒 `waitFor` 超时后 `destroyForcibly`
- 异步线程池处理，主线程不被阻塞
- 降级方案：直接返回空结果 + 前端 toast 提示
- Spring 端用 `CompletableFuture` + `orTimeout`

### 12.4 "敏感词库命中率太低怎么办？"

**答**：

- 加同义词替换（"草" ≈ "艹" ≈ "cao"）— 库已内置
- 加拼音匹配（"nmsl" ≈ "尼玛死了"）— 库已支持 `ignoreChineseStyle`
- 机器学习检测（语义分析）— 成本高
- 用户举报 + 人工审核 — UGC 通用做法

### 12.5 "你这个项目用什么部署？"

**答**：

- 单机：直接 `java -jar` + Nginx 反向代理
- 集群：Nginx 负载均衡 + 共享 MySQL（JWT 无状态，任意节点可处理）
- 文件存储：当前本地磁盘，生产换 MinIO / OSS（`music.file-path` 改远程路径即可）

### 12.6 "AI 简介生成耗时多少？影响上传体验吗？"

**答**：

- DeepSeek 响应 2-5 秒
- 改成"上传完成先返回，AI 异步生成后回填 description 字段"更友好
- 当前是同步调用，简单但慢；生产建议异步队列（RabbitMQ / Redis Stream）

### 12.7 "你这个项目跟网易云音乐比有什么不同？"

**答（心态要稳）**：

"这是一个**教学级**的课程设计，目标是展示 Web 开发的完整技术栈。功能上确实不能和商业产品比，但**架构清晰、可扩展**，比如 JWT 设计支持水平扩展、Service 层无状态、文件路径可一键换 OSS。"

---

## 十三、配套图表

`diagrams/` 目录下有 PlantUML 和 Mermaid 双格式源文件：

| 图名 | 文件 |
|------|------|
| ER 图 | `er-diagram.puml` / `er-diagram.mmd` |
| HTTP 请求流程 | `http-request-flow.puml` / `http-request-flow.mmd` |
| 前端加载顺序 | `frontend-loading-order.puml` / `frontend-loading-order.mmd` |
| 详情页加载流程 | `detail-page-flow.puml` / `detail-page-flow.mmd` |
| CSS 分层架构 | `css-layer.puml` / `css-layer.mmd` |

可用 VS Code "PlantUML" 插件预览 `.puml`，或 "Mermaid Preview" 插件预览 `.mmd`。

---

## 附：自评 / 项目亮点总结

1. **架构完整**：Controller → Service → Mapper 三层清晰，5 张表 E-R 规整
2. **安全到位**：JWT + BCrypt + Spring Security + MyBatis 预编译 + XSS 转义
3. **性能有考虑**：反范式冗余 + 唯一索引 + 事务边界 + 流式响应
4. **功能丰富**：基础 CRUD + 互动 + 排行榜 + 多平台搜索 + AI + B 站
5. **工程规范**：统一 `Result<T>` 响应、构造函数注入、敏感词双重防御、客户端预检 + 服务端权威

— END —
