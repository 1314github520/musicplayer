export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, OPTIONS",
      "Access-Control-Allow-Headers": "*"
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    const ok = (data) =>
      new Response(JSON.stringify({ code: 0, data }), {
        headers: { "Content-Type": "application/json", ...corsHeaders }
      });

    const err = (msg, code = 400) =>
      new Response(JSON.stringify({ code, message: msg }), {
        status: code,
        headers: { "Content-Type": "application/json", ...corsHeaders }
      });

    const formatTime = (sec) => {
      if (!sec && sec !== 0) return null;
      const m = Math.floor(sec / 60);
      const s = sec % 60;
      return `${m}:${s.toString().padStart(2, "0")}`;
    };

    try {

      // 🎵 1. 歌曲列表
      if (path === "/songs") {
        const page = parseInt(url.searchParams.get("page") || "1");
        const size = parseInt(url.searchParams.get("size") || "10");
        const offset = (page - 1) * size;

        const result = await env.songs.prepare(`
          SELECT id, title, artist, singer, photo, duration, album, lrc_id
          FROM songs
          ORDER BY created_at
          LIMIT ? OFFSET ?
        `).bind(size, offset).all();

        const data = result.results.map(s => ({
          ...s,
          duration_text: formatTime(s.duration)
        }));

        return ok(data);
      }

      // 🎵 1.5. 获取所有歌曲（用于缓存）
      if (path === "/songs/all") {
        const result = await env.songs.prepare(`
          SELECT id, title, artist, singer, photo, duration, album, lrc_id
          FROM songs
          ORDER BY created_at
        `).all();

        const data = result.results.map(s => ({
          ...s,
          duration_text: formatTime(s.duration)
        }));

        return ok(data);
      }

      // 🎵 2. 歌曲详情
      if (path === "/song/detail") {
        const id = url.searchParams.get("id");
        if (!id) return err("Missing id");

        const song = await env.songs.prepare(`
          SELECT *
          FROM songs
          WHERE id = ?
        `).bind(id).first();

        if (!song) return err("Not Found", 404);

        song.duration_text = formatTime(song.duration);

        return ok(song);
      }

      // 🔍 3. 搜索（支持 title,singer,album）
      if (path === "/song/search") {
        const keyword = url.searchParams.get("q");
        if (!keyword) return err("Missing query");

        const page = parseInt(url.searchParams.get("page") || "1");
        const size = parseInt(url.searchParams.get("size") || "10");
        const offset = (page - 1) * size;

        const kw = `%${keyword.toLowerCase()}%`;

        const result = await env.songs.prepare(`
          SELECT id, title, artist, singer, photo, duration, album, lrc_id
          FROM songs
          WHERE LOWER(title) LIKE ?
             OR LOWER(singer) LIKE ?
             OR LOWER(album) LIKE ?
          ORDER BY created_at DESC
          LIMIT ? OFFSET ?
        `)
        .bind(kw, kw, kw, size, offset)
        .all();

        const data = result.results.map(s => ({
          ...s,
          duration_text: formatTime(s.duration)
        }));

        return ok(data);
      }

      // 🎧 4. 播放（透传 + Range 支持）
      if (path === "/song/play") {
        const id = url.searchParams.get("id");
        if (!id) return err("Missing id");

        const song = await env.songs.prepare(`
          SELECT url
          FROM songs
          WHERE id = ?
        `).bind(id).first();

        if (!song) return err("Not Found", 404);

        const range = request.headers.get("Range");

        const response = await fetch(song.url, {
          headers: range ? { Range: range } : {}
        });

        const headers = new Headers(response.headers);
        headers.set("Access-Control-Allow-Origin", "*");

        return new Response(response.body, {
          status: response.status,
          headers
        });
      }

      // 📜 5. 通过lrc_id获取歌词
      if (path === "/lrc/get") {
        const lrcId = url.searchParams.get("id");
        if (!lrcId) return err("Missing lrc_id");

        try {
          const response = await fetch(`https://lrclib.net/api/get/${lrcId}`, {
            headers: {
              "User-Agent": "MusicPlayer/1.0",
              "Accept": "application/json"
            }
          });

          if (!response.ok) {
            return err("Lyrics not found", 404);
          }

          const lrcData = await response.json();
          
          return ok({
            id: lrcData.id,
            title: lrcData.trackName,
            artist: lrcData.artistName,
            album: lrcData.albumName,
            duration: lrcData.duration,
            plainLyrics: lrcData.plainLyrics,
            syncedLyrics: lrcData.syncedLyrics
          });
        } catch (e) {
          return err("Failed to fetch lyrics: " + e.message, 500);
        }
      }

      // 🔄 6. 检查应用更新
      if (path === "/app/version") {
        const version = await env.songs.prepare(`
          SELECT versionCode, versionName, downloadUrl, updateLog, publishTime
          FROM version
          ORDER BY id DESC
          LIMIT 1
        `).first();

        if (!version) {
          return err("No version found", 404);
        }

        return ok(version);
      }

      return err("Not Found", 404);

    } catch (e) {
      return err("Server Error: " + e.message, 500);
    }
  }
};