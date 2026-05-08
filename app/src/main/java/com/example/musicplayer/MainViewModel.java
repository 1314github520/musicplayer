package com.example.musicplayer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private SongDao songDao;
    private RecentPlayDao recentPlayDao;
    private SharedPreferences prefs;

    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private MutableLiveData<Long> duration = new MutableLiveData<>(0L);
    private MutableLiveData<Long> currentPosition = new MutableLiveData<>(0L);
    private MutableLiveData<Long> seekToPosition = new MutableLiveData<>(-1L);
    private MutableLiveData<String> songTitle = new MutableLiveData<>("未知歌曲");
    private MutableLiveData<String> songArtist = new MutableLiveData<>("未知歌手");
    private MutableLiveData<String> coverUrl = new MutableLiveData<>("");
    private MutableLiveData<Integer> currentSongId = new MutableLiveData<>(-1);
    private MutableLiveData<Integer> loopMode = new MutableLiveData<>(0); // 0: List Loop, 1: Single Loop
    private MutableLiveData<Long> nextRequest = new MutableLiveData<>(0L);
    private MutableLiveData<Long> prevRequest = new MutableLiveData<>(0L);

    private LiveData<Integer> importedCount;
    private LiveData<Integer> downloadedCount;
    private LiveData<Integer> recentCount;
    private LiveData<Integer> favoriteCount;

    // User Profile Data
    private MutableLiveData<String> userNickname = new MutableLiveData<>();
    private MutableLiveData<String> userSignature = new MutableLiveData<>();
    private MutableLiveData<String> userAvatarUri = new MutableLiveData<>();
    private MutableLiveData<String> userGender = new MutableLiveData<>();
    private MutableLiveData<String> userBirthday = new MutableLiveData<>();
    private MutableLiveData<String> userId = new MutableLiveData<>("1876006358");

    private MutableLiveData<List<Song>> remoteSongs = new MutableLiveData<>();
    private MutableLiveData<List<Song>> searchResults = new MutableLiveData<>(new ArrayList<>());
    private static final String BASE_URL = "https://hello.584399.xyz";
    
    // 用于管理异步任务的ExecutorService
    private java.util.concurrent.ExecutorService executorService;

    public MainViewModel(Application application) {
        super(application);
        // 初始化ExecutorService
        executorService = java.util.concurrent.Executors.newFixedThreadPool(4);
        
        AppDatabase db = AppDatabase.getInstance(application);
        songDao = db.songDao();
        recentPlayDao = db.recentPlayDao();
        prefs = application.getSharedPreferences("user_profile", Context.MODE_PRIVATE);

        loadProfile();

        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        
        importedCount = songDao.getImportedSongsCount();
        downloadedCount = songDao.getDownloadedSongsCount();
        recentCount = recentPlayDao.getRecentCount(sevenDaysAgo);
        favoriteCount = songDao.getFavoriteSongsCount();
        
        fetchRemoteSongs();
        initDummyData();
    }

    public LiveData<List<Song>> getRemoteSongs() { return remoteSongs; }
    public LiveData<List<Song>> getSearchResults() { return searchResults; }

    public void fetchRemoteSongs() {
        executorService.execute(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                okhttp3.Request request = new okhttp3.Request.Builder().url(BASE_URL + "/songs?size=100").build();
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<ApiResponse<List<RemoteSong>>>(){}.getType();
                        ApiResponse<List<RemoteSong>> apiResponse = new com.google.gson.Gson().fromJson(json, type);
                        List<Song> songs = convertToSongs(apiResponse.data);
                        remoteSongs.postValue(songs);
                        
                        // 智能插入：只插入新歌曲，更新已存在歌曲的信息
                        if (songs != null && !songs.isEmpty()) {
                            for (Song song : songs) {
                                Song existing = songDao.getSongById(song.id);
                                if (existing == null) {
                                    // 新歌曲，直接插入
                                    songDao.insert(song);
                                } else {
                                    // 已存在的歌曲，只更新基本信息，保留用户数据
                                    songDao.updateSongInfo(
                                        song.id, 
                                        song.title, 
                                        song.artist, 
                                        song.singer,
                                        song.coverUrl, 
                                        song.album, 
                                        song.duration
                                    );
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    public void searchSongs(String query) {
        searchSongs(query, 1, 50); // 默认第一页，每页50条
    }
    
    public void searchSongs(String query, int page, int size) {
        executorService.execute(() -> {
            try {
                // 优先从本地数据库搜索
                SongDao songDao = AppDatabase.getInstance(getApplication()).songDao();
                String searchPattern = "%" + query.toLowerCase() + "%";
                List<Song> localResults = songDao.searchSongsLocal(searchPattern);
                
                if (localResults != null && !localResults.isEmpty()) {
                    // 本地有结果，直接使用
                    android.util.Log.d("MainViewModel", "从本地数据库搜索到 " + localResults.size() + " 首歌曲");
                    searchResults.postValue(localResults);
                } else {
                    // 本地没有结果，从网络搜索
                    android.util.Log.d("MainViewModel", "本地无结果，从网络搜索: " + query);
                    okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder()
                        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                        .build();
                    String url = BASE_URL + "/song/search?q=" + java.net.URLEncoder.encode(query, "UTF-8") 
                               + "&page=" + page + "&size=" + size;
                    okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
                    try (okhttp3.Response response = client.newCall(request).execute()) {
                        if (response.isSuccessful() && response.body() != null) {
                            String json = response.body().string();
                            android.util.Log.d("MainViewModel", "网络搜索响应: " + json);
                            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<ApiResponse<List<RemoteSong>>>(){}.getType();
                            ApiResponse<List<RemoteSong>> apiResponse = new com.google.gson.Gson().fromJson(json, type);
                            List<Song> results = convertToSongs(apiResponse.data);
                            android.util.Log.d("MainViewModel", "网络搜索到 " + (results != null ? results.size() : 0) + " 首歌曲");
                            searchResults.postValue(results != null ? results : new ArrayList<>());
                        } else {
                            // 网络请求失败，返回空列表
                            android.util.Log.d("MainViewModel", "网络搜索失败: " + response.code());
                            searchResults.postValue(new ArrayList<>());
                        }
                    } catch (Exception e) {
                        android.util.Log.e("MainViewModel", "网络搜索异常", e);
                        searchResults.postValue(new ArrayList<>());
                    }
                }
            } catch (Exception e) { 
                android.util.Log.e("MainViewModel", "搜索失败", e);
                // 发生异常时，返回空列表
                searchResults.postValue(new ArrayList<>());
            }
        });
    }

    /**
     * 获取所有歌曲并更新本地数据库（增量更新）
     * 只更新有变化的歌曲（比较lrc_id等字段）
     */
    public void syncAllSongs() {
        executorService.execute(() -> {
            try {
                okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
                String url = BASE_URL + "/songs/all";
                okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
                
                try (okhttp3.Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<ApiResponse<List<RemoteSong>>>(){}.getType();
                        ApiResponse<List<RemoteSong>> apiResponse = new com.google.gson.Gson().fromJson(json, type);
                        
                        if (apiResponse.data != null && !apiResponse.data.isEmpty()) {
                            SongDao songDao = AppDatabase.getInstance(getApplication()).songDao();
                            int newSongsCount = 0;
                            int updatedSongsCount = 0;
                            
                            for (RemoteSong rs : apiResponse.data) {
                                Song existing = songDao.getSongById(rs.id);
                                
                                String playUrl = BASE_URL + "/song/play?id=" + rs.id;
                                String photoUrl = rs.photo != null ? rs.photo.trim() : null;
                                
                                if (existing == null) {
                                    // 新歌曲，直接插入
                                    Song newSong = new Song(rs.title, rs.artist, playUrl, photoUrl, false);
                                    newSong.id = rs.id;
                                    newSong.singer = (rs.singer != null && !rs.singer.isEmpty()) ? rs.singer : rs.artist;
                                    newSong.album = (rs.album != null && !rs.album.isEmpty()) ? rs.album : "Unknown Album";
                                    newSong.duration = rs.duration;
                                    newSong.lrcId = rs.lrc_id;
                                    songDao.insert(newSong);
                                    newSongsCount++;
                                } else {
                                    // 已存在的歌曲，检查是否需要更新
                                    boolean needUpdate = false;
                                    
                                    // 比较lrc_id是否有变化
                                    if (rs.lrc_id != null && !rs.lrc_id.equals(existing.lrcId)) {
                                        needUpdate = true;
                                    }
                                    
                                    // 比较其他字段是否有变化
                                    if (!rs.title.equals(existing.title) ||
                                        !rs.artist.equals(existing.artist) ||
                                        (rs.singer != null && !rs.singer.equals(existing.singer)) ||
                                        (rs.album != null && !rs.album.equals(existing.album)) ||
                                        rs.duration != existing.duration) {
                                        needUpdate = true;
                                    }
                                    
                                    if (needUpdate) {
                                        // 更新歌曲信息，但保留用户数据（isFavorite, isLocal等）
                                        existing.title = rs.title;
                                        existing.artist = rs.artist;
                                        existing.singer = (rs.singer != null && !rs.singer.isEmpty()) ? rs.singer : rs.artist;
                                        existing.coverUrl = photoUrl;
                                        existing.album = (rs.album != null && !rs.album.isEmpty()) ? rs.album : "Unknown Album";
                                        existing.duration = rs.duration;
                                        existing.lrcId = rs.lrc_id;
                                        songDao.updateSong(existing);
                                        updatedSongsCount++;
                                    }
                                }
                            }
                            
                            android.util.Log.d("MainViewModel", "同步完成: 新增 " + newSongsCount + " 首, 更新 " + updatedSongsCount + " 首");
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("MainViewModel", "同步歌曲失败", e);
            }
        });
    }

    private List<Song> convertToSongs(List<RemoteSong> rawList) {
        List<Song> songs = new java.util.ArrayList<>();
        if (rawList == null) return songs;
        for (RemoteSong rs : rawList) {
            String playUrl = BASE_URL + "/song/play?id=" + rs.id;
            // trim处理photo字段，去除可能的空格
            String photoUrl = rs.photo != null ? rs.photo.trim() : null;
            Song s = new Song(rs.title, rs.artist, playUrl, photoUrl, false);
            s.id = rs.id;
            s.singer = (rs.singer != null && !rs.singer.isEmpty()) ? rs.singer : rs.artist;
            s.album = (rs.album != null && !rs.album.isEmpty()) ? rs.album : "Unknown Album";
            s.duration = rs.duration;
            s.lrcId = rs.lrc_id;  // 设置歌词ID
            songs.add(s);
        }
        return songs;
    }

    private static class ApiResponse<T> {
        int code;
        T data;
    }

    private static class RemoteSong {
        int id;
        String title;
        String artist;  // 作曲家，用于搜索歌词
        String singer;  // 歌手，用于显示
        String photo;
        String album;
        int duration;
        Integer lrc_id;  // lrclib API返回的歌词ID
    }

    private void loadProfile() {
        userNickname.setValue(prefs.getString("nickname", "音乐爱好者"));
        userSignature.setValue(prefs.getString("signature", "愿音乐治愈所有的不开心 ✨"));
        userAvatarUri.setValue(prefs.getString("avatar", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&h=200&fit=crop"));
        userGender.setValue(prefs.getString("gender", "男"));
        userBirthday.setValue(prefs.getString("birthday", "填写生日，可以收到酷狗生日礼券哦~"));
    }

    public void saveProfile(String nickname, String signature, String avatar, String gender, String birthday) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("nickname", nickname);
        editor.putString("signature", signature);
        editor.putString("avatar", avatar);
        editor.putString("gender", gender);
        editor.putString("birthday", birthday);
        editor.apply();

        userNickname.setValue(nickname);
        userSignature.setValue(signature);
        userAvatarUri.setValue(avatar);
        userGender.setValue(gender);
        userBirthday.setValue(birthday);
    }

    public LiveData<String> getUserNickname() { return userNickname; }
    public void setUserNickname(String nickname) { userNickname.setValue(nickname); }
    public LiveData<String> getUserSignature() { return userSignature; }
    public void setUserSignature(String signature) { userSignature.setValue(signature); }
    public LiveData<String> getUserAvatarUri() { return userAvatarUri; }
    public void setUserAvatarUri(String uri) { userAvatarUri.setValue(uri); }
    public LiveData<String> getUserGender() { return userGender; }
    public void setUserGender(String gender) { userGender.setValue(gender); }
    public LiveData<String> getUserBirthday() { return userBirthday; }
    public void setUserBirthday(String birthday) { userBirthday.setValue(birthday); }
    public LiveData<String> getUserId() { return userId; }

    public LiveData<List<Song>> getRecentSongs() {
        long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
        return recentPlayDao.getRecentSongs(sevenDaysAgo);
    }

    public void addToRecent(int songId) {
        executorService.execute(() -> {
            // 清理7天前的记录
            long sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);
            recentPlayDao.deleteOldRecords(sevenDaysAgo);
            
            // 添加新的播放记录
            recentPlayDao.deleteBySongId(songId);
            recentPlayDao.insert(new RecentPlay(songId, System.currentTimeMillis()));
        });
    }
    
    public void deleteRecentPlay(int songId) {
        executorService.execute(() -> {
            recentPlayDao.deleteBySongId(songId);
        });
    }

    public LiveData<Integer> getCurrentSongId() { return currentSongId; }
    public void setCurrentSongId(int id) { currentSongId.setValue(id); }
    public List<Song> getAllSongsSync() { return songDao.getAllSongsSync(); }

    private void initDummyData() {
        // No longer scanning raw since we've moved to remote assets
    }

    public LiveData<Integer> getImportedCount() { return importedCount; }
    public LiveData<Integer> getDownloadedCount() { return downloadedCount; }
    public LiveData<Integer> getRecentCount() { return recentCount; }
    public LiveData<Integer> getFavoriteCount() { return favoriteCount; }
    public LiveData<Integer> getTotalPlayCount() { return recentPlayDao.getTotalPlayCount(); }
    public LiveData<List<Song>> getLocalSongs() { return songDao.getLocalSongs(); }
    public LiveData<List<Song>> getImportedSongs() { return songDao.getImportedSongs(); }
    public LiveData<List<Song>> getDownloadedSongs() { return songDao.getDownloadedSongs(); }
    public LiveData<List<Song>> getFavoriteSongs() { return songDao.getFavoriteSongs(); }
    public void insertSongs(List<Song> songs) { executorService.execute(() -> songDao.insertSongs(songs)); }
    public void updateSong(Song song) { executorService.execute(() -> songDao.updateSong(song)); }
    public void deleteSong(Song song) { executorService.execute(() -> songDao.delete(song)); }
    public LiveData<Long> getDuration() { return duration; }
    public void setDuration(long d) { duration.setValue(d); }
    public LiveData<Long> getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(long p) { currentPosition.setValue(p); }
    public LiveData<Long> getSeekToPosition() { return seekToPosition; }
    public void setSeekToPosition(long p) { seekToPosition.setValue(p); }
    public void clearSeek() { seekToPosition.setValue(-1L); }
    public LiveData<String> getSongTitle() { return songTitle; }
    public void setSongTitle(String t) { songTitle.setValue(t); }
    public LiveData<String> getSongArtist() { return songArtist; }
    public void setSongArtist(String a) { songArtist.setValue(a); }
    public LiveData<String> getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String c) { coverUrl.setValue(c); }

    public LiveData<Integer> getLoopMode() { return loopMode; }
    public void toggleLoopMode() {
        Integer current = loopMode.getValue();
        if (current == null) current = 0;
        loopMode.setValue((current + 1) % 2); // Switch between 0 and 1
    }

    public LiveData<Long> getNextRequest() { return nextRequest; }
    public void playNext() {
        nextRequest.setValue(System.currentTimeMillis());
    }

    public LiveData<Long> getPrevRequest() { return prevRequest; }
    public void playPrevious() {
        prevRequest.setValue(System.currentTimeMillis());
    }

    private MutableLiveData<List<LyricEntry>> lyrics = new MutableLiveData<>();
    private MutableLiveData<String> currentLyric = new MutableLiveData<>("");
    public LiveData<Boolean> getIsPlaying() { return isPlaying; }
    public void setIsPlaying(boolean playing) { isPlaying.setValue(playing); }
    public LiveData<List<LyricEntry>> getLyrics() { return lyrics; }
    public void setLyrics(List<LyricEntry> l) { lyrics.setValue(l); }
    public LiveData<String> getCurrentLyric() { return currentLyric; }
    public void setCurrentLyric(String l) { currentLyric.setValue(l); }
    
    public void cancelAllTasks() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
    
    public void restartExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = java.util.concurrent.Executors.newFixedThreadPool(4);
        }
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
