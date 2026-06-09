# 音乐分享平台 (MusicDemo)

一个基于 **Spring Boot 3.3.6 + MyBatis + Spring Security + JWT** 的全栈音乐分享平台。纯 HTML/CSS/JS 前端，Anthropic 纸质感设计风格。

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Java 17, Spring Boot 3.3.6, Spring Security 6, MyBatis 3.0.4 |
| 数据库 | MySQL 8.0 |
| 认证 | JWT (jjwt 0.12.6), BCrypt 密码加密 |
| 前端 | 纯 HTML/CSS/JS (无框架), Cormorant Garamond + Noto Serif SC 衬线字体 |
| 外部集成 | NetEase CloudSearch (Python 子进程, 搜索/封面/歌词), Bilibili (Python 下载器) |
| 构建 | Maven (mvnw wrapper), Log4j2 日志 |

---

## 快速开始

### 前置条件

- JDK 17+
- MySQL 8.0
- Python 3 (for Bilibili downloader & NetEase search)

### 1. 创建数据库

```sql
CREATE DATABASE IF NOT EXISTS music_platform DEFAULT CHARACTER SET utf8mb4;
```

手动执行 `schema.sql` 创建 5 张表：`user`, `music`, `comment`, `like_record`, `download_record`。

### 2. 配置 `application.yml`

编辑 `src/main/resources/application.yml`：
- 修改 `spring.datasource` 的 MySQL 连接信息（用户名/密码）
- 确认 `music.file-path`（音乐文件存储路径，默认 `D:/workspace/music/`）
- 可选：配置 `ai.deepseek.api-key`（用于 AI 生成音乐简介，从环境变量 `ANTHROPIC_AUTH_TOKEN` 读取）

### 3. 启动

```bash
mvnw.cmd spring-boot:run
```

访问 `http://localhost:8443`

### 构建 JAR

```bash
mvnw.cmd clean package
java -jar target/musicdemo-0.0.1-SNAPSHOT.jar
```

---

## 项目结构

```
musicdemo/
├── src/main/java/org/example/musicdemo/
│   ├── common/              # 通用工具
│   │   └── Result.java          统一 API 响应格式 {code, message, data}
│   │   └── ResultCode.java      状态码枚举 (200/400/401/403/404/500)
│   ├── config/              # 配置类
│   │   ├── JwtUtils.java        JWT 签发/解析/验签
│   │   ├── JwtAuthenticationFilter.java  JWT 请求过滤
│   │   ├── SecurityConfig.java  Spring Security 规则
│   │   └── WebConfig.java      静态资源映射
│   ├── controller/          # REST 控制器 (9 个)
│   │   ├── AuthController.java       POST /api/auth/login, /register
│   │   ├── MusicController.java      GET/POST/DELETE /api/music/**
│   │   ├── CommentController.java    GET/POST/DELETE /api/comment/**
│   │   ├── LikeController.java       POST/DELETE /api/like/**
│   │   ├── RankingController.java    GET /api/ranking/**
│   │   ├── AdminController.java      GET/PUT/DELETE /api/admin/**
│   │   ├── Listen1Controller.java    GET /api/external/**
│   │   ├── BilibiliController.java   POST /api/bilibili/download
│   │   └── AiController.java         POST /api/ai/description
│   ├── entity/              # 实体类 (5 个)
│   │   ├── User.java
│   │   ├── Music.java             含 coverPath 封面字段
│   │   ├── Comment.java
│   │   ├── LikeRecord.java
│   │   └── DownloadRecord.java
│   ├── mapper/              # MyBatis Mapper 接口 (5 个)
│   └── service/             # 服务层 (9 个)
│       ├── UserService.java
│       ├── MusicService.java       上传自动获取封面
│       ├── CommentService.java
│       ├── LikeService.java
│       ├── RankingService.java
│       ├── Listen1Service.java      Python 子进程代理 (netease_search.py)
│       ├── CoverService.java       多源封面搜索下载
│       ├── BilibiliService.java     Python 子进程
│       └── AiService.java          DeepSeek API 调用
├── src/main/resources/
│   ├── application.yml        主配置
│   ├── schema.sql             数据库 DDL (5 表)
│   ├── mapper/                5 个 Mapper XML
│   └── static/                前端页面
│       ├── index.html             首页 (歌单 + 搜索 + 分页)
│       ├── detail.html            详情 (播放器 + 封面 + 评论)
│       ├── ranking.html           排行榜 (点赞/下载/评论 3 标签)
│       ├── upload.html            上传 (含 AI 描述生成)
│       ├── bilibili.html          B 站音频下载
│       ├── login.html / register.html  登录/注册
│       ├── admin/users.html / music.html  管理后台
│       ├── css/
│       │   ├── base.css           重置 / 变量 / 基础排版
│       │   ├── layout.css         页面级布局（navbar / 容器 / 分页）
│       │   ├── components.css     通用组件（btn / card / form / toast / skeleton / track-list / rank / table / file-upload / comment / tabs）
│       │   └── player.css         详情页播放器（player-* / lyrics-* / btn-player / comments-section）
│       └── js/
│           ├── common.js          认证 / 提示 / 格式化 / navbar / loading
│           ├── api.js             get/post/put/del/postForm
│           ├── app.js             DOMContentLoaded 入口
│           └── pages/             各页面逻辑 (index/login/register/detail/upload/ranking/bilibili/admin)
├── scripts/                 # 辅助脚本
│   ├── bilibili_demo.py         B站音频下载 (Python)
│   └── netease_search.py        网易云搜索/封面/歌词 (Python)
├── docs/                    # 项目文档
│   ├── 功能实现文档.md
│   ├── 音乐分享平台-详细开发教程.md
│   ├── 音乐分享平台-详细开发教程-第二部分.md
│   ├── USAGE.md
│   ├── defense-keypoints.md
│   └── frontend-tutorial.md
└── .gitignore
```

