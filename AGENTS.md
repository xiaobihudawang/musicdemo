# AGENTS.md

## Build & Run (Windows)
```
mvnw.cmd clean package         # build
mvnw.cmd spring-boot:run       # run on port 8443
mvnw.cmd test                  # only 1 context-loads test exists
```

## Architecture
Spring Boot 3.3.6 / Java 17 / MyBatis / Spring Security / JWT / MySQL 8.0.
Pure HTML/CSS/JS frontend (no framework). All API responses use `Result<T>` (`{code, message, data}`).

```
Controller (@RestController) → Service (@Service) → Mapper (@Mapper + XML)
```

## Quirks & Conventions

- **No `@Autowired` field injection** — always constructor injection (Lombok `@RequiredArgsConstructor` optional).
- **XML mappers** are in `src/main/resources/mapper/` (not alongside Java interfaces — CLAUDE.md is wrong about this). Config: `mybatis.mapper-locations: classpath:mapper/*.xml`.
- **DB**: database `music_platform` must be created manually. `spring.sql.init.mode: never`. `data.sql` is empty. Schema in `schema.sql`. `map-underscore-to-camel-case: true`.
- **JWT**: `jjwt 0.12.6`. `application.yml` has `expiration: 20000` (20 seconds — likely a dev override, check before trusting). Principal = `userId` (Integer). DB stores role as `'admin'` / `'user'` (lowercase); `JwtAuthFilter` upper-cases and prefixes `ROLE_` when mapping to Spring authorities. Client reads `parseJwt(token).role` as `'admin'` / `'user'`.
- **File serving**: `WebConfig` maps `/api/music/file/**` to `file:${music.file-path}` (default `D:/workspace/music/`).
- **Error pattern**: services throw `RuntimeException`, controllers catch and return `Result.fail(msg)`.
- **Security rules**: `/api/auth/**` + GET endpoints (`/api/music/{id}` and `/api/music/{id}/stream` and `/api/music/{id}/lyrics` and `/api/music/list`) + `/api/ranking/**` + `/api/external/**` + `/api/music/cover/**` + `/api/music/file/**` are public. `/api/admin/**` requires `ROLE_ADMIN`. All other `/api/**` require auth.
- **Frontend — script loading order** (every page):
  ```html
  <script src="/js/common.js"></script>   <!-- 认证 / 提示 / 格式化 / navbar / loading 全局函数 -->
  <script src="/js/api.js"></script>     <!-- get/post/put/del/postForm + 401 自动跳转 -->
  <script src="/js/pages/<page>.js"></script>
  <script src="/js/app.js"></script>     <!-- 最后：插入 navbar + 淡出 loading -->
  ```
  `app.js` 在 `body.auth-page`（登录/注册页）下不插入 navbar。`common.js` 在自身加载时记录 `PAGE_LOAD_TS`，`fadeOutLoading()` 据此保证遮罩至少显示 1200ms。

## Frontend File Layout

```
static/
├── css/
│   ├── base.css           重置 / 变量 / 基础排版
│   ├── layout.css         页面级布局（navbar / 容器 / 分页）
│   ├── components.css     通用组件（btn / card / form / toast / skeleton / track-list / rank / table / file-upload / comment / tabs）
│   └── player.css         详情页播放器（player-* / lyrics-* / btn-player / comments-section）
├── js/
│   ├── common.js          认证 / 提示 / 格式化 / navbar / loading
│   ├── api.js             get/post/put/del/postForm
│   ├── app.js             DOMContentLoaded 入口
│   └── pages/
│       ├── index.js       音乐列表 + 搜索 + 翻页
│       ├── login.js       doLogin (用户名/密码/记住我)
│       ├── register.js    doRegister
│       ├── detail.js      详情 + 播放 + 点赞（局部更新）+ 评论（局部增删）+ 歌词 + 封面 + 下载 + 删除
│       ├── upload.js      doUpload + generateDescription
│       ├── ranking.js     排行榜（likes / downloads / comments）
│       ├── bilibili.js    B 站音频下载
│       └── admin/
│           ├── users.js   用户列表 + 启用/禁用
│           └── music.js   音乐列表 + 删除
├── admin/{users,music}.html
├── index.html / login.html / register.html
├── detail.html / upload.html / ranking.html / bilibili.html
└── video/logo.png
```

`/css/style.css`（旧版合并 CSS）已废弃，分散在 `base/layout/components/player` 四个文件。

## Implementation Status

**Done**: UserMapper, UserService, AuthController, all 5 entities + schemas, config classes, all 5 controllers (Auth/Music/Comment/Like/Ranking/Admin/Bilibili/Ai/Listen1), all HTML pages (login/register/index/detail/upload/ranking/bilibili/admin), all page JS.

**Not yet implemented**: MusicMapper / CommentMapper / LikeRecordMapper / DownloadRecordMapper (interfaces + XML), MusicService, CommentService, LikeService, RankingService.

## Test
Only `MusicdemoApplicationTests` with `@SpringBootTest` context-loads test. No meaningful tests exist yet.
