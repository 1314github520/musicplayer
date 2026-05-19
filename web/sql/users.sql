CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(50) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '加密后的密码',
    nickname VARCHAR(50) DEFAULT '' COMMENT '昵称',
    avatar VARCHAR(255) DEFAULT '/resource/img/default_avatar.jpg' COMMENT '头像路径',
    phone VARCHAR(20) DEFAULT '' COMMENT '手机号',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_at DATETIME DEFAULT NULL COMMENT '最后登录时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';


CREATE TABLE IF NOT EXISTS user_favorites (
    user_id INT NOT NULL COMMENT '用户ID',
    song_id INT NOT NULL COMMENT '歌曲ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (user_id, song_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_favorites_song_id (song_id),
    INDEX idx_user_favorites_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户收藏表';

CREATE TABLE IF NOT EXISTS user_recent_plays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT '用户ID',
    song_id INT NOT NULL COMMENT '歌曲ID',
    played_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '播放时间',
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_recent_plays_user_played (user_id, played_at),
    INDEX idx_user_recent_plays_user_song (user_id, song_id),
    INDEX idx_user_recent_plays_song_id (song_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户最近播放记录表';

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
