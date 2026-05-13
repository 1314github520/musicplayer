# 音乐播放器项目部署指南

## 🚀 快速部署

### 一、服务器端部署

#### 1. 环境准备
```bash
# 安装 Node.js (推荐 v18+)
node --version

# 安装 MySQL (推荐 8.0+)
mysql --version
```

#### 2. 数据库配置
```bash
# 登录MySQL
mysql -u root -p

# 创建数据库
CREATE DATABASE musicplayer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 创建用户并授权
CREATE USER 'musicplayer_user'@'localhost' IDENTIFIED BY 'your_secure_password_here';
GRANT ALL PRIVILEGES ON musicplayer.* TO 'musicplayer_user'@'localhost';
FLUSH PRIVILEGES;

# 执行索引优化脚本
USE musicplayer;
SOURCE /path/to/database_indexes.sql;
```

#### 3. 应用配置
```bash
# 进入web目录
cd web/

# 安装依赖
npm install

# 创建环境变量文件
cp .env.example .env

# 编辑 .env 文件，配置数据库连接
nano .env
```

**.env 文件内容**:
```env
DB_HOST=localhost
DB_PORT=3306
DB_USER=musicplayer_user
DB_PASSWORD=your_secure_password_here  # ⚠️ 必须修改
DB_NAME=musicplayer
PORT=3000
```

#### 4. 启动服务
```bash
# 开发环境
npm run dev

# 生产环境
npm start

# 使用 PM2 (推荐)
pm2 start server.js --name musicplayer-server
pm2 save
pm2 startup
```

#### 5. 验证部署
```bash
# 测试API
curl http://localhost:3000/api/songs?size=10

# 测试健康检查
curl http://localhost:3000/
```

---

### 二、Android客户端部署

#### 1. 环境准备
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK 35

#### 2. 项目配置
```bash
# 克隆项目
git clone <repository-url>
cd MusicPlayer

# 创建签名配置文件 (keystore.properties)
nano keystore.properties
```

**keystore.properties 内容**:
```properties
keystore.path=key/musicPlayer
keystore.password=your_keystore_password
keystore.alias=your_key_alias
keystore.keyPassword=your_key_password
```

#### 3. 构建APK
```bash
# Debug版本
./gradlew assembleDebug

# Release版本
./gradlew assembleRelease

# 生成的APK位置
# app/release/app-release.apk
```

#### 4. 安装测试
```bash
# 安装到设备
adb install app/release/app-release.apk

# 或使用Android Studio直接运行
```

---

## 🔒 安全配置

### 1. 服务器安全

#### 防火墙配置
```bash
# 开放必要端口
sudo ufw allow 3000/tcp
sudo ufw allow 3306/tcp
sudo ufw enable
```

#### SSL证书配置 (推荐)
```bash
# 使用 Let's Encrypt
sudo apt install certbot
sudo certbot certonly --standalone -d yourdomain.com

# 配置Nginx反向代理
sudo nano /etc/nginx/sites-available/musicplayer
```

**Nginx配置**:
```nginx
server {
    listen 443 ssl http2;
    server_name yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}

server {
    listen 80;
    server_name yourdomain.com;
    return 301 https://$server_name$request_uri;
}
```

### 2. 数据库安全

```sql
-- 删除空用户
DELETE FROM mysql.user WHERE User='';

-- 禁止root远程登录
DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');

-- 删除测试数据库
DROP DATABASE IF EXISTS test;

-- 刷新权限
FLUSH PRIVILEGES;
```

---

## 📊 性能优化

### 1. 数据库优化

```sql
-- 查看索引使用情况
SHOW INDEX FROM songs;

-- 分析查询性能
EXPLAIN SELECT * FROM songs WHERE title LIKE '%test%';

-- 优化表
OPTIMIZE TABLE songs;
ANALYZE TABLE songs;
```

### 2. 服务器优化

#### PM2集群模式
```bash
# 启动集群模式 (根据CPU核心数)
pm2 start server.js -i max --name musicplayer-server

# 监控
pm2 monit
```

#### Node.js内存优化
```bash
# 增加内存限制
pm2 start server.js --node-args="--max-old-space-size=2048"
```

---

## 🔍 监控与日志

### 1. 应用监控

```bash
# PM2监控
pm2 monit

# 查看日志
pm2 logs musicplayer-server

# 实时日志
pm2 logs --lines 100
```

### 2. 数据库监控

```sql
-- 查看连接数
SHOW STATUS LIKE 'Threads_connected';

-- 查看慢查询
SHOW VARIABLES LIKE 'slow_query_log';
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 2;
```

---

## 🐛 故障排查

### 常见问题

#### 1. 数据库连接失败
```bash
# 检查MySQL服务
sudo systemctl status mysql

# 检查连接
mysql -u musicplayer_user -p -h localhost musicplayer

# 查看错误日志
tail -f /var/log/mysql/error.log
```

#### 2. 端口被占用
```bash
# 查看端口占用
lsof -i :3000

# 杀死进程
kill -9 <PID>
```

#### 3. 内存不足
```bash
# 查看内存使用
free -h

# 清理缓存
sudo sync && sudo sysctl -w vm.drop_caches=3
```

---

## 📝 维护计划

### 每日任务
- [ ] 检查应用日志
- [ ] 监控服务器资源
- [ ] 备份数据库

### 每周任务
- [ ] 检查磁盘空间
- [ ] 更新安全补丁
- [ ] 分析性能指标

### 每月任务
- [ ] 数据库优化
- [ ] 安全审计
- [ ] 备份验证

---

## 📞 技术支持

如遇问题，请检查：
1. 服务器日志: `pm2 logs musicplayer-server`
2. 数据库日志: `/var/log/mysql/error.log`
3. 系统日志: `/var/log/syslog`

---

**部署完成！** 🎉

如有疑问，请参考 [OPTIMIZATION_LOG.md](file:///e:/Code/AndroidApplication/MusicPlayer/OPTIMIZATION_LOG.md) 查看详细优化内容。
