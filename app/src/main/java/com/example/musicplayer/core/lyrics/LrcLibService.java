package com.example.musicplayer.core.lyrics;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import androidx.annotation.NonNull;
import com.example.musicplayer.core.network.HttpClient;
import com.example.musicplayer.data.model.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LrcLibService {

    private static final String TAG = "LrcLibService";
    private static final String API_GET = "https://lrclib.net/api/get";
    private static final String API_GET_CACHED = "https://lrclib.net/api/get-cached";
    private static final String API_SEARCH = "https://lrclib.net/api/search";
    
    private static final String USER_AGENT = "MusicPlayer/2.0 https://github.com/1314github520/musicplayer)";

    private final OkHttpClient client = HttpClient.getInstance();
    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final LruCache<String, String> cache = new LruCache<>(500);
    private final Map<String, List<LyricsCallback>> pendingMap = new HashMap<>();
    private LyricCacheManager persistentCache;

    public interface LyricsCallback {
        void onSuccess(String lrcContent, Long lrcId);
        void onError(String message);
    }

    static class LrcItem {
        long id;
        String trackName;
        String artistName;
        String albumName;
        double duration;
        boolean instrumental;
        String syncedLyrics;
        String plainLyrics;
    }
    
    public void init(Context context) {
        this.persistentCache = LyricCacheManager.getInstance(context);
        this.persistentCache.clearExpiredCache();
    }

    public void fetchLyrics(String title, String artist, String album, int duration, LyricsCallback callback) {
        if (title == null || title.trim().isEmpty()) {
            postError(callback, "Empty title");
            return;
        }

        String key = (title + "_" + artist + "_" + duration).toLowerCase();

        String cached = cache.get(key);
        if (cached != null) {
            postSuccess(callback, cached);
            return;
        }
        
        if (persistentCache != null) {
            String persistentCached = persistentCache.getLyric(key);
            if (persistentCached != null) {
                cache.put(key, persistentCached);
                postSuccess(callback, persistentCached);
                return;
            }
        }

        synchronized (pendingMap) {
            if (pendingMap.containsKey(key)) {
                pendingMap.get(key).add(callback);
                return;
            } else {
                List<LyricsCallback> list = new ArrayList<>();
                list.add(callback);
                pendingMap.put(key, list);
            }
        }

        // Phase 1: Try get-cached (fast, internal DB only)
        requestGet(API_GET_CACHED, title, artist, album, duration, key, true);
    }

    /**
     * Fetch lyrics by lrc_id directly from LRCLIB API.
     * This is the most accurate method when lrc_id is available.
     */
    public void fetchLyricsById(long lrcId, LyricsCallback callback) {
        String key = "lrc_id_" + lrcId;

        String cached = cache.get(key);
        if (cached != null) {
            postSuccess(callback, cached);
            return;
        }

        synchronized (pendingMap) {
            if (pendingMap.containsKey(key)) {
                pendingMap.get(key).add(callback);
                return;
            } else {
                List<LyricsCallback> list = new ArrayList<>();
                list.add(callback);
                pendingMap.put(key, list);
            }
        }

        String url = "https://lrclib.net/api/get/" + lrcId;
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Failed to fetch lyrics by ID: " + lrcId, e);
                mainHandler.post(() -> {
                    synchronized (pendingMap) {
                        List<LyricsCallback> callbacks = pendingMap.remove(key);
                        if (callbacks != null) {
                            for (LyricsCallback cb : callbacks) {
                                cb.onError("Network error: " + e.getMessage());
                            }
                        }
                    }
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (Response res = response) {
                    if (!res.isSuccessful() || res.body() == null) {
                        mainHandler.post(() -> {
                            synchronized (pendingMap) {
                                List<LyricsCallback> callbacks = pendingMap.remove(key);
                                if (callbacks != null) {
                                    for (LyricsCallback cb : callbacks) {
                                        cb.onError("Lyrics not found");
                                    }
                                }
                            }
                        });
                        return;
                    }

                    String json = res.body().string();
                    LrcItem item = gson.fromJson(json, LrcItem.class);
                    String lrc = extractLrc(item);

                    if (lrc != null) {
                        cache.put(key, lrc);
                        mainHandler.post(() -> {
                            synchronized (pendingMap) {
                                List<LyricsCallback> callbacks = pendingMap.remove(key);
                                if (callbacks != null) {
                                    for (LyricsCallback cb : callbacks) {
                                        cb.onSuccess(lrc, item.id);
                                    }
                                }
                            }
                        });
                    } else {
                        mainHandler.post(() -> {
                            synchronized (pendingMap) {
                                List<LyricsCallback> callbacks = pendingMap.remove(key);
                                if (callbacks != null) {
                                    for (LyricsCallback cb : callbacks) {
                                        cb.onError("No lyrics available");
                                    }
                                }
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing lyrics response", e);
                    mainHandler.post(() -> {
                        synchronized (pendingMap) {
                            List<LyricsCallback> callbacks = pendingMap.remove(key);
                            if (callbacks != null) {
                                for (LyricsCallback cb : callbacks) {
                                    cb.onError("Parse error: " + e.getMessage());
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    private void requestGet(String api, String title, String artist, String album,
                            int duration, String key, boolean fallbackToGet) {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(api).newBuilder()
                .addQueryParameter("track_name", title)
                .addQueryParameter("artist_name", artist)
                .addQueryParameter("duration", String.valueOf(duration));
        
        if (album != null && !album.isEmpty()) {
            urlBuilder.addQueryParameter("album_name", album);
        } else {
            urlBuilder.addQueryParameter("album_name", "");
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", USER_AGENT)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                handleFailure(api, title, artist, album, duration, key, fallbackToGet);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (Response res = response) {
                    if (res.isSuccessful() && res.body() != null) {
                        String json = res.body().string();
                        LrcItem item = gson.fromJson(json, LrcItem.class);
                        String lrc = extractLrc(item);
                        if (lrc != null) {
                            finishSuccess(key, lrc, item.id);
                            return;
                        }
                    }
                    handleFailure(api, title, artist, album, duration, key, fallbackToGet);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing GET response", e);
                    handleFailure(api, title, artist, album, duration, key, fallbackToGet);
                }
            }
        });
    }

    private void handleFailure(String api, String title, String artist, String album,
                              int duration, String key, boolean fallbackToGet) {
        if (api.equals(API_GET_CACHED) && fallbackToGet) {
            // Phase 2: Try get (triggers external search if needed)
            // 如果时长为0，get API 通常无法返回准确结果，可能直接尝试 search
            if (duration > 0) {
                requestGet(API_GET, title, artist, album, duration, key, false);
            } else {
                mainHandler.postDelayed(() -> performSearch(title, artist, album, duration, key, 0), 200);
            }
        } else {
            // Phase 3: Try search (fuzzy matching) with delay
            mainHandler.postDelayed(() -> performSearch(title, artist, album, duration, key, 0), 300);
        }
    }

    private void performSearch(String title, String artist, String album, int duration, String key, int depth) {
        if (depth > 2) {
            finishError(key, "Lyrics not found after search");
            return;
        }

        HttpUrl.Builder urlBuilder = HttpUrl.parse(API_SEARCH).newBuilder();
        
        if (depth == 0) {
            // Precise search with parameters
            urlBuilder.addQueryParameter("track_name", title);
            urlBuilder.addQueryParameter("artist_name", artist);
            if (album != null && !album.isEmpty()) {
                urlBuilder.addQueryParameter("album_name", album);
            }
        } else if (depth == 1) {
            // Keyword search
            urlBuilder.addQueryParameter("q", title + " " + artist);
        } else {
            // Minimalist search
            urlBuilder.addQueryParameter("q", title);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("User-Agent", USER_AGENT)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                finishError(key, e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                try (Response res = response) {
                    if (!res.isSuccessful() || res.body() == null) {
                        mainHandler.postDelayed(() -> 
                            performSearch(title, artist, album, duration, key, depth + 1), 1000);
                        return;
                    }

                    String json = res.body().string();
                    List<LrcItem> list = gson.fromJson(json, new TypeToken<List<LrcItem>>(){}.getType());

                    if (list == null || list.isEmpty()) {
                        mainHandler.postDelayed(() -> 
                            performSearch(title, artist, album, duration, key, depth + 1), 1000);
                        return;
                    }

                    LrcItem best = findBestMatch(list, title, artist, duration);
                    String lrc = extractLrc(best);

                    if (lrc != null) {
                        finishSuccess(key, lrc, best.id);
                    } else {
                        mainHandler.postDelayed(() -> 
                            performSearch(title, artist, album, duration, key, depth + 1), 1000);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing search response", e);
                    mainHandler.postDelayed(() -> 
                        performSearch(title, artist, album, duration, key, depth + 1), 1000);
                }
            }
        });
    }

    private LrcItem findBestMatch(List<LrcItem> list, String title, String artist, int targetDuration) {

        boolean userIsJapanese = hasJapanese(title) || hasJapanese(artist);

        LrcItem best = null;
        int bestScore = -1;

        for (LrcItem item : list) {

            int score = 0;

            // ❌ 过滤日文（如果用户输入中没有日文，而匹配项有日文，通常是匹配到了日文翻唱版）
            if (!userIsJapanese && (hasJapanese(item.trackName) || hasJapanese(item.artistName))) {
                continue;
            }

            // 🎯 标题匹配
            if (isSame(item.trackName, title)) score += 50;
            else if (containsIgnoreCase(item.trackName, title)) score += 20;

            // 🎤 歌手匹配
            if (isSame(item.artistName, artist)) score += 40;
            else if (containsIgnoreCase(item.artistName, artist)) score += 15;

            // ⏱ 时长匹配
            if (targetDuration > 0) {
                if (Math.abs(item.duration - targetDuration) <= 2) score += 30;
                else if (Math.abs(item.duration - targetDuration) <= 5) score += 15;
                else if (Math.abs(item.duration - targetDuration) > 15) score -= 20; // 时长差距过大减分
            } else {
                // 如果没有时长，稍微给带时长信息的项一点分数
                if (item.duration > 0) score += 5;
            }

            // 🎵 同步歌词优先
            if (item.syncedLyrics != null && !item.syncedLyrics.isEmpty()) score += 15;

            // 🎧 纯音乐处理
            if (item.instrumental) score += 5;

            if (score > bestScore) {
                bestScore = score;
                best = item;
            }
        }

        // 🔁 如果全被过滤（比如全是日文），再放开限制重新评分
        if (best == null || bestScore < 20) {
            for (LrcItem item : list) {
                int score = 0;
                if (isSame(item.trackName, title)) score += 40;
                if (isSame(item.artistName, artist)) score += 30;
                if (targetDuration > 0 && Math.abs(item.duration - targetDuration) <= 5) score += 20;
                if (item.syncedLyrics != null) score += 10;

                if (score > bestScore) {
                    bestScore = score;
                    best = item;
                }
            }
        }

        return best != null ? best : list.get(0);
    }

    private boolean containsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.toLowerCase().contains(b.toLowerCase());
    }

    private boolean hasJapanese(String text) {
        if (text == null) return false;
        // Check for Hiragana or Katakana
        return text.matches(".*[\\u3040-\\u309F\\u30A0-\\u30FF].*");
    }

    private boolean isSame(String s1, String s2) {
        if (s1 == null || s2 == null) return false;
        return s1.trim().equalsIgnoreCase(s2.trim());
    }

    private String extractLrc(LrcItem item) {
        if (item == null) return null;
        
        // Handle instrumental tracks explicitly
        if (item.instrumental) {
            return "[00:00.00] 纯音乐，请欣赏";
        }
        
        if (item.syncedLyrics != null && !item.syncedLyrics.trim().isEmpty()) {
            return item.syncedLyrics;
        }
        if (item.plainLyrics != null && !item.plainLyrics.trim().isEmpty()) {
            return item.plainLyrics;
        }
        return null;
    }

    private void finishSuccess(String key, String lrc, Long lrcId) {
        cache.put(key, lrc);
        if (persistentCache != null) {
            persistentCache.putLyric(key, lrc);
        }
        List<LyricsCallback> callbacks;
        synchronized (pendingMap) {
            callbacks = pendingMap.remove(key);
        }
        if (callbacks != null) {
            for (LyricsCallback cb : callbacks) {
                postSuccess(cb, lrc, lrcId);
            }
        }
    }

    private void finishError(String key, String msg) {
        List<LyricsCallback> callbacks;
        synchronized (pendingMap) {
            callbacks = pendingMap.remove(key);
        }
        if (callbacks != null) {
            for (LyricsCallback cb : callbacks) {
                postError(cb, msg);
            }
        }
    }

    private void postSuccess(LyricsCallback cb, String lrc) {
        postSuccess(cb, lrc, null);
    }
    
    private void postSuccess(LyricsCallback cb, String lrc, Long lrcId) {
        mainHandler.post(() -> cb.onSuccess(lrc, lrcId));
    }

    private void postError(LyricsCallback cb, String msg) {
        mainHandler.post(() -> cb.onError(msg));
    }
    
    /**
     * 取消所有待处理的歌词请求
     * 在Activity或Fragment销毁时调用，避免内存泄漏
     */
    public void cancelAll() {
        mainHandler.removeCallbacksAndMessages(null);
        synchronized (pendingMap) {
            pendingMap.clear();
        }
        client.dispatcher().cancelAll();
    }
}

