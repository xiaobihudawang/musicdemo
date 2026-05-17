# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build (Windows)
mvnw.cmd clean package

# Run (starts on port 8443)
mvnw.cmd spring-boot:run

# Run tests
mvnw.cmd test

# Reload Maven in IDEA
# Right-click pom.xml → Maven → Reload Project, or use the Maven tool window refresh icon
```

After starting, access: `http://localhost:8443/login.html`

## Project Overview

Spring Boot 3.3.6 music sharing platform with MyBatis + Spring Security + JWT authentication. Pure HTML/CSS/JS frontend (no framework). Port 8443. Java 17. JJWT 0.12.6.

## Database

- MySQL 8.0, database name: `music_platform`
- Schema defined in `schema.sql` (5 tables: `user`, `music`, `comment`, `like_record`, `download_record`)
- `application.yml` has `spring.sql.init.mode: never` — database must be created and schema applied manually via MySQL client
- BCrypt passwords, camelCase Java ↔ snake_case DB column mapping enabled via `map-underscore-to-camel-case: true`
- Default admin: manu

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
| `mapper/` | MyBatis interfaces (`@Mapper`) + XML in same package |
| `service/` | Business logic with `@Transactional` where needed |
| `controller/` | `@RestController`, returns `Result<T>` or `Result<?>` |
| `config/` | JWT utils, auth filter, SecurityConfig, WebConfig |
| `common/` | `Result<T>` generic response wrapper, `ResultCode` enum |

### XML Mappers Location

Unlike standard MyBatis projects, `UserMapper.xml` is under `src/main/java/org/example/musicdemo/mapper/` alongside the Java interface, NOT under `resources/mapper/`. The `mybatis.mapper-locations: classpath:mapper/*.xml` config works because Maven copies XML files from source roots to the classpath.

## Security & JWT Flow

1. POST `/api/auth/login` → returns `{token, username, role}`
2. Frontend stores token in `localStorage` (key: `music_token`)
3. `api.js` auto-attaches `Authorization: Bearer <token>` to all requests
4. `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) extracts token, validates, sets `SecurityContextHolder` with `UsernamePasswordAuthenticationToken` where principal = `userId` (Integer)
5. Roles prefixed with `ROLE_` in Spring Security (e.g., `ROLE_ADMIN`)

### URL Security Rules (SecurityConfig)

- Public: `/api/auth/**`, GET-only music/comment/download/file endpoints, `/api/ranking/**`
- Admin: `/api/admin/**` (requires `ROLE_ADMIN`)
- Authenticated: any other `/api/**`
- Static files: all permitted

### Music File Serving

`WebConfig` maps URL `/api/music/file/**` to filesystem `D:/workspace/music/`. Uploaded files get UUID-based names to avoid collisions.

## Frontend

- `api.js`: Token management, HTTP wrappers (`get`, `post`, `put`, `del`), toast notifications. Auto-redirects to login on 401.
- `common.js`: Date/size formatting, logout, dynamic navbar rendering (shows different links based on login state)
- `style.css`: Full CSS with navbar, cards, forms, tables, pagination, toast animations, responsive @768px

## Implementation Status (Based on Tutorial)

The tutorial (`音乐分享平台-详细开发教程*.md`) describes a complete app with MusicMapper/CommentMapper/LikeRecordMapper/DownloadRecordMapper, MusicService/CommentService/LikeService/RankingService, and controllers for music/comments/likes/ranking/admin.

**Currently implemented:** UserMapper, UserService, AuthController, all configs, all entities, all frontend files.
**Not yet implemented:** MusicMapper, CommentMapper, LikeRecordMapper, DownloadRecordMapper (interfaces + XML), MusicService (upload/delete/download/list), CommentService, LikeService, RankingService, MusicController, CommentController, LikeController, RankingController, AdminController, and remaining HTML pages (index, detail, upload, ranking, admin pages).

## Configuration Notes

- `application.yml` `jwt.expiration: 259200000` = 3 days
- File upload limit: 50MB (configured in `spring.servlet.multipart`)
- `UserMapper.xml` uses `useGeneratedKeys="true" keyProperty="id"` for auto-fill on insert
- Constructors use constructor injection (no `@Autowired` field injection)
- `Result.fail()` static methods catch `RuntimeException` and return error messages — services throw plain `RuntimeException` for business errors