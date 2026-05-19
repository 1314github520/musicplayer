package com.example.musicplayer.feature.main;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.musicplayer.core.Constants;
import com.example.musicplayer.core.network.HttpClient;
import com.example.musicplayer.data.local.AppDatabase;
import com.example.musicplayer.data.local.RecentPlayDao;
import com.example.musicplayer.data.local.SongDao;
import com.example.musicplayer.data.model.LyricEntry;
import com.example.musicplayer.data.model.RecentPlay;
import com.example.musicplayer.data.model.Song;
import com.example.musicplayer.data.model.User;
import com.example.musicplayer.feature.auth.UserManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainViewModel extends AndroidViewModel {
    public static final String BASE_URL = "http://8.162.14.195:3000";

    private SongDao songDao;
    private RecentPlayDao recentPlayDao;
    public ExecutorService executorService = Executors.newFixedThreadPool(4);

    // 音乐列表数据
    private final MutableLiveData<List<Song>> remoteSongs = new MutableLiveData<>();
    private final MutableLiveData<List<Song>> searchResults = new MutableLiveData<>();
    
    // 当前播放状态
    private final MutableLiveData<Integer> currentSongId = new MutableLiveData<>();
    private final MutableLiveData<String> songTitle = new MutableLiveData<>("未知歌曲");
    private final MutableLiveData<String> songArtist = new MutableLiveData<>("未知歌手");
    private final MutableLiveData<String> coverUrl = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<Long> currentPosition = new MutableLiveData<>(0L);
    private final MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private final MutableLiveData<Integer> loopMode = new MutableLiveData<>(0); // 0:列表循环 1:单曲循环
    
    // 歌词数据
    private final MutableLiveData<List<LyricEntry>> lyrics = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> currentLyric = new MutableLiveData<>("");

    // 控制请求
    private final MutableLiveData<Long> seekToPosition = new MutableLiveData<>(-1L);
    private final MutableLiveData<Integer> togglePlaybackRequest = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> nextRequest = new MutableLiveData<>(0);
    private final MutableLiveData<Integer> prevRequest = new MutableLiveData<>(0);

    // 用户资料
    private final MutableLiveData<String> userNickname = new MutableLiveData<>();
    private final MutableLiveData<String> userSignature = new MutableLiveData<>();
    private final MutableLiveData<String> userId = new MutableLiveData<>("ID: 20240001");
    private final MutableLiveData<String> userGender = new MutableLiveData<>();
    private final MutableLiveData<String> userBirthday = new MutableLiveData<>();
    private final MutableLiveData<String> userAvatarUri = new MutableLiveData<>();
    private final MutableLiveData<Integer> totalPlayCount = new MutableLiveData<>(0);

    // 数据同步状态（用于解决重新登录后收藏数据不显示的问题）
    private final MutableLiveData<Boolean> isDataSynced = new MutableLiveData<>(false);
    private volatile boolean syncInProgress = false;
    private volatile boolean networkSyncCompleted = false; // 标记网络同步是否已完成，防止本地数据覆盖网络数据

    public MainViewModel(@NonNull Application application) {
        super(application);
        try {
            AppDatabase db = AppDatabase.getInstance(application);
            songDao = db.songDao();
            recentPlayDao = db.recentPlayDao();
            refreshProfile();
        } catch (Exception e) {
            android.util.Log.e("MainViewModel", "初始化数据库失败", e);
        }
    }

    // --- Getter 方法 ---
    public LiveData<List<Song>> getRemoteSongs() { return remoteSongs; }
    public LiveData<List<Song>> getSearchResults() { return searchResults; }
    public LiveData<Integer> getCurrentSongId() { return currentSongId; }
    public LiveData<String> getSongTitle() { return songTitle; }
    public LiveData<String> getSongArtist() { return songArtist; }
    public LiveData<String> getCoverUrl() { return coverUrl; }
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public LiveData<Long> getCurrentPosition() { return currentPosition; }
    public LiveData<Long> getDuration() { return duration; }
    public LiveData<Integer> getLoopMode() { return loopMode; }
    public LiveData<List<LyricEntry>> getLyrics() { return lyrics; }
    public LiveData<String> getCurrentLyric() { return currentLyric; }
    public LiveData<Long> getSeekToPosition() { return seekToPosition; }
    public LiveData<Integer> getTogglePlaybackRequest() { return togglePlaybackRequest; }
    public LiveData<Integer> getNextRequest() { return nextRequest; }
    public LiveData<Integer> getPrevRequest() { return prevRequest; }
    public LiveData<String> getUserNickname() { return userNickname; }
    public LiveData<String> getUserSignature() { return userSignature; }
    public LiveData<String> getUserId() { return userId; }
    public LiveData<String> getUserGender() { return userGender; }
    public LiveData<String> getUserBirthday() { return userBirthday; }
    public LiveData<String> getUserAvatarUri() { return userAvatarUri; }

    // --- 计数 LiveData ---
    public LiveData<Integer> getFavoriteCount() { return songDao.getFavoriteSongsCount(); }
    public LiveData<Integer> getDownloadedCount() { return songDao.getDownloadedSongsCount(); }
    public LiveData<Integer> getImportedCount() { return songDao.getImportedSongsCount(); }
    public LiveData<Integer> getRecentCount() { 
        return recentPlayDao.getRecentCount(System.currentTimeMillis() - 604800000L); 
    }
    public LiveData<Integer> getTotalPlayCount() { return totalPlayCount; }
    
    // --- 数据同步状态 ---
    public LiveData<Boolean> isDataSynced() { return isDataSynced; }
    public boolean isSyncInProgress() { return syncInProgress; }

    // --- 列表查询 ---
    public LiveData<List<Song>> getLocalSongs() { return songDao.getLocalSongs(); }
    public LiveData<List<Song>> getFavoriteSongs() { return songDao.getFavoriteSongs(); }
    public LiveData<List<Song>> getImportedSongs() { return songDao.getImportedSongs(); }
    public LiveData<List<Song>> getDownloadedSongs() { return songDao.getDownloadedSongs(); }
    public LiveData<List<Song>> getRecentSongs() { 
        return recentPlayDao.getRecentSongs(System.currentTimeMillis() - 604800000L); 
    }

    // --- 状态 Setter ---
    public void setCurrentSongId(int id) { currentSongId.setValue(id); }
    public void setSongTitle(String title) { songTitle.setValue(title); }
    public void setSongArtist(String artist) { songArtist.setValue(artist); }
    public void setCoverUrl(String url) { coverUrl.setValue(url); }
    public void setIsPlaying(boolean playing) { isPlaying.setValue(playing); }
    public void setCurrentPosition(long pos) { currentPosition.postValue(pos); }
    public void setDuration(long dur) { duration.postValue(dur); }
    public void setLyrics(List<LyricEntry> list) { lyrics.setValue(list); }
    public void setCurrentLyric(String text) { currentLyric.postValue(text); }
    public void setSeekToPosition(long pos) { seekToPosition.setValue(pos); }
    public void clearSeek() { seekToPosition.setValue(-1L); }
    public void requestTogglePlayback() {
        Integer current = togglePlaybackRequest.getValue();
        togglePlaybackRequest.setValue(current == null ? 1 : current + 1);
    }
    public void consumeTogglePlaybackRequest() { togglePlaybackRequest.setValue(0); }
    public void playNext() {
        Integer current = nextRequest.getValue();
        nextRequest.setValue(current == null ? 1 : current + 1);
    }
    public void clearNextRequest() { nextRequest.setValue(0); }
    public void playPrevious() {
        Integer current = prevRequest.getValue();
        prevRequest.setValue(current == null ? 1 : current + 1);
    }
    public void clearPrevRequest() { prevRequest.setValue(0); }
    
    public void toggleLoopMode() {
        int nextMode = (loopMode.getValue() != null ? loopMode.getValue() + 1 : 1) % 2;
        loopMode.setValue(nextMode);
    }

    // --- 数据库操作 ---
    public void updateSong(Song song) {
        if (song == null) return;
        executorService.execute(() -> {
            try { songDao.updateSong(song); } catch (Exception e) { android.util.Log.e("MainViewModel", "Update failed", e); }
        });
    }

    public void insertSongs(List<Song> songs) {
        if (songs == null || songs.isEmpty()) return;
        executorService.execute(() -> {
            try { songDao.insertSongs(songs); } catch (Exception e) { android.util.Log.e("MainViewModel", "Insert failed", e); }
        });
    }

    public void deleteSong(Song song) {
        if (song == null) return;
        executorService.execute(() -> {
            try { songDao.delete(song); } catch (Exception e) { android.util.Log.e("MainViewModel", "Delete failed", e); }
        });
    }

    public void addToRecent(int songId) {
        executorService.execute(() -> {
            try {
                long currentTime = System.currentTimeMillis();
                recentPlayDao.insert(new RecentPlay(songId, currentTime));
                recentPlayDao.deleteOldRecords(currentTime - 604800000L);
                incrementTotalPlayCount();
                syncRecentPlayToServer(songId, currentTime);
            } catch (Exception e) { android.util.Log.e("MainViewModel", "Add recent failed", e); }
        });
    }

    public void deleteRecentPlay(int songId) {
        executorService.execute(() -> {
            try {
                recentPlayDao.deleteBySongId(songId);
                deleteRecentPlayFromServer(songId);
            } catch (Exception e) { android.util.Log.e("MainViewModel", "Delete recent play failed", e); }
        });
    }

    public void syncFavoriteToServer(int songId, boolean isFavorite) {
        UserManager userManager = UserManager.getInstance(getApplication());
        String token = userManager.getToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        executorService.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("songId", songId);
                body.put("isFavorite", isFavorite);

                Request request = new Request.Builder()
                        .url(Constants.API.BASE_URL + "/api/user/favorites")
                        .header("Authorization", "Bearer " + token)
                        .post(RequestBody.create(
                                body.toString(),
                                MediaType.parse("application/json")
                        ))
                        .build();

                try (Response response = HttpClient.getInstance().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        android.util.Log.w("MainViewModel", "收藏同步失败: HTTP " + response.code());
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("MainViewModel", "Sync favorite failed", e);
            }
        });
    }

    // --- 网络同步与缓存 ---
    public void fetchRemoteSongs() {
        syncInProgress = true;
        isDataSynced.setValue(false);
        executorService.execute(() -> {
            try {
                android.util.Log.d("MainViewModel", "Fetching from: " + BASE_URL);
                okhttp3.OkHttpClient client = HttpClient.getInstance();
                okhttp3.Request request = new okhttp3.Request.Builder().url(BASE_URL + "/api/songs?size=100").build();
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<ApiResponse<List<RemoteSong>>>(){}.getType();
                        ApiResponse<List<RemoteSong>> apiResponse = new com.google.gson.Gson().fromJson(json, type);
                        if (apiResponse != null && apiResponse.data != null) {
                            List<Song> songs = convertToSongs(apiResponse.data);
                            songDao.mergeRemoteSongs(songs);
                            syncUserLibraryCache();
                            remoteSongs.postValue(songDao.getAllSongsSync());
                        }
                    }
                }
                // 标记同步完成
                syncInProgress = false;
                isDataSynced.postValue(true);
            } catch (Exception e) { 
                android.util.Log.e("MainViewModel", "Fetch failed", e); 
                syncInProgress = false;
                isDataSynced.postValue(false);
            }
        });
    }

    public void syncAllSongs() {
        syncInProgress = true;
        isDataSynced.setValue(false);
        networkSyncCompleted = false; // 重置标志
        
        // 第1步：先立即加载本地数据到UI，避免白屏
        executorService.execute(() -> {
            try {
                android.util.Log.d("MainViewModel", "syncAllSongs - 第1步：加载本地数据");
                List<Song> localSongs = songDao.getAllSongsSync();
                if (localSongs != null && !localSongs.isEmpty()) {
                    android.util.Log.d("MainViewModel", "本地数据库有 " + localSongs.size() + " 首歌曲，先显示到UI");
                    // 只有在网络同步未完成时才更新UI，防止覆盖新数据
                    if (!networkSyncCompleted) {
                        android.util.Log.d("MainViewModel", "第1步 - 网络未完成，更新UI: " + localSongs.size() + " 首");
                        remoteSongs.postValue(localSongs);
                    } else {
                        android.util.Log.d("MainViewModel", "第1步 - 网络已完成，跳过更新，避免覆盖");
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("MainViewModel", "加载本地数据失败", e);
            }
        });
        
        // 第2步：后台同步网络数据
        executorService.execute(() -> {
            try {
                android.util.Log.d("MainViewModel", "syncAllSongs - 第2步：同步网络数据");
                okhttp3.OkHttpClient client = HttpClient.getInstance();
                okhttp3.Request request = new okhttp3.Request.Builder().url(BASE_URL + "/api/songs/all").build();
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<ApiResponse<List<RemoteSong>>>(){}.getType();
                        ApiResponse<List<RemoteSong>> apiResponse = new com.google.gson.Gson().fromJson(json, type);
                        if (apiResponse != null && apiResponse.data != null) {
                            android.util.Log.d("MainViewModel", "syncAllSongs - 收到网络数据: " + apiResponse.data.size() + " 首");
                            List<Song> songs = convertToSongs(apiResponse.data);
                            android.util.Log.d("MainViewModel", "syncAllSongs - 转换后歌曲数: " + songs.size() + " 首");
                            songDao.mergeRemoteSongs(songs);
                            syncUserLibraryCache();
                            // 标记网络同步完成
                            networkSyncCompleted = true;
                            android.util.Log.d("MainViewModel", "网络数据同步完成，更新UI");
                            android.util.Log.d("MainViewModel", "syncAllSongs - 发送到UI的歌曲数: " + songs.size() + " 首");
                            remoteSongs.postValue(songDao.getAllSongsSync()); // 更新UI显示最新数据
                        }
                    }
                }
                syncInProgress = false;
                isDataSynced.postValue(true);
            } catch (Exception e) { 
                android.util.Log.e("MainViewModel", "Sync failed", e); 
                syncInProgress = false;
                isDataSynced.postValue(false);
            }
        });
    }

    public void searchSongs(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchResults.postValue(new ArrayList<>());
            return;
        }
        executorService.execute(() -> {
            try {
                List<Song> local = songDao.searchSongsLocal("%" + query.toLowerCase().trim() + "%");
                searchResults.postValue(local != null ? local : new ArrayList<>());
            } catch (Exception e) { searchResults.postValue(new ArrayList<>()); }
        });
    }

    private List<Song> convertToSongs(List<RemoteSong> rawList) {
        List<Song> songs = new ArrayList<>();
        if (rawList == null) return songs;
        for (RemoteSong rs : rawList) {
            String playUrl = BASE_URL + "/api/song/play?id=" + rs.id;
            String photoUrl = rs.photo != null ? rs.photo.trim() : null;
            if (photoUrl != null && photoUrl.startsWith("/")) photoUrl = BASE_URL + photoUrl;
            Song s = new Song(rs.title, rs.artist, playUrl, photoUrl, false);
            s.id = rs.id;
            s.singer = (rs.singer != null && !rs.singer.isEmpty()) ? rs.singer : rs.artist;
            s.album = (rs.album != null && !rs.album.isEmpty()) ? rs.album : "Unknown Album";
            s.duration = rs.duration;
            s.lrcId = rs.lrc_id;
            songs.add(s);
        }
        android.util.Log.d("MainViewModel", "convertToSongs - 转换完成，共 " + songs.size() + " 首");
        if (!songs.isEmpty()) {
            android.util.Log.d("MainViewModel", "第一首: id=" + songs.get(0).id + " title=" + songs.get(0).title);
            android.util.Log.d("MainViewModel", "最后一首: id=" + songs.get(songs.size()-1).id + " title=" + songs.get(songs.size()-1).title);
        }
        return songs;
    }

    public void refreshProfile() {
        UserManager userManager = UserManager.getInstance(getApplication());
        User user = userManager.getCurrentUser();
        if (user != null) {
            String nickname = (user.getNickname() != null && !user.getNickname().isEmpty()) ? user.getNickname() : "听乐人";
            String avatar = user.getAvatar() != null ? user.getAvatar() : "";
            String gender = (user.getGender() != null && !user.getGender().isEmpty()) ? user.getGender() : "保密";
            String birthday = (user.getBirthday() != null && !user.getBirthday().isEmpty()) ? user.getBirthday() : "未设置";
            String idStr = "ID: " + user.getUserId();
            
            SharedPreferences sp = getApplication().getSharedPreferences("user_profile", Context.MODE_PRIVATE);
            String sig = sp.getString("signature", "音乐让生活更美好");
            
            userId.setValue(idStr);
            saveProfile(nickname, sig, avatar, gender, birthday);
        } else {
            userNickname.setValue("未登录");
            userSignature.setValue("");
            userAvatarUri.setValue("");
            userGender.setValue("");
            userBirthday.setValue("");
            userId.setValue("");
            totalPlayCount.setValue(0);
        }
    }

    private void loadProfile() {
        SharedPreferences sp = getApplication().getSharedPreferences("user_profile", Context.MODE_PRIVATE);
        userNickname.setValue(sp.getString("nickname", "听乐人"));
        userSignature.setValue(sp.getString("signature", "音乐让生活更美好"));
        userGender.setValue(sp.getString("gender", "男"));
        userBirthday.setValue(sp.getString("birthday", "2000-01-01"));
        userAvatarUri.setValue(sp.getString("avatar", ""));
    }

    public void saveProfile(String name, String sig, String avatar, String gender, String birthday) {
        SharedPreferences sp = getApplication().getSharedPreferences("user_profile", Context.MODE_PRIVATE);
        sp.edit().putString("nickname", name)
                .putString("signature", sig)
                .putString("avatar", avatar)
                .putString("gender", gender)
                .putString("birthday", birthday)
                .apply();
        userNickname.setValue(name);
        userSignature.setValue(sig);
        userAvatarUri.setValue(avatar);
        userGender.setValue(gender);
        userBirthday.setValue(birthday);
    }
    
    public void setUserAvatarUri(String uri) {
        userAvatarUri.setValue(uri);
    }

    public void clearProfile() {
        userNickname.setValue("未登录");
        userSignature.setValue("");
        userAvatarUri.setValue("");
        userGender.setValue("");
        userBirthday.setValue("");
        userId.setValue("");
        totalPlayCount.setValue(0);
    }

    public void cancelAllTasks() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
    
    public void restartExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newFixedThreadPool(4);
        }
    }

    private void syncUserLibraryCache() {
        UserManager userManager = UserManager.getInstance(getApplication());
        String token = userManager.getToken();
        if (token == null || token.isEmpty()) {
            totalPlayCount.postValue(0);
            return;
        }

        try {
            Request request = new Request.Builder()
                    .url(Constants.API.BASE_URL + "/api/user/library")
                    .header("Authorization", "Bearer " + token)
                    .build();

            try (Response response = HttpClient.getInstance().newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    android.util.Log.w("MainViewModel", "用户音乐数据同步失败: HTTP " + response.code());
                    return;
                }

                JSONObject json = new JSONObject(response.body().string());
                if (json.optInt("code", -1) != 0) {
                    android.util.Log.w("MainViewModel", "用户音乐数据同步失败: " + json.optString("message"));
                    return;
                }

                JSONObject data = json.optJSONObject("data");
                if (data == null) {
                    return;
                }

                JSONArray favoriteSongIds = data.optJSONArray("favoriteSongIds");
                JSONArray recentPlays = data.optJSONArray("recentPlays");

                songDao.clearFavorites();
                if (favoriteSongIds != null) {
                    for (int i = 0; i < favoriteSongIds.length(); i++) {
                        songDao.setFavoriteStatus(favoriteSongIds.optInt(i, -1), true);
                    }
                }

                recentPlayDao.deleteAll();
                if (recentPlays != null) {
                    for (int i = 0; i < recentPlays.length(); i++) {
                        JSONObject item = recentPlays.optJSONObject(i);
                        if (item == null) continue;
                        int songId = item.optInt("songId", -1);
                        long timestamp = item.optLong("timestamp", 0L);
                        if (songId > 0 && timestamp > 0L) {
                            recentPlayDao.insert(new RecentPlay(songId, timestamp));
                        }
                    }
                }

                totalPlayCount.postValue(data.optInt("totalPlayCount", 0));
            }
        } catch (Exception e) {
            android.util.Log.e("MainViewModel", "Sync user library failed", e);
        }
    }

    private void syncRecentPlayToServer(int songId, long playedAt) {
        UserManager userManager = UserManager.getInstance(getApplication());
        String token = userManager.getToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            JSONObject body = new JSONObject();
            body.put("songId", songId);
            body.put("playedAt", playedAt);

            Request request = new Request.Builder()
                    .url(Constants.API.BASE_URL + "/api/user/recent-play")
                    .header("Authorization", "Bearer " + token)
                    .post(RequestBody.create(
                            body.toString(),
                            MediaType.parse("application/json")
                    ))
                    .build();

            try (Response response = HttpClient.getInstance().newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    android.util.Log.w("MainViewModel", "最近播放同步失败: HTTP " + response.code());
                    return;
                }

                JSONObject json = new JSONObject(response.body().string());
                JSONObject data = json.optJSONObject("data");
                if (data != null) {
                    totalPlayCount.postValue(data.optInt("totalPlayCount", totalPlayCount.getValue() != null ? totalPlayCount.getValue() : 0));
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainViewModel", "Sync recent play failed", e);
        }
    }

    private void deleteRecentPlayFromServer(int songId) {
        UserManager userManager = UserManager.getInstance(getApplication());
        String token = userManager.getToken();
        if (token == null || token.isEmpty()) {
            return;
        }

        try {
            Request request = new Request.Builder()
                    .url(Constants.API.BASE_URL + "/api/user/recent-play/" + songId)
                    .header("Authorization", "Bearer " + token)
                    .delete()
                    .build();

            try (Response response = HttpClient.getInstance().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    android.util.Log.w("MainViewModel", "删除最近播放同步失败: HTTP " + response.code());
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainViewModel", "Delete recent play from server failed", e);
        }
    }

    private void incrementTotalPlayCount() {
        Integer current = totalPlayCount.getValue();
        totalPlayCount.postValue((current != null ? current : 0) + 1);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    android.util.Log.w("MainViewModel", "ExecutorService did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static class ApiResponse<T> { public T data; }
    private static class RemoteSong { public int id; public String title; public String artist; public String singer; public String photo; public String album; public int duration; public Long lrc_id; }
}
