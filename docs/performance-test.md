性能你# 性能测试报告

> 项目：音乐分享平台 (MusicDemo)
> 测试工具：Apache Bench (ab) + Chrome DevTools Network 面板

---

## 一、测试环境

| 项目 | 配置 |
|------|------|
| CPU | |
| 内存 | |
| 硬盘 | |
| 操作系统 | Windows |
| JDK | 24.0.1 |
| MySQL | 8.0 |
| 测试工具 | Apache Bench / Chrome DevTools |

---

## 二、测试场景与结果

### 场景 1：音乐列表页

```bash
ab -n 100 -c 10 http://localhost:8443/api/music/list?page=1&size=12
```

| 指标 | 结果 |
|:----|:----:|
| 请求总数 | 100 |
| 并发数 | 10 |
| 平均响应时间 | |
| P95 响应时间 | |
| P99 响应时间 | |
| 吞吐量 (req/s) | |
| 错误率 | |

### 场景 2：音乐详情

```bash
ab -n 100 -c 10 http://localhost:8443/api/music/1
```

| 指标 | 结果 |
|:----|:----:|
| 请求总数 | 100 |
| 并发数 | 10 |
| 平均响应时间 | |
| P95 响应时间 | |
| 错误率 | |

### 场景 3：用户登录

```bash
ab -n 50 -c 5 -p login.json -T application/json http://localhost:8443/api/auth/login
```

`login.json`:
```json
{"username":"test","password":"123456"}
```

| 指标 | 结果 |
|:----|:----:|
| 请求总数 | 50 |
| 并发数 | 5 |
| 平均响应时间 | |
| 错误率 | |

### 场景 4：排行榜

```bash
ab -n 100 -c 10 http://localhost:8443/api/ranking/likes
```

| 指标 | 结果 |
|:----|:----:|
| 平均响应时间 | |
| 吞吐量 | |

### 场景 5：音频流播放（大文件响应）

测试说明：模拟浏览器请求音频流，观察首次字节到达时间和传输速度。

```bash
# 只请求 HTTP 头，测试首字节时间
curl -o /dev/null -s -w "HTTP Code: %{http_code}\nTime: %{time_total}s\nSize: %{size_download}\n" http://localhost:8443/api/music/1/stream
```

| 指标 | 结果 |
|:----|:----:|
| 首字节时间 | |
| 下载速度 | |

### 场景 6：评论写入（带敏感词检测）

```bash
ab -n 50 -c 5 -p comment.json -T application/json -H "Authorization: Bearer {TOKEN}" http://localhost:8443/api/music/1/comments
```

| 指标 | 结果 |
|:----|:----:|
| 平均响应时间 | |
| 错误率 | |

---

## 三、性能分析

### 3.1 瓶颈分析

| 模块 | 潜在瓶颈 | 优化建议 |
|:----|----------|----------|
| 音乐列表 | 大页数时 COUNT 查询 | 加缓存（Redis） |
| 音频播放 | 磁盘 I/O | 用 Nginx 直接代理静态文件 |
| 登录 | BCrypt 加密慢（约 100ms/次） | 适当降低 cost 值 |
| 敏感词检测 | 6W+ 词 DFA 匹配 | 词库预热，实例复用 |

### 3.2 SQL 查询分析

```sql
-- 查看慢查询日志配置
SHOW VARIABLES LIKE 'slow_query%';
SHOW VARIABLES LIKE 'long_query_time';

-- 开启慢查询日志
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1; -- 超过 1 秒记录
```

---

## 四、测试结论

| 结论项 | 说明 |
|--------|------|
| 功能完整性 | 所有接口返回正确状态码和数据结构 |
| 响应时间 | |
| 并发能力 | |
| 改进建议 | |
