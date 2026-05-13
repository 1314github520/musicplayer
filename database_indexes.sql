-- 数据库性能优化索引
-- 执行此脚本以提升查询性能

-- 歌曲表索引
CREATE INDEX IF NOT EXISTS idx_songs_title ON songs(title);
CREATE INDEX IF NOT EXISTS idx_songs_singer ON songs(singer);
CREATE INDEX IF NOT EXISTS idx_songs_album ON songs(album);
CREATE INDEX IF NOT EXISTS idx_songs_lrcid ON songs(lrcId);
CREATE INDEX IF NOT EXISTS idx_songs_islocal ON songs(isLocal);
CREATE INDEX IF NOT EXISTS idx_songs_isfavorite ON songs(isFavorite);
CREATE INDEX IF NOT EXISTS idx_songs_created_at ON songs(created_at);

-- 最近播放表索引
CREATE INDEX IF NOT EXISTS idx_recentplay_songid ON recent_play(songId);
CREATE INDEX IF NOT EXISTS idx_recentplay_playtime ON recent_play(playTime);

-- 组合索引（用于搜索）
CREATE INDEX IF NOT EXISTS idx_songs_search ON songs(LOWER(title), LOWER(singer), LOWER(album));
