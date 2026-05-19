const express = require('express');
const path = require('path');
const mysql = require('mysql2/promise');
const fs = require('fs');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');

const app = express();
const PORT = 3000;

const JWT_SECRET = '123456';
const JWT_EXPIRES_IN = '7d';
const RECENT_PLAY_WINDOW_DAYS = 7;

app.use(express.json({
  strict: false,
  limit: '10mb'
}));
app.use(express.urlencoded({ extended: true }));

const corsHeaders = (req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Range, Authorization');

  if (req.method === 'OPTIONS') {
    return res.sendStatus(200);
  }

  next();
};

app.use(corsHeaders);

/**
 * 资源根目录
 * 当前资源实际分布：
 *
 * /musicplayer/song/
 * /musicplayer/img/
 */
const RESOURCE_PATH = '/musicplayer';

const dbConfig = {
  host: '8.162.14.195',
  port: '3306',
  user: 'xqf',
  password: '123456',
  database: 'musicplayer',
  waitForConnections: true,
  connectionLimit: 10,
  charset: 'utf8mb4'
};

const pool = mysql.createPool(dbConfig);

function formatTime(sec) {
  if (!sec || sec === 0) return null;

  const m = Math.floor(sec / 60);
  const s = sec % 60;

  return `${m}:${s.toString().padStart(2, '0')}`;
}

/**
 * 格式化日期为 YYYY-MM-DD 格式，处理时区问题
 */
function formatDate(dateValue) {
  if (!dateValue) {
    return null;
  }
  
  let date;
  if (dateValue instanceof Date) {
    date = dateValue;
  } else if (typeof dateValue === 'string') {
    date = new Date(dateValue);
    if (isNaN(date.getTime())) {
      const match = dateValue.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/);
      if (match) {
        return `${match[1]}-${match[2].padStart(2, '0')}-${match[3].padStart(2, '0')}`;
      }
      return dateValue.split('T')[0].split('.')[0];
    }
  } else {
    return String(dateValue).split('T')[0].split('.')[0];
  }
  
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  
  return `${year}-${month}-${day}`;
}

/**
 * 修复图片路径
 *
 * 数据库存的是：
 * /musicplayer/img/xxx.jpg
 *
 * 前端需要：
 * /resource/img/xxx.jpg
 */
function fixPhoto(photo) {
  if (!photo) {
    return '/resource/img/default.jpg';
  }

  // 如果已经是 /resource 开头，直接返回
  if (photo.startsWith('/resource/')) {
    return photo;
  }

  // 兼容旧数据 /musicplayer 开头
  if (photo.startsWith('/musicplayer/')) {
    return photo.replace('/musicplayer', '/resource');
  }

  return photo;
}

const ok = (data) => ({
  code: 0,
  data
});

const err = (msg, code = 400) => ({
  code,
  message: msg
});

function escapeLikeWildcards(str) {
  if (!str || typeof str !== 'string') return '';
  return str.replace(/[%_\\]/g, '\\$&');
}

function validateId(id) {
  if (!id) return { valid: false, error: 'Missing id' };
  const num = parseInt(id);
  if (isNaN(num) || num <= 0) {
    return { valid: false, error: 'Invalid id: must be a positive integer' };
  }
  return { valid: true, value: num };
}

function validatePage(page, size) {
  const p = parseInt(page) || 1;
  const s = parseInt(size) || 10;
  
  if (p < 1) return { valid: false, error: 'Page must be >= 1' };
  if (s < 1 || s > 100) return { valid: false, error: 'Size must be between 1 and 100' };
  
  return { valid: true, page: p, size: s };
}

function validateFilePath(filePath, resourcePath) {
  const resolved = path.resolve(resourcePath, filePath);
  const normalized = path.normalize(resolved);
  
  if (!normalized.startsWith(path.resolve(resourcePath))) {
    return { valid: false, error: 'Path traversal detected' };
  }
  
  return { valid: true, path: normalized };
}

/**
 * 静态资源映射
 *
 * /resource/*
 * -> /musicplayer/*
 */