---

## API 总览

### 认证 (`/api/auth`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/auth/register` | 注册 | 公开 |
| POST | `/api/auth/login` | 登录 | 公开 |

### 音乐 (`/api/music`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/music/list?page=&size=&keyword=` | 分页列表+搜索 | 公开 |
| GET | `/api/music/{id}` | 详情+评论 | 公开 |
| POST | `/api/music/upload` | 上传 (multipart) | 登录 |
| GET | `/api/music/{id}/stream` | 在线播放 | 公开 |
| GET | `/api/music/{id}/download` | 下载 | 公开 |
| DELETE | `/api/music/{id}` | 删除 (本人或管理员) | 登录 |

### 评论 (`/api/comment`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/comment/list/{musicId}` | 评论列表 | 公开 |
| POST | `/api/comment` | 发表评论 | 登录 |
| DELETE | `/api/comment/{id}` | 删除 (本人) | 登录 |

### 点赞 (`/api/like`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/like/{musicId}` | 点赞 | 登录 |
| DELETE | `/api/like/{musicId}` | 取消点赞 | 登录 |
| GET | `/api/like/check/{musicId}` | 是否已赞 | 登录 |
| GET | `/api/like/count/{musicId}` | 点赞数 | 公开 |

### 排行榜 (`/api/ranking`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/ranking/likes` | 点赞榜 | 公开 |
| GET | `/api/ranking/downloads` | 下载榜 | 公开 |
| GET | `/api/ranking/comments` | 评论榜 | 公开 |

### 管理 (`/api/admin`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/admin/users` | 用户列表 | ADMIN |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用 | ADMIN |
| DELETE | `/api/admin/music/{id}` | 删除音乐 | ADMIN |
| DELETE | `/api/admin/comments/{id}` | 删除评论 | ADMIN |
| POST | `/api/admin/music/{id}/cover` | 上传封面 | ADMIN |

### 外部搜索 (`/api/external`)
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/external/search?source=&keywords=&page=` | 多平台搜索 | 公开 |
| GET | `/api/external/playlist?source=&offset=` | 热门歌单 | 公开 |
| GET | `/api/external/playlist/{listId}` | 歌单详情 | 公开 |
| GET | `/api/external/lyric?trackId=` | 歌词 | 公开 |
| GET | `/api/external/bootstrap?trackId=` | 获取播放地址 | 公开 |

### 其他
| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/bilibili/download` | B站音频下载 | 登录 |
| POST | `/api/ai/description` | AI 生成音乐简介 | 登录 |

---

## 认证与权限

- **认证方式**: JWT Bearer Token, 存储于 `localStorage` key `music_token`
- **密码**: BCrypt 哈希存储
- **角色层级**: 游客 → USER (普通用户) → ADMIN (管理员)
- **安全规则**:
  - `/api/auth/**` — 公开
  - GET `/api/music/**` — 公开 (浏览/播放)
  - GET `/api/ranking/**` — 公开
  - GET `/api/external/**` — 公开
  - `/api/music/file/**`, `/api/music/cover/**` — 公开 (文件/封面直链)
  - `/api/admin/**` — 仅 `ROLE_ADMIN`
  - 其他 `/api/**` — 需登录

---

## 前端设计

Anthropic 纸质感风格 (参考 `样板.html`):
- **底色** `#f5f0e6` + SVG 噪点纹理 (`feTurbulence opacity: 0.03`)
- **字体** `Cormorant Garamond` (英文衬线) + `Noto Serif SC` (中文衬线)
- **文字色** `#2c2821` 深棕墨水色
- **强调色** `#8b6914` 琥珀金
- **阴影** 暖棕柔影 `rgba(44,40,33, 0.06~0.10)`
- **布局** 800px 窄版居中，大量留白
- **水印** 右下角固定 `MUSIC` 文字，`opacity: 0.025`
- **卡片** `#faf8f5` 暖白表面，`#e0d8cc` 边框

---

## 外部集成

### 网易云音乐搜索 (NetEase CloudSearch)
- 通过 `netease_search.py` Python 脚本搜索网易云音乐
- 支持搜索、封面获取、歌词获取
- 通过 `ProcessBuilder` 以子进程方式调用

### 封面自动搜索 (CoverService)
- 上传音乐时自动获取封面图
- 优先使用 NetEase CloudSearch (`netease_search.py`)，6 个备用源兜底
- 下载至 `covers/{uuid}.jpg`，路径存入 `music.cover_path`
- WebConfig 将 `/api/music/cover/**` 映射至文件系统

### Bilibili 音频下载
- `scripts/bilibili_demo.py` Python 脚本
- `BilibiliService` 通过 `ProcessBuilder` 调用，下载音频文件

### AI 简介生成
- 对接 DeepSeek API，根据歌名+歌手自动生成音乐简介
- API Key 从环境变量 `ANTHROPIC_AUTH_TOKEN` 读取

---

## 注意事项

- **Windows 路径**: `File.separator` 在 URL 中会产出 `\`，封面路径硬编码为 `"covers/"` 保持兼容
- **数据库**: `sql.init.mode=never`，需手动执行 `schema.sql`
- **JWT 过期**: 默认 259200000ms (72 小时)，可在 `application.yml` 调整
- **NetEase**: `/api/cloudsearch/pc` (unencrypted, POST with User-Agent + Referer) 可用于搜索和封面获取；`bootstrap_track` 返回空 URL (反爬)，不可用
