# 数据备份与恢复

> 项目：音乐分享平台 (MusicDemo)

---

## 一、备份

### 方式 1：手动 mysqldump（推荐）

```bash
mysqldump -uroot -p963369Lbc --routines --events --triggers music_platform > backup\music_platform_20260701.sql
```

### 方式 3：仅导出结构（用于分享给他人建库）

```bash
mysqldump -uroot -p963369Lbc --no-data --routines --events --triggers music_platform > schema_full.sql
```

---

## 二、恢复

### 方式：手动恢复

```bash
mysql -uroot -p963369Lbc music_platform < backup\music_platform_20260701.sql
```

---

## 三、导入样本数据

```bash
mysql -uroot -p963369Lbc music_platform < src\main\resources\data.sql
```

这会创建一个管理员账号（admin/admin123）和 10 首示例音乐。

---

## 四、建库完整流程（新环境）

```bash
# 1. 创建数据库
mysql -uroot -p -e "CREATE DATABASE music_platform DEFAULT CHARACTER SET utf8mb4;"

# 2. 建表 + 存储过程 + 触发器 + 函数 + 事件
mysql -uroot -p music_platform < src\main\resources\schema.sql

# 3. 可选：导入样本数据
mysql -uroot -p music_platform < src\main\resources\data.sql

# 4. 启动应用
mvnw.cmd spring-boot:run
```