app.use('/resource', express.static(RESOURCE_PATH, {
  setHeaders: (res, filePath) => {
    if (
      filePath.endsWith('.mp3') ||
      filePath.endsWith('.m4a') ||
      filePath.endsWith('.flac')
    ) {
      res.setHeader('Accept-Ranges', 'bytes');
    }
  }
}));

// 同时支持 /musicplayer 路径访问（兼容旧数据）
app.use('/musicplayer', express.static(RESOURCE_PATH, {
  setHeaders: (res, filePath) => {
    if (
      filePath.endsWith('.mp3') ||
      filePath.endsWith('.m4a') ||
      filePath.endsWith('.flac')
    ) {
      res.setHeader('Accept-Ranges', 'bytes');
    }
  }
}));

/**
 * 首页
 */
app.get('/', (req, res) => {
  res.sendFile(path.join(__dirname, 'index.html'));
});

/**
 * 获取歌曲分页
 */
app.get('/api/songs', async (req, res) => {
  try {
    const validation = validatePage(req.query.page, req.query.size);
    if (!validation.valid) {
      return res.status(400).json(err(validation.error));
    }

    const { page, size } = validation;
    const offset = (page - 1) * size;

    const [rows] = await pool.query(
      `
      SELECT
        id,
        title,
        artist,
        singer,
        photo,
        duration,
        album,
        lrc_id
      FROM songs
      ORDER BY id
      LIMIT ? OFFSET ?
      `,
      [size, offset]
    );

    const result = rows.map(s => ({
      ...s,
      photo: fixPhoto(s.photo),
      duration_text: formatTime(s.duration)
    }));

    res.json(ok(result));

  } catch (e) {
    console.error(e);

    res.status(500).json(
      err('Database error: ' + e.message, 500)
    );
  }
});

/**
 * 获取全部歌曲
 */
app.get('/api/songs/all', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `
      SELECT
        id,
        title,
        artist,
        singer,
        photo,
        duration,
        album,
        lrc_id
      FROM songs
      ORDER BY id
      `
    );

    const result = rows.map(s => ({
      ...s,
      photo: fixPhoto(s.photo),
      duration_text: formatTime(s.duration)
    }));

    res.json(ok(result));

  } catch (e) {
    console.error(e);

    res.status(500).json(
      err('Database error: ' + e.message, 500)
    );
  }
});

/**
 * 歌曲详情
 */
app.get('/api/song/detail', async (req, res) => {
  try {
    const validation = validateId(req.query.id);
    if (!validation.valid) {
      return res.status(400).json(err(validation.error));
    }

    const [rows] = await pool.query(
      'SELECT * FROM songs WHERE id = ?',
      [validation.value]
    );

    if (rows.length === 0) {
      return res.status(404).json(
        err('Not Found', 404)
      );
    }

    const song = rows[0];

    song.photo = fixPhoto(song.photo);
    song.duration_text = formatTime(song.duration);

    res.json(ok(song));

  } catch (e) {
    console.error(e);

    res.status(500).json(
      err('Database error: ' + e.message, 500)
    );
  }
});

/**
 * 搜索歌曲
 */
app.get('/api/song/search', async (req, res) => {
  try {
    const keyword = req.query.q;

    if (!keyword || typeof keyword !== 'string') {
      return res.status(400).json(
        err('Missing or invalid query parameter')
      );
    }

    if (keyword.length > 100) {
      return res.status(400).json(
        err('Query too long (max 100 characters)')
      );
    }

    const pageValidation = validatePage(req.query.page, req.query.size);
    if (!pageValidation.valid) {
      return res.status(400).json(err(pageValidation.error));
    }

    const { page, size } = pageValidation;
    const offset = (page - 1) * size;

    const escapedKeyword = escapeLikeWildcards(keyword.trim());
    const kw = `%${escapedKeyword}%`;

    const [rows] = await pool.query(
      `
      SELECT
        id,
        title,
        artist,
        singer,
        photo,
        duration,
        album,
        lrc_id
      FROM songs
      WHERE
        LOWER(title) LIKE ?
        OR LOWER(singer) LIKE ?
        OR LOWER(album) LIKE ?
      ORDER BY created_at DESC
      LIMIT ? OFFSET ?
      `,
      [kw, kw, kw, size, offset]
    );

    const result = rows.map(s => ({
      ...s,
      photo: fixPhoto(s.photo),
      duration_text: formatTime(s.duration)
    }));

    res.json(ok(result));

  } catch (e) {
    console.error(e);

    res.status(500).json(
      err('Database error: ' + e.message, 500)
    );
  }
});

