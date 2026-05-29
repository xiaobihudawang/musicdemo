# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (Windows)
mvnw.cmd clean package

# Run (starts on port 8443)
mvnw.cmd spring-boot:run

# Run tests (only 1 context-loads test)
mvnw.cmd test
```

After starting, access: `http://localhost:8443/login.html`

## Project Overview

Spring Boot 3.3.6 music sharing platform with MyBatis + Spring Security + JWT authentication. Pure HTML/CSS/JS frontend (no framework). Port 8443. Java 17. JJWT 0.12.6. Log4j2 (Logback excluded).

## Database

- MySQL 8.0, database name: `music_platform`
- Schema defined in `schema.sql` (5 tables: `user`, `music`, `comment`, `like_record`, `download_record`)
- `application.yml` has `spring.sql.init.mode: never` — database must be created and schema applied manually via MySQL client
- `data.sql` is empty and ignored (only contains comments)
- BCrypt passwords, camelCase Java ↔ snake_case DB column mapping enabled via `map-underscore-to-camel-case: true`
- No default admin seed data in schema — insert manually (e.g., `admin` / `admin123` with `role='admin'`)

## Architecture

```
Controller (@RestController) → Service (@Service) → Mapper (@Mapper interface + XML)
         ↓
    returns Result<T> (unified JSON: {code, message, data})
         ↓
    Spring Security filters all /api/** requests
```

### Java package: `org.example.musicdemo`

| Package | Role |
|---------|------|
| `entity/` | POJOs with `@Data` (Lombok), one per DB table |
| `mapper/` | MyBatis interfaces (`@Mapper`) |
| `service/` | Business logic with `@Transactional` where needed |
| `controller/` | `@RestController`, returns `Result<T>` or `Result<?>` |
| `config/` | JWT utils, auth filter, SecurityConfig, WebConfig |
| `common/` | `Result<T>` generic response wrapper, `ResultCode` enum |

### XML Mappers Location

All XML mapper files are in `src/main/resources/mapper/` (not alongside Java interfaces).

## Security & JWT Flow

1. POST `/api/auth/login` → returns `{token, username, role}`
2. Frontend stores token in `localStorage` (key: `music_token`)
3. `api.js` auto-attaches `Authorization: Bearer <token>` to all requests
4. `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) extracts token, validates, sets `SecurityContextHolder` with `UsernamePasswordAuthenticationToken` where principal = `userId` (Integer)
5. Roles prefixed with `ROLE_` in Spring Security (e.g., `ROLE_ADMIN`)

### URL Security Rules (SecurityConfig)

- **Public (no auth needed):** `/api/auth/**`, `/api/music/file/**`, `/api/music/list`, GET `/api/music/*`, GET `/api/music/*/stream`, GET `/api/ranking/**`, static files
- **Admin only (`ROLE_ADMIN`):** `/api/admin/**`
- **Authenticated (any logged-in user):** all other `/api/**` (e.g., music upload/delete, comments, likes, downloads, AI chat, Bilibili download)
- **Everything else:** permitAll (static assets, etc.)

### Music File Serving

`WebConfig` maps URL `/api/music/file/**` to filesystem `D:/workspace/music/`. Uploaded files get UUID-based names to avoid collisions.

## Frontend

- `api.js`: Token management, HTTP wrappers (`get`, `post`, `put`, `del`), toast notifications. Auto-redirects to login on 401. Client-side JWT expiry check via `parseJwt()`.
- `common.js`: Date/size formatting, logout, dynamic navbar rendering (shows different links based on login state)
- `style.css`: Full CSS with navbar, cards, forms, tables, pagination, toast animations, responsive @768px
- Pages: `login.html`, `register.html`, `index.html` (music list), `detail.html` (music detail + comments), `upload.html`, `ranking.html`, `bilibili.html`, `admin/users.html`, `admin/music.html`

## Implementation Status

**All tutorial features are fully implemented:**

| Layer | Files |
|-------|-------|
| Mappers (Java + XML) | UserMapper, MusicMapper, CommentMapper, LikeRecordMapper, DownloadRecordMapper |
| Services | UserService, MusicService, CommentService, LikeService, RankingService |
| Controllers | AuthController, MusicController, CommentController, LikeController, RankingController, AdminController |
| Config | SecurityConfig, WebConfig, JwtUtils, JwtAuthenticationFilter |
| Common | Result, ResultCode |

**Additional features beyond the tutorial:**

| Feature | Files | Description |
|---------|-------|-------------|
| AI Service | `AiController`, `AiService` | DeepSeek-powered description generation (`POST /api/ai/description`) and chat (`POST /api/ai/chat`). API key from `ANTHROPIC_AUTH_TOKEN` env var |
| Bilibili Integration | `BilibiliController`, `BilibiliService`, `bilibili_demo.py`, `bilibili.html` | Proxy Bilibili video audio for download. Python script extracts audio URL via Bilibili API |
| Log4j2 | `log4j2.xml`, `pom.xml` excludes Logback | Replaces Spring Boot default Logback with Log4j2 |
| Admin Pages | `admin/users.html`, `admin/music.html` | User management and music management interfaces |

## Configuration Notes

- `application.yml` `jwt.expiration: 259200000` = 3 days (note: AGENTS.md may reference an old 20000ms dev override — check actual yml before trusting)
- File upload limit: 50MB (configured in `spring.servlet.multipart`)
- All XML mappers use `useGeneratedKeys="true" keyProperty="id"` for auto-fill on insert
- Constructor injection throughout (no `@Autowired` field injection; Lombok `@RequiredArgsConstructor` optional)
- `Result.fail()` returns error messages — services throw `RuntimeException` for business errors
- `ai.deepseek.api-key` reads from environment variable `ANTHROPIC_AUTH_TOKEN`