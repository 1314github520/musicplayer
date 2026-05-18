-- =====================================================
-- MusicPlayer 版本管理表
-- 用于存储应用版本信息和更新日志
-- =====================================================

CREATE TABLE IF NOT EXISTS version (
    id INT AUTO_INCREMENT PRIMARY KEY,
    versionCode INT NOT NULL COMMENT '版本号(整数，用于比较)',
    versionName VARCHAR(50) NOT NULL COMMENT '版本名称(如 v2.0)',
    downloadUrl VARCHAR(500) NOT NULL COMMENT 'APK下载地址',
    updateLog TEXT COMMENT '更新日志内容(Markdown或纯文本)',
    publishTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    INDEX idx_version_code (versionCode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用版本表';

-- -----------------------------------------------------
-- 插入示例版本数据（请根据实际情况修改）
-- -----------------------------------------------------

-- 清空现有数据（可选）
-- TRUNCATE TABLE version;

INSERT INTO version (versionCode, versionName, downloadUrl, updateLog, publishTime) VALUES
(20, 'v2.0', '/resource/app/MusicPlayer-v2.0.apk',
'## 🎉 MusicPlayer v2.0 更新内容

### ✨ 新功能
- 新增用户登录/注册系统
- 支持深色/浅色主题切换
- 添加歌词实时同步显示
- 支持歌曲收藏和最近播放记录
- 新增歌曲下载功能
- 添加个人资料编辑（头像、昵称、签名等）

### 🎨 界面优化
- 重新设计登录/注册界面
- 优化播放器界面视觉效果
- 添加黑胶唱片旋转动画
- 实现毛玻璃背景效果
- 改进发现页布局

### 🐛 Bug修复
- 修复头像上传失败问题
- 解决重新登录后数据丢失问题
- 修复时区显示错误
- 优化内存泄漏问题

### ⚡ 性能提升
- 优化网络请求重试机制
- 改善数据库查询性能
- 提升图片加载速度',
NOW());

-- -----------------------------------------------------
-- 查询验证数据是否正确插入
-- -----------------------------------------------------

SELECT 
    versionCode AS '版本号',
    versionName AS '版本名',
    downloadUrl AS '下载地址',
    LENGTH(updateLog) AS '日志长度',
    LEFT(updateLog, 100) AS '日志预览...',
    publishTime AS '发布时间'
FROM version 
ORDER BY versionCode DESC 
LIMIT 1;

-- =====================================================
-- 常用维护命令：
--
-- 1. 更新版本信息：
--    UPDATE version SET 
--        versionCode = 21,
--        versionName = 'v2.1',
--        downloadUrl = '/resource/app/MusicPlayer-v2.1.apk',
--        updateLog = '新的更新日志...',
--        publishTime = NOW()
--    WHERE id = 1;
--
-- 2. 查看所有版本历史：
--    SELECT * FROM version ORDER BY versionCode DESC;
--
-- 3. 删除某个版本：
--    DELETE FROM version WHERE versionCode = 20;
-- =====================================================