/**
 * 检查应用更新
 */
app.get('/api/app/version', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT
        versionCode,
        versionName,
        downloadUrl,
        updateLog,
        publishTime
      FROM version
      ORDER BY versionCode DESC
      LIMIT 1`
    );

    if (rows.length === 0) {
      return res.status(404).json(
        err('No version found', 404)
      );
    }

    const version = rows[0];

    // 修复下载地址路径
    let downloadUrl = version.downloadUrl;
    if (downloadUrl.startsWith('/musicplayer/')) {
      downloadUrl = '/resource/' + downloadUrl.replace('/musicplayer/', '');
    } else if (downloadUrl.startsWith('/resource/')) {
      // Already correct
    } else if (!downloadUrl.startsWith('http')) {
      downloadUrl = '/resource/app/' + downloadUrl;
    }

    // 防御性处理：如果 updateLog 为空，提供默认值
    let updateLog = version.updateLog;
    if (updateLog === null || updateLog === undefined || String(updateLog).trim() === '') {
      updateLog = '## 🎉 更新内容\n\n### ✨ 新功能\n- 性能优化\n- Bug修复\n- 体验提升';
      console.warn('[WARN] updateLog 为空，使用默认内容');
    }

    const responseData = {
      versionCode: version.versionCode,
      versionName: version.versionName,
      downloadUrl: downloadUrl,
      updateLog: updateLog,
      publishTime: version.publishTime
    };

    res.json(ok(responseData));

  } catch (e) {
    console.error(e);
    res.status(500).json(
      err('Database error: ' + e.message, 500)
    );
  }
});

/**
 * 音乐播放
 */
app.get('/api/song/play', async (req, res) => {
  try {
    const validation = validateId(req.query.id);
    if (!validation.valid) {
      return res.status(400).json(err(validation.error));
    }

    const [rows] = await pool.query(
      'SELECT url FROM songs WHERE id = ?',
      [validation.value]
    );

    if (rows.length === 0) {
      return res.status(404).json(
        err('Not Found', 404)
      );
    }

    const song = rows[0];

    if (
      song.url.startsWith('http://') ||
      song.url.startsWith('https://')
    ) {
      return res.redirect(song.url);
    }

    let filePath = song.url;

    if (filePath.startsWith('/musicplayer/')) {
      filePath = filePath.replace('/musicplayer/', '');
    }

    const pathValidation = validateFilePath(filePath, RESOURCE_PATH);
    if (!pathValidation.valid) {
      console.error('Path traversal attempt:', filePath);
      return res.status(403).json(
        err('Access denied', 403)
      );
    }

    filePath = pathValidation.path;

    if (!fs.existsSync(filePath)) {
      return res.status(404).json(
        err('File not found', 404)
      );
    }

    const stat = fs.statSync(filePath);

    if (!stat.isFile()) {
      return res.status(400).json(
        err('Invalid file', 400)
      );
    }

    const fileSize = stat.size;
    const range = req.headers.range;

    if (range) {
      const parts = range
        .replace(/bytes=/, '')
        .split('-');

      const start = parseInt(parts[0], 10);

      if (isNaN(start) || start < 0 || start >= fileSize) {
        return res.status(416).json(
          err('Invalid range', 416)
        );
      }

      const end = parts[1]
        ? parseInt(parts[1], 10)
        : fileSize - 1;

      if (isNaN(end) || end < start || end >= fileSize) {
        return res.status(416).json(
          err('Invalid range', 416)
        );
      }

      const chunkSize = end - start + 1;

      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunkSize,
        'Content-Type': 'audio/mpeg'
      });

      fs
        .createReadStream(filePath, {
          start,
          end
        })
        .pipe(res);

    } else {

      res.writeHead(200, {
        'Content-Length': fileSize,
        'Content-Type': 'audio/mpeg'
      });

      fs
        .createReadStream(filePath)
        .pipe(res);
    }

  } catch (e) {
    console.error(e);

    res.status(500).json(
      err('Server error: ' + e.message, 500)
    );
  }
});

function validateEmail(email) {
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return re.test(email);
}

function validateUsername(username) {
  if (!username || username.length < 3 || username.length > 50) {
    return false;
  }
  const re = /^[a-zA-Z0-9_]+$/;
  return re.test(username);
}

function validatePassword(password) {
  return password && password.length >= 6 && password.length <= 100;
}

function generateToken(userId) {
  return jwt.sign({ userId }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
}

async function verifyToken(token) {
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    return { valid: true, userId: decoded.userId };
  } catch (e) {
    return { valid: false, error: e.message };
  }
}

async function authenticateToken(req, res, next) {
  const authHeader = req.headers.authorization;
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json(err('Unauthorized: No token provided', 401));
  }

  const result = await verifyToken(token);
  if (!result.valid) {
    return res.status(401).json(err('Unauthorized: Invalid token', 401));
  }

  req.userId = result.userId;
  next();
}

async function ensureSongExists(songId) {
  const [rows] = await pool.query('SELECT id FROM songs WHERE id = ? LIMIT 1', [songId]);
  return rows.length > 0;
}

async function getUserLibrarySnapshot(userId) {
  const [favoriteRows] = await pool.query(
    `
    SELECT song_id
    FROM user_favorites
    WHERE user_id = ?
    ORDER BY created_at DESC, song_id DESC
    `,
    [userId]
  );

  const [recentRows] = await pool.query(
    `
    SELECT
      song_id AS songId,
      UNIX_TIMESTAMP(MAX(played_at)) * 1000 AS timestamp
    FROM user_recent_plays
    WHERE user_id = ?
      AND played_at >= DATE_SUB(NOW(), INTERVAL ? DAY)
    GROUP BY song_id
    ORDER BY MAX(played_at) DESC
    `,
    [userId, RECENT_PLAY_WINDOW_DAYS]
  );

  const [countRows] = await pool.query(
    `
    SELECT
      (SELECT COUNT(*) FROM user_favorites WHERE user_id = ?) AS favoriteCount,
      (
        SELECT COUNT(DISTINCT song_id)
        FROM user_recent_plays
        WHERE user_id = ?
          AND played_at >= DATE_SUB(NOW(), INTERVAL ? DAY)
      ) AS recentCount,
      (
        SELECT COUNT(*)
        FROM user_recent_plays
        WHERE user_id = ?
      ) AS totalPlayCount
    `,
    [userId, userId, RECENT_PLAY_WINDOW_DAYS, userId]
  );

  const counts = countRows[0] || {};

  return {
    favoriteSongIds: favoriteRows.map(row => row.song_id),
    recentPlays: recentRows.map(row => ({
      songId: row.songId,
      timestamp: Number(row.timestamp) || 0
    })),
    favoriteCount: Number(counts.favoriteCount) || 0,
    recentCount: Number(counts.recentCount) || 0,
    totalPlayCount: Number(counts.totalPlayCount) || 0
  };
}

app.post('/api/user/register', async (req, res) => {
  try {
    const { username, email, password } = req.body;

    if (!username || !email || !password) {
      return res.status(400).json(err('Username, email, and password are required'));
    }

    if (!validateUsername(username)) {
      return res.status(400).json(err('Invalid username: must be 3-50 characters, only letters, numbers, and underscores'));
    }

    if (!validateEmail(email)) {
      return res.status(400).json(err('Invalid email format'));
    }

    if (!validatePassword(password)) {
      return res.status(400).json(err('Password must be 6-100 characters'));
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const [usernameCheck] = await pool.query('SELECT id FROM users WHERE username = ?', [username]);
    if (usernameCheck.length > 0) {
      return res.status(400).json(err('Username already exists'));
    }

    const [emailCheck] = await pool.query('SELECT id FROM users WHERE email = ?', [email]);
    if (emailCheck.length > 0) {
      return res.status(400).json(err('Email already registered'));
    }

    const [result] = await pool.query(
      'INSERT INTO users (username, email, password, nickname) VALUES (?, ?, ?, ?)',
      [username, email, hashedPassword, username]
    );

    const userId = result.insertId;
    const token = generateToken(userId);

    res.json(ok({
      userId,
      username,
      email,
      nickname: username,
      avatar: '/resource/img/default_avatar.jpg',
      gender: null,
      birthday: null,
      token
    }));

  } catch (e) {
    console.error('Registration error:', e);
    res.status(500).json(err('Registration failed: ' + e.message, 500));
  }
});

app.post('/api/user/login', async (req, res) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      return res.status(400).json(err('Username and password are required'));
    }

    const [rows] = await pool.query(
      'SELECT id, username, email, password, nickname, avatar, status, gender, birthday FROM users WHERE username = ? OR email = ?',
      [username, username]
    );

    if (rows.length === 0) {
      return res.status(401).json(err('账号不存在，请先注册', 401));
    }

    const user = rows[0];

    if (user.status !== 1) {
      return res.status(401).json(err('账号已被禁用，请联系管理员', 401));
    }

    const isValidPassword = await bcrypt.compare(password, user.password);

    if (!isValidPassword) {
      return res.status(401).json(err('密码错误，请重试', 401));
    }

    await pool.query(
      'UPDATE users SET last_login_at = CURRENT_TIMESTAMP WHERE id = ?',
      [user.id]
    );

    const token = generateToken(user.id);

    res.json(ok({
      userId: user.id,
      username: user.username,
      email: user.email,
      nickname: user.nickname,
      avatar: fixPhoto(user.avatar),
      gender: user.gender,
      birthday: formatDate(user.birthday),
      token
    }));

  } catch (e) {
    console.error('Login error:', e);
    res.status(500).json(err('Login failed: ' + e.message, 500));
  }
});

app.get('/api/user/profile', authenticateToken, async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT id, username, email, nickname, avatar, phone, gender, birthday, created_at FROM users WHERE id = ?',
      [req.userId]
    );

    if (rows.length === 0) {
      return res.status(404).json(err('User not found', 404));
    }

    const user = rows[0];

    res.json(ok({
      userId: user.id,
      username: user.username,
      email: user.email,
      nickname: user.nickname,
      avatar: fixPhoto(user.avatar),
      phone: user.phone,
      gender: user.gender,
      birthday: formatDate(user.birthday),
      createdAt: user.created_at
    }));

  } catch (e) {
    console.error('Get profile error:', e);
    res.status(500).json(err('Failed to get profile: ' + e.message, 500));
  }
});

app.get('/api/user/library', authenticateToken, async (req, res) => {
  try {
    const snapshot = await getUserLibrarySnapshot(req.userId);
    res.json(ok(snapshot));
  } catch (e) {
    console.error('Get user library error:', e);
    res.status(500).json(err('Failed to get user library: ' + e.message, 500));
  }
});

app.post('/api/user/favorites', authenticateToken, async (req, res) => {
  try {
    const { songId, isFavorite } = req.body;
    const validation = validateId(songId);

    if (!validation.valid) {
      return res.status(400).json(err(validation.error));
    }

    if (typeof isFavorite !== 'boolean') {
      return res.status(400).json(err('isFavorite must be a boolean'));
    }

    const exists = await ensureSongExists(validation.value);
    if (!exists) {
      return res.status(404).json(err('Song not found', 404));
    }

    if (isFavorite) {
      await pool.query(
        `
        INSERT INTO user_favorites (user_id, song_id)
        VALUES (?, ?)
        ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP
        `,
        [req.userId, validation.value]
      );
    } else {
      await pool.query(
        'DELETE FROM user_favorites WHERE user_id = ? AND song_id = ?',
        [req.userId, validation.value]
      );
    }

    const snapshot = await getUserLibrarySnapshot(req.userId);
    res.json(ok({
      songId: validation.value,
      isFavorite,
      favoriteCount: snapshot.favoriteCount
    }));
  } catch (e) {
    console.error('Update favorites error:', e);
    res.status(500).json(err('Failed to update favorites: ' + e.message, 500));
  }
});

app.post('/api/user/recent-play', authenticateToken, async (req, res) => {
  try {
    const { songId, playedAt } = req.body;
    const validation = validateId(songId);

    if (!validation.valid) {
      return res.status(400).json(err(validation.error));
    }

    const exists = await ensureSongExists(validation.value);
    if (!exists) {
      return res.status(404).json(err('Song not found', 404));
    }

    const playDate = new Date(
      typeof playedAt === 'number' && playedAt > 0 ? playedAt : Date.now()
    );

    await pool.query(
      'INSERT INTO user_recent_plays (user_id, song_id, played_at) VALUES (?, ?, ?)',
      [req.userId, validation.value, playDate]
    );

    const snapshot = await getUserLibrarySnapshot(req.userId);
    res.json(ok({
      songId: validation.value,
      recentCount: snapshot.recentCount,
      totalPlayCount: snapshot.totalPlayCount
    }));
  } catch (e) {
    console.error('Add recent play error:', e);
    res.status(500).json(err('Failed to add recent play: ' + e.message, 500));
  }
});

app.delete('/api/user/recent-play/:songId', authenticateToken, async (req, res) => {
  try {
    const validation = validateId(req.params.songId);

    if (!validation.valid) {
      return res.status(400).json(err(validation.error));
    }

    await pool.query(
      'DELETE FROM user_recent_plays WHERE user_id = ? AND song_id = ?',
      [req.userId, validation.value]
    );

    const snapshot = await getUserLibrarySnapshot(req.userId);
    res.json(ok({
      songId: validation.value,
      recentCount: snapshot.recentCount,
      totalPlayCount: snapshot.totalPlayCount
    }));
  } catch (e) {
    console.error('Delete recent play error:', e);
    res.status(500).json(err('Failed to delete recent play: ' + e.message, 500));
  }
});

app.put('/api/user/profile', authenticateToken, async (req, res) => {
  try {
    const { nickname, phone, avatar, gender, birthday, email } = req.body;

    const updates = [];
    const params = [];

    if (nickname && nickname.length > 0 && nickname.length <= 50) {
      updates.push('nickname = ?');
      params.push(nickname);
    }

    if (email && validateEmail(email)) {
      updates.push('email = ?');
      params.push(email);
    }

    if (phone !== undefined) {
      updates.push('phone = ?');
      params.push(phone);
    }

    if (avatar) {
      updates.push('avatar = ?');
      params.push(avatar);
    }

    if (gender !== undefined) {
      updates.push('gender = ?');
      params.push(gender);
    }

    if (birthday !== undefined) {
      let formattedBirthday = birthday;
      if (birthday.includes('T')) {
        formattedBirthday = birthday.split('T')[0];
      }
      updates.push('birthday = ?');
      params.push(formattedBirthday);
    }

    if (updates.length === 0) {
      return res.status(400).json(err('No valid fields to update'));
    }

    params.push(req.userId);

    await pool.query(
      `UPDATE users SET ${updates.join(', ')} WHERE id = ?`,
      params
    );

    const [rows] = await pool.query(
      'SELECT id, username, email, nickname, avatar, phone, gender, birthday FROM users WHERE id = ?',
      [req.userId]
    );

    const user = rows[0];

    res.json(ok({
      userId: user.id,
      username: user.username,
      email: user.email,
      nickname: user.nickname,
      avatar: fixPhoto(user.avatar),
      phone: user.phone,
      gender: user.gender,
      birthday: formatDate(user.birthday)
    }));

  } catch (e) {
    console.error('Update profile error:', e);
    res.status(500).json(err('Failed to update profile: ' + e.message, 500));
  }
});

app.post('/api/user/change-password', authenticateToken, async (req, res) => {
  try {
    const { newPassword } = req.body;

    if (!newPassword) {
      return res.status(400).json(err('New password is required'));
    }

    if (!validatePassword(newPassword)) {
      return res.status(400).json(err('New password must be 6-100 characters'));
    }

    const hashedPassword = await bcrypt.hash(newPassword, 10);

    await pool.query('UPDATE users SET password = ? WHERE id = ?', [hashedPassword, req.userId]);

    res.json(ok({ message: 'Password changed successfully' }));

  } catch (e) {
    console.error('Change password error:', e);
    res.status(500).json(err('Failed to change password: ' + e.message, 500));
  }
});

app.post('/api/user/delete', authenticateToken, async (req, res) => {
  try {
    const userId = req.userId;

    // 这里可以执行物理删除，或者逻辑删除（status=0）
    // 为了彻底注销，通常可以物理删除或者清空个人敏感信息

    // 物理删除
    await pool.query('DELETE FROM users WHERE id = ?', [userId]);

    // 如果有其他关联表（如播放记录、收藏等），建议也一并清理或级联删除
    // await pool.query('DELETE FROM favorite_songs WHERE user_id = ?', [userId]);

    res.json(ok({ message: 'Account deleted successfully' }));

  } catch (e) {
    console.error('Delete account error:', e);
    res.status(500).json(err('Failed to delete account: ' + e.message, 500));
  }
});

app.post('/api/user/upload-avatar', authenticateToken, async (req, res) => {
  try {
    const { image } = req.body;
    if (!image) {
      return res.status(400).json(err('未提供图片数据'));
    }

    console.log(`Uploading avatar for user ${req.userId}, image length: ${image.length}`);

    // 处理 base64
    const matches = image.match(/^data:([A-Za-z-+\/]+);base64,(.+)$/);
    let buffer;
    if (matches && matches.length === 3) {
      buffer = Buffer.from(matches[2], 'base64');
    } else {
      // 尝试直接作为 base64 处理
      buffer = Buffer.from(image, 'base64');
    }

    if (!buffer || buffer.length === 0) {
      return res.status(400).json(err('图片数据无效'));
    }

    const filename = `avatar_${req.userId}.jpg`;
    // 使用 avatar 目录存储头像
    const relativePath = `/avatar/${filename}`;
    const fullPath = path.join(RESOURCE_PATH, relativePath);

    // 确保目录存在
    const dir = path.dirname(fullPath);
    if (!fs.existsSync(dir)) {
      console.log(`Creating directory: ${dir}`);
      fs.mkdirSync(dir, { recursive: true });
    }

    // 直接写入会覆盖旧文件
    fs.writeFileSync(fullPath, buffer);

    // 返回 /resource/avatar/xxx.jpg 格式，与静态资源映射保持一致
    res.json(ok({
      url: `/resource${relativePath}`
    }));

  } catch (e) {
    console.error('Upload error:', e);
    res.status(500).json(err('上传失败: ' + e.message, 500));
  }
});

/**
 * 静态页面
 */
app.use(express.static(path.join(__dirname, '.')));

app.use((err, req, res, next) => {
  console.error('Unhandled error:', {
    message: err.message,
    stack: err.stack,
    url: req.url,
    method: req.method,
    timestamp: new Date().toISOString()
  });

  if (err.name === 'UnauthorizedError') {
    return res.status(401).json({
      code: 401,
      message: 'Unauthorized access'
    });
  }

  if (err.name === 'ValidationError') {
    return res.status(400).json({
      code: 400,
      message: 'Validation error: ' + err.message
    });
  }

  if (err.code === 'LIMIT_FILE_SIZE') {
    return res.status(413).json({
      code: 413,
      message: 'File too large'
    });
  }

  if (err.code === 'ECONNREFUSED') {
    return res.status(503).json({
      code: 503,
      message: 'Service unavailable: database connection failed'
    });
  }

  if (err.code === 'ETIMEDOUT') {
    return res.status(504).json({
      code: 504,
      message: 'Gateway timeout'
    });
  }

  res.status(500).json({
    code: 500,
    message: 
    'Internal Server Error: ' + err.message
  });
});

app.use((req, res) => {
  res.status(404).json({
    code: 404,
    message: 'Not Found: ' + req.method + ' ' + req.url
  });
});

/**
 * 启动服务
 */
async function startServer() {
  try {

    const conn = await pool.getConnection();

    console.log('MySQL connected successfully');

    conn.release();

    app.listen(PORT, '0.0.0.0', () => {

      console.log(
        `MusicPlayer Server running at https://0.0.0.0:${PORT}`
      );

      console.log(
        `API endpoint: http://8.162.14.195:${PORT}/api`
      );

      console.log(
        `Resource path: ${RESOURCE_PATH}`
      );
    });

  } catch (e) {

    console.error(
      'Failed to connect to MySQL:',
      e.message
    );

    process.exit(1);
  }
}

startServer();
