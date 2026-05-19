# 音乐分享平台 (Music Sharing Platform)
![img.png](img.png)
基于 Spring Boot 3.3.6 + MyBatis + JWT 的音乐分享平台，提供音乐上传、播放、评论、点赞、排行及 AI 辅助功能。

## 技术栈

- **后端**: Spring Boot 3.3.6 / Java 17 / MyBatis / Spring Security / JWT (jjwt 0.12.6)
- **数据库**: MySQL 8.0
- **日志**: Log4j2
- **前端**: 纯 HTML/CSS/JavaScript (无框架)
- **AI 集成**: DeepSeek API

## 项目结构

```
src/main/java/org/example/musicdemo/
├── common/          # 通用组件 (Result 统一响应等)
├── config/          # 配置类 (Security, JWT, Web, MyBatis 等)
├── controller/      # 控制器层
│   ├── AuthController      # 认证 (登录/注册)
│   ├── MusicController     # 音乐管理
│   ├── CommentController   # 评论管理
│   ├── LikeController      # 点赞管理
│   ├── RankingController   # 排行榜
│   ├── AdminController     # 后台管理
│   ├── AiController        # AI 功能
│   └── BilibiliController  # B站相关
├── service/         # 业务逻辑层
├── mapper/          # 数据访问层 (MyBatis)
└── entity/          # 实体类 (User, Music, Comment, LikeRecord, DownloadRecord)

src/main/resources/
├── application.yml  # 应用配置
├── schema.sql       # 数据库建表脚本
├── data.sql         # 初始数据
├── mapper/*.xml     # MyBatis XML 映射文件
└── static/          # 前端页面
    ├── index.html       # 首页
    ├── login.html       # 登录页
    ├── register.html    # 注册页
    ├── detail.html      # 音乐详情页
    ├── upload.html      # 上传页
    ├── ranking.html     # 排行榜页
    ├── bilibili.html    # B站相关页
    └── admin/           # 后台管理
        ├── music.html   # 音乐管理
        └── users.html   # 用户管理
```

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- MySQL 8.0+

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE music_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行建表脚本 `src/main/resources/schema.sql`

3. 修改 `application.yml` 中的数据库连接信息

### 构建与运行

```bash
# 编译打包
mvnw.cmd clean package

# 启动应用 (默认端口 8443)
mvnw.cmd spring-boot:run

# 运行测试
mvnw.cmd test
```

### 访问地址

- 首页: http://localhost:8443/index.html
- 登录: http://localhost:8443/login.html
- 后台管理: http://localhost:8443/admin/music.html

## 配置说明

### 核心配置 (application.yml)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | 8443 | 服务端口 |
| `spring.datasource.url` | localhost:3306/music_platform | 数据库连接 |
| `jwt.secret` | MusicPlatformSecretKey... | JWT 密钥 |
| `jwt.expiration` | 259200000 (3天) | Token 过期时间 |
| `music.file-path` | D:/workspace/music/ | 音乐文件存储路径 |
| `ai.deepseek.api-key` | 环境变量 | DeepSeek API Key |

### 安全规则

- **公开访问**: `/api/auth/**`、GET 端点、`/api/ranking/**`
- **需要认证**: 其他 `/api/**` 端点
- **管理员权限**: `/api/admin/**` (需要 `ROLE_ADMIN`)

## API 响应格式

所有 API 响应使用统一的 `Result<T>` 格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

## 前端说明

- `api.js`: 封装 `get/post/put/del` 请求，自动携带 Bearer Token
- `common.js`: 公共工具函数 (如 `parseJwt()` 解析 Token)
- Token 存储在 `localStorage` 的 `music_token` 键中

## 注意事项

1. 首次运行前需手动创建 `music_platform` 数据库并执行 `schema.sql`
2. 音乐文件存储路径 `music.file-path` 需确保存在且有读写权限
3. JWT 密钥请妥善保管，不要提交到版本库
4. AI 功能需要配置有效的 DeepSeek API Key

## 开发规范

- 使用构造器注入 (Lombok `@RequiredArgsConstructor`)，禁止字段注入
- XML Mapper 文件位于 `src/main/resources/mapper/`
- 服务层抛出 `RuntimeException`，控制器捕获后返回 `Result.fail(msg)`
- 数据库字段使用下划线命名，自动映射为驼峰

## License

MIT
