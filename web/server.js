const express = require('express');
const path = require('path');
const mysql = require('mysql2/promise');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(express.json());

const corsHeaders = (req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Range');

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
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 3306,
  user: process.env.DB_USER || 'xqf',
  password: process.env.DB_PASSWORD || '123456',
  database: process.env.DB_NAME || 'musicplayer',
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

  return photo.replace('/musicplayer', '/resource');
}

const ok = (data) => ({
  code: 0,
  data
});

const err = (msg, code = 400) => ({
  code,
  message: msg
});

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
    const page = parseInt(req.query.page) || 1;
    const size = parseInt(req.query.size) || 10;

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
      ORDER BY created_at DESC
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
      ORDER BY created_at DESC
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
    const id = req.query.id;

    if (!id) {
      return res.status(400).json(
        err('Missing id')
      );
    }

    const [rows] = await pool.query(
      'SELECT * FROM songs WHERE id = ?',
      [id]
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

    if (!keyword) {
      return res.status(400).json(
        err('Missing query')
      );
    }

    const page = parseInt(req.query.page) || 1;
    const size = parseInt(req.query.size) || 10;

    const offset = (page - 1) * size;

    const kw = `%${keyword}%`;

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
        OR LOWER(artist) LIKE ?
      ORDER BY created_at DESC
      LIMIT ? OFFSET ?
      `,
      [kw, kw, kw, kw, size, offset]
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
 * 音乐播放
 */
app.get('/api/song/play', async (req, res) => {
  try {
    const id = req.query.id;

    if (!id) {
      return res.status(400).json(
        err('Missing id')
      );
    }

    const [rows] = await pool.query(
      'SELECT url FROM songs WHERE id = ?',
      [id]
    );

    if (rows.length === 0) {
      return res.status(404).json(
        err('Not Found', 404)
      );
    }

    const song = rows[0];

    /**
     * 外链音频
     */
    if (
      song.url.startsWith('http://') ||
      song.url.startsWith('https://')
    ) {
      return res.redirect(song.url);
    }

    /**
     * 本地文件
     */
    let filePath = song.url;

    if (filePath.startsWith('/musicplayer/')) {
      filePath = filePath.replace('/musicplayer/', '');
    }

    filePath = path.join(RESOURCE_PATH, filePath);

    if (!fs.existsSync(filePath)) {
      return res.status(404).json(
        err('File not found', 404)
      );
    }

    const stat = fs.statSync(filePath);

    const fileSize = stat.size;
    const range = req.headers.range;

    /**
     * Range 播放
     */
    if (range) {
      const parts = range
        .replace(/bytes=/, '')
        .split('-');

      const start = parseInt(parts[0], 10);

      const end = parts[1]
        ? parseInt(parts[1], 10)
        : fileSize - 1;

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

      /**
       * 普通播放
       */
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

/**
 * 静态页面
 */
app.use(express.static(path.join(__dirname, '.')));

/**
 * 全局错误
 */
app.use((err, req, res, next) => {
  console.error(err);

  res.status(500).json({
    code: 500,
    message: 'Internal Server Error'
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
        `API endpoint: https://localhost:${PORT}/api`
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