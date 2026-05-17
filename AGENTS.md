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
- **JWT**: `jjwt 0.12.6`. `application.yml` has `expiration: 20000` (20 seconds — likely a dev override, check before trusting). Principal = `userId` (Integer). Roles stored as `ROLE_ADMIN` / `ROLE_USER` in Spring Security.
- **File serving**: `WebConfig` maps `/api/music/file/**` to `file:${music.file-path}` (default `D:/workspace/music/`).
- **Error pattern**: services throw `RuntimeException`, controllers catch and return `Result.fail(msg)`.
- **Frontend**: `api.js` exposes `get/post/put/del` wrappers with auto Bearer token from `localStorage` key `music_token`. Token decoded client-side via `parseJwt()` for expiry check.
- **Security rules**: `/api/auth/**` + GET endpoints + `/api/ranking/**` are public. `/api/admin/**` requires `ROLE_ADMIN`. All other `/api/**` require auth.

## Implementation Status

**Done**: UserMapper, UserService, AuthController, config classes, all 5 entities, frontend (style.css, api.js, common.js, login/register/index HTML).

**Not yet implemented**: MusicMapper/CommentMapper/LikeRecordMapper/DownloadRecordMapper (interfaces + XML), MusicService, CommentService, LikeService, RankingService, MusicController, CommentController, LikeController, RankingController, AdminController, remaining HTML (detail, upload, ranking, admin pages).

## Test
Only `MusicdemoApplicationTests` with `@SpringBootTest` context-loads test. No meaningful tests exist yet.
