# ECS 部署方案

## 1. 环境准备（ECS 端）

| 组件 | 版本要求 | 安装方式 |
|------|---------|---------|
| JDK | 17+ | `yum install -y java-17-amazon-corretto` 或 `apt install openjdk-17-jdk` |
| MySQL | 8.0 | `yum install mysql-server` 或使用阿里云 RDS |
| Maven | 3.9+ | 可选，也可本地编译后上传 JAR |

## 2. 配置文件变更

### 2.1 application.yml — 配置外部化

```yaml
server:
  port: 8443

spring:
  datasource:
    url: jdbc:mysql://<RDS或ECS内网IP>:3306/music_platform?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

music:
  file-path: /data/music/     # Linux 路径，替代 D:/workspace/music/

jwt:
  secret: ${JWT_SECRET}       # 从环境变量读取，不硬编码
  expiration: 259200000
```

**建议新增 `application-prod.yml`**，通过 `spring.profiles.active=prod` 切换。

### 2.2 需外部化的敏感变量

| 变量 | 说明 | 设置方式 |
|------|------|---------|
| `DB_USERNAME` | 数据库用户名 | ECS 环境变量 |
| `DB_PASSWORD` | 数据库密码 | ECS 环境变量 |
| `JWT_SECRET` | JWT 签名密钥 | ECS 环境变量 |
| `ANTHROPIC_AUTH_TOKEN` | DeepSeek API Key | 已支持环境变量 |

## 3. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE music_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 导入表结构
mysql -u root -p music_platform < /path/to/schema.sql

# 可选：导入示例数据
mysql -u root -p music_platform < /path/to/sample-data.sql
```

## 4. 构建与部署

### 方式 A：本地构建上传 JAR（推荐）

```bash
# Windows 本地打包
.\mvnw.cmd clean package -DskipTests

# 上传到 ECS
scp target/musicdemo-0.0.1-SNAPSHOT.jar root@<ECS_IP>:/opt/musicdemo/app.jar

# ECS 上启动
nohup java -jar /opt/musicdemo/app.jar --spring.profiles.active=prod > /opt/musicdemo/app.log 2>&1 &
```

### 方式 B：ECS 上安装 Maven 构建

```bash
# ECS 上拉代码构建
git clone https://github.com/xiaobihudawang/musicdemo.git /opt/musicdemo
cd /opt/musicdemo
export MAVEN_HOME=/opt/maven
export PATH=$MAVEN_HOME/bin:$PATH
mvn clean package -DskipTests
nohup java -jar target/musicdemo-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod > app.log 2>&1 &
```

## 5. 直接访问（无需 Nginx）

应用直接监听 8443 端口，Spring Boot 同时提供 API 和前端静态资源：

```yaml
# application-prod.yml
server:
  port: 8443
```

访问方式：`http://<ECS公网IP>:8443`

如需 HTTPS，直接在 Spring Boot 中配置 SSL（需准备证书文件）：

```yaml
server:
  port: 443
  ssl:
    enabled: true
    key-store: /etc/ssl/musicdemo.p12
    key-store-password: ${SSL_KEY_PASSWORD}
    key-store-type: PKCS12
```

## 6. Systemd 服务（开机自启）

创建 `/etc/systemd/system/musicdemo.service`：

```ini
[Unit]
Description=Music Platform Backend
After=network.target mysql.service

[Service]
Type=simple
User=root
WorkingDirectory=/opt/musicdemo
Environment=DB_USERNAME=root
Environment=DB_PASSWORD=xxx
Environment=JWT_SECRET=xxx
Environment=ANTHROPIC_AUTH_TOKEN=xxx
ExecStart=/usr/bin/java -jar /opt/musicdemo/app.jar --spring.profiles.active=prod
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
systemctl daemon-reload
systemctl enable musicdemo
systemctl start musicdemo
systemctl status musicdemo
```

## 7. 阿里云 ECS 安全组规则

| 协议 | 端口 | 用途 | 开放范围 |
|------|------|------|---------|
| TCP | 8443 | 应用直接访问 | 0.0.0.0/0（或限定 IP） |
| TCP | 3306 | MySQL | ECS 内网或白名单（不应公开） |

## 8. 需修改的代码文件清单

| 文件 | 修改内容 |
|------|---------|
| `src/main/resources/application.yml` | 数据库凭证外部化、文件路径改为 `/data/music/` |
| 新增 `src/main/resources/application-prod.yml` | 生产环境专用配置覆盖 |

## 9. 部署检查清单

- [ ] ECS 安装 JDK 17
- [ ] ECS 安装 MySQL 8.0（或购买 RDS）
- [ ] 创建数据库 `music_platform`，导入 `schema.sql`
- [ ] 创建 `/data/music/` 目录并设置权限
- [ ] ECS 环境变量设置：`DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`
- [ ] 安全组开放 8443 端口
- [ ] 启动前测试：`java -jar app.jar --spring.profiles.active=prod`
- [ ] 配置 Systemd 服务并 enable
- [ ] 验证：`curl http://your-domain.com:8443/api/music/list`
