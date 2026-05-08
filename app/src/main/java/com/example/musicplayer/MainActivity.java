package com.example.musicplayer;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.example.musicplayer.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import coil.Coil;
import coil.request.ImageRequest;

public class MainActivity extends AppCompatActivity {

    private MediaController mediaController;
    private MainViewModel viewModel;
    private ImageView rotatingDisk;
    private android.widget.ProgressBar progressBar;
    private android.widget.ProgressBar importProgressBar;
    private ObjectAnimator diskAnimator;
    private static class ProgressHandler extends android.os.Handler {
        private final java.lang.ref.WeakReference<MainActivity> activityRef;
        
        ProgressHandler(MainActivity activity) {
            super(android.os.Looper.getMainLooper());
            activityRef = new java.lang.ref.WeakReference<>(activity);
        }
        
        void scheduleUpdate() {
            postDelayed(new UpdateRunnable(this), 1000);
        }
        
        void stopUpdates() {
            removeCallbacksAndMessages(null);
        }
        
        private static class UpdateRunnable implements Runnable {
            private final java.lang.ref.WeakReference<ProgressHandler> handlerRef;
            
            UpdateRunnable(ProgressHandler handler) {
                handlerRef = new java.lang.ref.WeakReference<>(handler);
            }
            
            @Override
            public void run() {
                ProgressHandler handler = handlerRef.get();
                if (handler != null) {
                    MainActivity activity = handler.activityRef.get();
                    if (activity != null && !activity.isFinishing()) {
                        activity.updateProgress();
                        handler.scheduleUpdate();
                    }
                }
            }
        }
    }
    
    private ProgressHandler progressHandler;
    private java.util.List<LyricEntry> lyricList;
    private android.widget.TextSwitcher lyricSwitcher;
    private View navContent;
    private LrcLibService lrcLibService = new LrcLibService();
    private boolean isInitialPlaylistSetup = true;
    private boolean isUserInitiatedPlay = false;
    private boolean hasUserPlayedSong = false; // 标记用户是否主动播放过歌曲
    private java.util.concurrent.ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        executorService = java.util.concurrent.Executors.newFixedThreadPool(4);
        progressHandler = new ProgressHandler(this);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        progressBar = findViewById(R.id.playbackProgress);
        importProgressBar = findViewById(R.id.importProgressBar);
        
        // 请求通知权限（Android 13+）
        requestNotificationPermission();
        
        // 同步所有歌曲到本地数据库（增量更新）
        viewModel.syncAllSongs();
        
        // 检查下载目录并更新本地音乐记录
        checkDownloadedSongs();
        
        setupLyricSwitcher();
        setupMiniPlayer();

        setupNavigation();
        setupRotatingDisk();
        initializeMediaController();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        // Default fragment
        if (savedInstanceState == null) {
            switchFragment(new DiscoveryFragment(), "discover");
            updateNavAndMiniPlayer(true);
        }
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
    }
    
    private void checkDownloadedSongs() {
        executorService.execute(() -> {
            java.io.File musicDir = getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC);
            if (musicDir == null || !musicDir.exists()) return;
            
            java.io.File[] files = musicDir.listFiles();
            if (files == null || files.length == 0) return;
            
            SongDao songDao = AppDatabase.getInstance(MainActivity.this).songDao();
            
            for (java.io.File file : files) {
                if (file.isFile() && file.getName().endsWith(".mp3")) {
                    // 从文件名获取歌曲标题（去掉.mp3后缀）
                    String fileName = file.getName();
                    String songTitle = fileName.substring(0, fileName.length() - 4);
                    
                    // 查找数据库中匹配的歌曲
                    List<Song> allSongs = songDao.getAllSongsSync();
                    for (Song song : allSongs) {
                        String sanitizedTitle = sanitizeFileName(song.title);
                        if (sanitizedTitle.equals(songTitle) && !song.isLocal) {
                            // 找到匹配的歌曲，更新为本地音乐
                            song.path = file.getAbsolutePath();
                            song.isLocal = true;
                            songDao.updateSong(song);
                            break;
                        }
                    }
                }
            }
        });
    }

    private final androidx.activity.result.ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    importMusicFiles(uris);
                }
            });

    public void openFilePicker() {
        filePickerLauncher.launch(new String[]{"audio/*"});
    }

    private void importMusicFiles(List<android.net.Uri> uris) {
        if (importProgressBar != null) {
            importProgressBar.setVisibility(View.VISIBLE);
            importProgressBar.setMax(uris.size());
            importProgressBar.setProgress(0);
        }

        executorService.execute(() -> {
            int current = 0;
            for (android.net.Uri uri : uris) {
                try {
                    // 持久化 URI 权限
                    try {
                        final int takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String title = "Unknown";
                    String artist = "本地导入";

                    // 使用 MediaMetadataRetriever 提取元数据
                    android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                    try {
                        retriever.setDataSource(this, uri);
                        String extractedTitle = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String extractedArtist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        
                        String album = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ALBUM);
                        String durationStr = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                        int duration = 0;
                        if (durationStr != null) {
                            duration = Integer.parseInt(durationStr) / 1000;
                        }

                        if (extractedTitle != null && !extractedTitle.isEmpty()) {
                            title = extractedTitle;
                        } else {
                            // 退回到文件名
                            android.database.Cursor cursor = null;
                            try {
                                cursor = getContentResolver().query(uri, null, null, null, null);
                                if (cursor != null && cursor.moveToFirst()) {
                                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                                    if (nameIndex != -1) {
                                        title = cursor.getString(nameIndex);
                                        if (title.contains(".")) {
                                            title = title.substring(0, title.lastIndexOf('.'));
                                        }
                                    }
                                }
                            } finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                        
                        if (extractedArtist != null && !extractedArtist.isEmpty()) {
                            artist = extractedArtist;
                        }

                        // 检查是否已经存在（可能是下载的音乐）
                        List<Song> existingSongs = viewModel.getLocalSongs().getValue();
                        Song existingSong = null;
                        if (existingSongs != null) {
                            for (Song s : existingSongs) {
                                if (s.title.equals(title) && s.artist.equals(artist)) {
                                    existingSong = s;
                                    break;
                                }
                            }
                        }

                        if (existingSong != null) {
                            // 已存在（下载的音乐），更新为本地导入
                            existingSong.path = uri.toString();
                            existingSong.isLocal = true;
                            existingSong.coverUrl = "android.resource://" + getPackageName() + "/" + R.drawable.music;
                            existingSong.album = album != null ? album : "Unknown Album";
                            existingSong.duration = duration;
                            viewModel.updateSong(existingSong);
                        } else {
                            // 新歌曲，插入，封面显示为music.png
                            Song localSong = new Song(title, artist, uri.toString(), 
                                "android.resource://" + getPackageName() + "/" + R.drawable.music, true);
                            localSong.album = album != null ? album : "Unknown Album";
                            localSong.duration = duration;
                            viewModel.insertSongs(java.util.Collections.singletonList(localSong));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try { retriever.release(); } catch (Exception ignored) {}
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                current++;
                final int finalCurrent = current;
                runOnUiThread(() -> {
                    if (importProgressBar != null) {
                        importProgressBar.setProgress(finalCurrent);
                    }
                });
            }
            runOnUiThread(() -> {
                if (importProgressBar != null) {
                    importProgressBar.setVisibility(View.GONE);
                }
                ToastHelper.showShort(this, "导入完成");
            });
        });
    }

    private ListenableFuture<MediaController> controllerFuture;

    private void setupMiniPlayer() {
        // 迷你播放器UI已删除，仅保留逻辑占位
    }

    private void updateNavAndMiniPlayer(boolean showMainUI) {
        View bottomNav = findViewById(R.id.bottomNav);
        View navDiscover = findViewById(R.id.navDiscover);
        View navPlayer = findViewById(R.id.navPlayer);
        View navMine = findViewById(R.id.navMine);

        if (showMainUI) {
            // 显示主界面底部导航
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
            if (navDiscover != null) navDiscover.setVisibility(View.VISIBLE);
            if (navPlayer != null) navPlayer.setVisibility(View.VISIBLE);
            if (navMine != null) navMine.setVisibility(View.VISIBLE);
        } else {
            // 全屏播放页，隐藏所有底部元素
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        }
    }

    private void setupNavigation() {
        View navDiscover = findViewById(R.id.navDiscover);
        View navMine = findViewById(R.id.navMine);
        ImageView imgDiscover = (ImageView) ((android.view.ViewGroup) navDiscover).getChildAt(0);
        TextView txtDiscover = (TextView) ((android.view.ViewGroup) navDiscover).getChildAt(1);
        ImageView imgMine = (ImageView) ((android.view.ViewGroup) navMine).getChildAt(0);
        TextView txtMine = (TextView) ((android.view.ViewGroup) navMine).getChildAt(1);

        navDiscover.setOnClickListener(v -> {
            switchFragment(new DiscoveryFragment(), "discover");
            updateNavUI(imgDiscover, txtDiscover, imgMine, txtMine, true);
        });

        navMine.setOnClickListener(v -> {
            switchFragment(new MineFragment(), "mine");
            updateNavUI(imgDiscover, txtDiscover, imgMine, txtMine, false);
        });
    }

    private void updateNavUI(ImageView imgDisc, TextView txtDisc, ImageView imgMine, TextView txtMine, boolean isDiscover) {
        int activeColor = ContextCompat.getColor(this, R.color.white);
        int inactiveColor = ContextCompat.getColor(this, R.color.text_grey);

        imgDisc.setColorFilter(isDiscover ? activeColor : inactiveColor);
        txtDisc.setTextColor(isDiscover ? activeColor : inactiveColor);
        imgMine.setColorFilter(isDiscover ? inactiveColor : activeColor);
        txtMine.setTextColor(isDiscover ? inactiveColor : activeColor);
    }

    public void playSongAt(int position) {
        if (mediaController != null && position >= 0 && position < mediaController.getMediaItemCount()) {
            // 标记用户主动播放歌曲
            hasUserPlayedSong = true;
            
            // 获取目标歌曲信息判断是否是本地导入音乐
            MediaItem item = mediaController.getMediaItemAt(position);
            int songId = mediaIdFromItem(item);
            
            // 立即清空歌词
            viewModel.setLyrics(new ArrayList<>());
            
            if (songId != -1) {
                new Thread(() -> {
                    Song song = AppDatabase.getInstance(MainActivity.this).songDao().getSongById(songId);
                    boolean isLocalImported = isLocalImportedSong(song);
                    runOnUiThread(() -> {
                        if (isLocalImported) {
                            // 本地导入音乐：显示music.png封面和暂无歌词
                            viewModel.setCoverUrl("android.resource://" + getPackageName() + "/" + R.drawable.music);
                            viewModel.setCurrentLyric("暂无歌词");
                            if (lyricSwitcher != null) {
                                lyricSwitcher.setText("暂无歌词");
                            }
                        } else {
                            // 在线音乐：显示搜索歌词中
                            viewModel.setCurrentLyric("搜索歌词中...");
                            if (lyricSwitcher != null) {
                                lyricSwitcher.setText("搜索歌词中...");
                            }
                        }
                    });
                }).start();
            } else {
                viewModel.setCurrentLyric("搜索歌词中...");
                if (lyricSwitcher != null) {
                    lyricSwitcher.setText("搜索歌词中...");
                }
            }
            
            mediaController.seekTo(position, 0);
            mediaController.play();
            updateCurrentSongIdFromController();
        }
    }

    private boolean isLocalImportedSong(Song song) {
        return song != null && song.isLocal && 
            (song.coverUrl == null || song.coverUrl.isEmpty() || 
             song.coverUrl.contains("music.png") || 
             song.coverUrl.startsWith("android.resource://"));
    }

    public void pauseMusic() {
        if (mediaController != null && mediaController.isPlaying()) {
            mediaController.pause();
            viewModel.setIsPlaying(false);
        }
    }

    public void pauseAllBackgroundTasks() {
        pauseMusic();
        if (lrcLibService != null) {
            lrcLibService.cancelAll();
        }
        if (viewModel != null) {
            viewModel.cancelAllTasks();
        }
        if (DownloadManager.getInstance(this) != null) {
            DownloadManager.getInstance(this).shutdown();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        android.util.Log.d("MainActivity", "所有后台任务已暂停");
    }
    
    public void resumeBackgroundTasks() {
        if (viewModel != null) {
            viewModel.restartExecutorService();
        }
        if (executorService == null || executorService.isShutdown()) {
            executorService = java.util.concurrent.Executors.newFixedThreadPool(4);
        }
        android.util.Log.d("MainActivity", "后台任务已恢复");
    }

    public void playSongList(List<Song> songs, int position) {
        if (mediaController == null || songs == null || songs.isEmpty()) return;

        // 标记用户主动播放歌曲
        hasUserPlayedSong = true;

        // 判断目标歌曲是否是本地导入音乐
        Song targetSong = songs.get(position);
        boolean isLocalImported = isLocalImportedSong(targetSong);
        
        // 立即清空歌词
        viewModel.setLyrics(new ArrayList<>());
        if (isLocalImported) {
            // 本地导入音乐：显示music.png封面和暂无歌词
            viewModel.setCoverUrl("android.resource://" + getPackageName() + "/" + R.drawable.music);
            viewModel.setCurrentLyric("暂无歌词");
            if (lyricSwitcher != null) {
                lyricSwitcher.setText("暂无歌词");
            }
        } else {
            // 在线音乐或下载的音乐：显示原有封面
            if (targetSong.coverUrl != null && !targetSong.coverUrl.isEmpty()) {
                viewModel.setCoverUrl(targetSong.coverUrl);
            } else {
                viewModel.setCoverUrl("android.resource://" + getPackageName() + "/" + R.drawable.music);
            }
            viewModel.setCurrentLyric("搜索歌词中...");
            if (lyricSwitcher != null) {
                lyricSwitcher.setText("搜索歌词中...");
            }
        }

        mediaController.stop();
        mediaController.clearMediaItems();
        for (Song song : songs) {
            mediaController.addMediaItem(createMediaItemFromSong(song));
        }
        mediaController.prepare();
        mediaController.seekTo(position, 0);
        mediaController.play();

        if (position < songs.size()) {
            viewModel.setCurrentSongId(songs.get(position).id);
        }
        showPlayerFragment();
        
        // 设置歌曲信息（显示歌手）
        viewModel.setSongTitle(targetSong.title);
        viewModel.setSongArtist(targetSong.singer != null ? targetSong.singer : targetSong.artist);
        
        // 如果是在线音乐，搜索歌词（使用artist作曲家字段）
        if (!isLocalImported && targetSong != null) {
            String searchArtist = targetSong.artist != null ? targetSong.artist : "Unknown";
            String searchTitle = targetSong.title.replaceAll("(?i)\\.mp3|\\.flac|\\.wav", "")
                                                .replaceAll("\\(.*?\\)|\\[.*?\\]", "").trim();
            
            int durationSeconds = targetSong.duration;
            if (durationSeconds < 0) durationSeconds = 0;
            
            lrcLibService.fetchLyrics(searchTitle, searchArtist, targetSong.album, durationSeconds, new LrcLibService.LyricsCallback() {
                @Override
                public void onSuccess(String lrcContent, Integer lrcId) {
                    runOnUiThread(() -> {
                        // 如果获取到了lrc_id，保存到数据库
                        if (lrcId != null && lrcId > 0 && targetSong.lrcId == null) {
                            new Thread(() -> {
                                targetSong.lrcId = lrcId;
                                AppDatabase.getInstance(MainActivity.this).songDao().updateSong(targetSong);
                                android.util.Log.d("MainActivity", "保存lrc_id到数据库: " + lrcId);
                            }).start();
                        }
                        
                        lyricList = LyricUtils.parseLrc(lrcContent);
                        if (lyricList.isEmpty()) {
                            loadNoLyricsState();
                        } else {
                            viewModel.setLyrics(lyricList);
                            if (!lyricList.isEmpty()) {
                                String firstLine = lyricList.get(0).text;
                                viewModel.setCurrentLyric(firstLine);
                                if (lyricSwitcher != null) {
                                    lyricSwitcher.setText(firstLine);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> loadNoLyricsState());
                }
            });
        }
    }

    public void playSong(Song song) {
        if (mediaController == null) return;

        // 标记用户主动播放歌曲
        hasUserPlayedSong = true;

        // 判断是否是本地导入音乐
        boolean isLocalImported = isLocalImportedSong(song);
        
        // 立即清空歌词
        viewModel.setLyrics(new ArrayList<>());
        if (isLocalImported) {
            // 本地导入音乐：显示music.png封面和暂无歌词
            viewModel.setCoverUrl("android.resource://" + getPackageName() + "/" + R.drawable.music);
            viewModel.setCurrentLyric("暂无歌词");
            if (lyricSwitcher != null) {
                lyricSwitcher.setText("暂无歌词");
            }
        } else {
            // 在线音乐：显示搜索歌词中
            viewModel.setCurrentLyric("搜索歌词中...");
            if (lyricSwitcher != null) {
                lyricSwitcher.setText("搜索歌词中...");
            }
        }

        int existingIndex = -1;
        for (int i = 0; i < mediaController.getMediaItemCount(); i++) {
            MediaItem item = mediaController.getMediaItemAt(i);
            if (item.mediaId.equals(String.valueOf(song.id))) {
                existingIndex = i;
                break;
            }
        }

        if (existingIndex != -1) {
            mediaController.seekTo(existingIndex, 0);
        } else {
            MediaItem mediaItem = createMediaItemFromSong(song);
            int nextIndex = Math.max(0, mediaController.getCurrentMediaItemIndex() + 1);
            mediaController.addMediaItem(nextIndex, mediaItem);
            mediaController.seekTo(nextIndex, 0);
        }
        
        mediaController.play();
        viewModel.setCurrentSongId(song.id);
        showPlayerFragment();
        
        // 设置歌曲信息（显示歌手）
        viewModel.setSongTitle(song.title);
        viewModel.setSongArtist(song.singer != null ? song.singer : song.artist);
        
        // 如果是在线音乐，搜索歌词（使用artist作曲家字段）
        if (!isLocalImported) {
            String searchArtist = song.artist != null ? song.artist : "Unknown";
            String searchTitle = song.title.replaceAll("(?i)\\.mp3|\\.flac|\\.wav", "")
                                           .replaceAll("\\(.*?\\)|\\[.*?\\]", "").trim();
            
            int durationSeconds = song.duration;
            if (durationSeconds < 0) durationSeconds = 0;
            
            lrcLibService.fetchLyrics(searchTitle, searchArtist, song.album, durationSeconds, new LrcLibService.LyricsCallback() {
                @Override
                public void onSuccess(String lrcContent, Integer lrcId) {
                    runOnUiThread(() -> {
                        // 如果获取到了lrc_id，保存到数据库
                        if (lrcId != null && lrcId > 0 && song.lrcId == null) {
                            new Thread(() -> {
                                song.lrcId = lrcId;
                                AppDatabase.getInstance(MainActivity.this).songDao().updateSong(song);
                                android.util.Log.d("MainActivity", "保存lrc_id到数据库: " + lrcId);
                            }).start();
                        }
                        
                        lyricList = LyricUtils.parseLrc(lrcContent);
                        if (lyricList.isEmpty()) {
                            loadNoLyricsState();
                        } else {
                            viewModel.setLyrics(lyricList);
                            if (!lyricList.isEmpty()) {
                                String firstLine = lyricList.get(0).text;
                                viewModel.setCurrentLyric(firstLine);
                                if (lyricSwitcher != null) {
                                    lyricSwitcher.setText(firstLine);
                                }
                            }
                        }
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> loadNoLyricsState());
                }
            });
        }
    }

    private MediaItem createMediaItemFromSong(Song song) {
        String path = song.path;
        android.net.Uri uri = android.net.Uri.parse(path);
        
        return new MediaItem.Builder()
                .setMediaId(String.valueOf(song.id))
                .setUri(uri)
                .setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.coverUrl != null && !song.coverUrl.isEmpty() ? android.net.Uri.parse(song.coverUrl) : null)
                        .setExtras(new android.os.Bundle())
                        .build())
                .build();
    }

    private void updateCurrentSongIdFromController() {
        if (mediaController != null && mediaController.getCurrentMediaItem() != null) {
            String mediaId = mediaController.getCurrentMediaItem().mediaId;
            try {
                viewModel.setCurrentSongId(Integer.parseInt(mediaId));
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void playFirstSongAndShowInfo() {
        if (mediaController == null || mediaController.getMediaItemCount() == 0) return;
        
        // 获取第一首歌
        MediaItem firstItem = mediaController.getMediaItemAt(0);
        int songId = mediaIdFromItem(firstItem);
        
        // 清空歌词
        viewModel.setLyrics(new ArrayList<>());
        viewModel.setCurrentLyric("搜索歌词中...");
        if (lyricSwitcher != null) {
            lyricSwitcher.setText("搜索歌词中...");
        }
        
        // 从数据库获取歌曲信息
        if (songId != -1) {
            new Thread(() -> {
                Song song = AppDatabase.getInstance(MainActivity.this).songDao().getSongById(songId);
                if (song != null) {
                    boolean isLocalImported = isLocalImportedSong(song);
                    runOnUiThread(() -> {
                        // 设置歌曲信息
                        viewModel.setSongTitle(song.title);
                        viewModel.setSongArtist(song.singer != null ? song.singer : song.artist);
                        viewModel.setCurrentSongId(song.id);
                        
                        if (isLocalImported) {
                            // 本地导入音乐
                            viewModel.setCoverUrl("android.resource://" + getPackageName() + "/" + R.drawable.music);
                            viewModel.setCurrentLyric("暂无歌词");
                            if (lyricSwitcher != null) {
                                lyricSwitcher.setText("暂无歌词");
                            }
                        } else {
                            // 在线音乐
                            if (song.coverUrl != null && !song.coverUrl.isEmpty()) {
                                viewModel.setCoverUrl(song.coverUrl);
                            }
                            
                            // 搜索歌词
                            String searchArtist = song.artist != null ? song.artist : "Unknown";
                            String searchTitle = song.title.replaceAll("(?i)\\.mp3|\\.flac|\\.wav", "")
                                                           .replaceAll("\\(.*?\\)|\\[.*?\\]", "").trim();
                            int durationSeconds = song.duration;
                            if (durationSeconds < 0) durationSeconds = 0;
                            
                            lrcLibService.fetchLyrics(searchTitle, searchArtist, song.album, durationSeconds, new LrcLibService.LyricsCallback() {
                                @Override
                                public void onSuccess(String lrcContent, Integer lrcId) {
                                    runOnUiThread(() -> {
                                        if (lrcId != null && lrcId > 0 && song.lrcId == null) {
                                            new Thread(() -> {
                                                song.lrcId = lrcId;
                                                AppDatabase.getInstance(MainActivity.this).songDao().updateSong(song);
                                            }).start();
                                        }
                                        
                                        lyricList = LyricUtils.parseLrc(lrcContent);
                                        if (lyricList.isEmpty()) {
                                            loadNoLyricsState();
                                        } else {
                                            viewModel.setLyrics(lyricList);
                                            if (!lyricList.isEmpty()) {
                                                String firstLine = lyricList.get(0).text;
                                                viewModel.setCurrentLyric(firstLine);
                                                if (lyricSwitcher != null) {
                                                    lyricSwitcher.setText(firstLine);
                                                }
                                            }
                                        }
                                    });
                                }

                                @Override
                                public void onError(String message) {
                                    runOnUiThread(() -> loadNoLyricsState());
                                }
                            });
                        }
                    });
                }
            }).start();
        }
        
        // 开始播放
        mediaController.seekTo(0, 0);
        mediaController.play();
        
        // 添加到最近播放
        if (songId != -1) {
            viewModel.addToRecent(songId);
        }
        
        // 显示播放页面
        showPlayerFragment();
    }

    private void switchFragment(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
    }

    private void initializeMediaController() {
        SessionToken sessionToken = new SessionToken(this, new ComponentName(this, PlaybackService.class));
        controllerFuture = new MediaController.Builder(this, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                onControllerInitialized();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    private boolean isInitialLoopMode = true;

    private void onControllerInitialized() {
        if (mediaController != null) {
            // Observe remote songs for the default playlist (Discovery/Recommended)
            viewModel.getRemoteSongs().observe(this, songs -> {
                if (mediaController != null && songs != null && !songs.isEmpty() && mediaController.getMediaItemCount() == 0) {
                    List<MediaItem> items = new ArrayList<>();
                    for (Song song : songs) {
                        items.add(createMediaItemFromSong(song));
                    }
                    mediaController.setMediaItems(items);
                    mediaController.prepare();
                    // Do NOT call mediaController.play() here to satisfy "don't play" requirement
                    
                    // 软件启动时不选中任何歌曲，不显示歌曲信息
                    // 用户点击歌曲后才会显示
                }
            });

            mediaController.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_ENDED) {
                        // 播放结束时的处理，如果不是单曲循环，且有下一首，则播放下一首
                        if (mediaController.getRepeatMode() == Player.REPEAT_MODE_OFF || mediaController.getRepeatMode() == Player.REPEAT_MODE_ALL) {
                            if (mediaController.hasNextMediaItem()) {
                                mediaController.seekToNext();
                            } else {
                                // 列表播完了，回到第一首
                                mediaController.seekTo(0, 0);
                                mediaController.pause();
                            }
                        }
                    }
                }

                @Override
                public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                    if (mediaItem != null && mediaItem.mediaMetadata.title != null) {
                        String title = mediaItem.mediaMetadata.title.toString();
                        String artist = mediaItem.mediaMetadata.artist != null ? mediaItem.mediaMetadata.artist.toString() : "Unknown";
                        String album = mediaItem.mediaMetadata.albumTitle != null ? mediaItem.mediaMetadata.albumTitle.toString() : "";
                        
                        // 重置播放位置和当前歌词
                        viewModel.setCurrentPosition(0);
                        lyricList = new ArrayList<>();
                        
                        // 检查是否是播放列表改变（软件启动时设置播放列表）
                        // PLAYLIST_CHANGED = 3
                        boolean isPlaylistChange = (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED);
                        
                        // 软件启动时的第一次 PLAYLIST_CHANGED，不设置任何歌曲信息
                        if (isPlaylistChange && isInitialPlaylistSetup) {
                            isInitialPlaylistSetup = false;
                            // 软件启动时不选中任何歌曲，不显示任何内容
                            return;
                        }
                        
                        // 用户通过 playSongList/playSong/playSongAt 切换歌曲时，会触发 PLAYLIST_CHANGED
                        // 此时歌词已经在那些方法中设置好了，不需要再处理
                        if (isPlaylistChange) {
                            return;
                        }
                        
                        // 只有用户主动播放过歌曲后，才处理歌词搜索和最近播放记录
                        if (!hasUserPlayedSong) {
                            return;
                        }
                        
                        // 只有自动播放下一首（reason=AUTO）或用户通过其他方式切换歌曲（reason=SEEK）时才处理歌词
                        // 检查是否是本地音乐
                        int songId = mediaIdFromItem(mediaItem);
                        
                        if (songId != -1) {
                            new Thread(() -> {
                                Song song = AppDatabase.getInstance(MainActivity.this).songDao().getSongById(songId);
                                boolean isLocalImported = isLocalImportedSong(song);
                                
                                if (isLocalImported) {
                                    runOnUiThread(() -> {
                                        // 本地导入音乐：显示暂无歌词，不搜索歌词
                                        viewModel.setSongTitle(song.title);
                                        viewModel.setSongArtist(song.singer != null ? song.singer : "未知歌手");
                                        
                                        // 保持封面显示为music.png
                                        viewModel.setCoverUrl("android.resource://" + getPackageName() + "/" + R.drawable.music);
                                        
                                        // 显示暂无歌词
                                        viewModel.setLyrics(new ArrayList<>());
                                        viewModel.setCurrentLyric("暂无歌词");
                                        if (lyricSwitcher != null) {
                                            lyricSwitcher.setText("暂无歌词");
                                        }
                                    });
                                } else {
                                    // 在线音乐：搜索歌词
                                    runOnUiThread(() -> {
                                        // 显示歌手
                                        viewModel.setSongTitle(song.title);
                                        viewModel.setSongArtist(song.singer != null ? song.singer : song.artist);
                                        
                                        if (mediaItem.mediaMetadata.artworkUri != null) {
                                            viewModel.setCoverUrl(mediaItem.mediaMetadata.artworkUri.toString());
                                        }
                                        
                                        // 显示搜索歌词中
                                        viewModel.setLyrics(new ArrayList<>());
                                        viewModel.setCurrentLyric("搜索歌词中...");
                                        if (lyricSwitcher != null) {
                                            lyricSwitcher.setText("搜索歌词中...");
                                        }
                                        
                                        // 获取时长并搜索歌词
                                        int durationSeconds = (int) (mediaController.getDuration() / 1000);
                                        if (durationSeconds <= 0 && song != null) {
                                            durationSeconds = song.duration;
                                        }
                                        if (durationSeconds < 0) durationSeconds = 0;
                                        
                                        // 优先使用lrc_id获取歌词
                                        if (song.lrcId != null && song.lrcId > 0) {
                                            android.util.Log.d("MainActivity", "使用lrc_id获取歌词: " + song.lrcId);
                                            lrcLibService.fetchLyricsById(song.lrcId, new LrcLibService.LyricsCallback() {
                                                @Override
                                                public void onSuccess(String lrcContent, Integer lrcId) {
                                                    runOnUiThread(() -> {
                                                        lyricList = LyricUtils.parseLrc(lrcContent);
                                                        if (lyricList.isEmpty()) {
                                                            loadNoLyricsState();
                                                        } else {
                                                            viewModel.setLyrics(lyricList);
                                                            if (!lyricList.isEmpty()) {
                                                                String firstLine = lyricList.get(0).text;
                                                                viewModel.setCurrentLyric(firstLine);
                                                                if (lyricSwitcher != null) {
                                                                    lyricSwitcher.setText(firstLine);
                                                                }
                                                            }
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onError(String message) {
                                                    runOnUiThread(() -> loadNoLyricsState());
                                                }
                                            });
                                        } else {
                                            // 没有lrc_id，使用传统搜索方式
                                            android.util.Log.d("MainActivity", "使用传统方式搜索歌词");
                                            String searchArtist = song.artist != null ? song.artist : "Unknown";
                                            String searchTitle = song.title.replaceAll("(?i)\\.mp3|\\.flac|\\.wav", "")
                                                                           .replaceAll("\\(.*?\\)|\\[.*?\\]", "").trim();

                                            lrcLibService.fetchLyrics(searchTitle, searchArtist, album, durationSeconds, new LrcLibService.LyricsCallback() {
                                                @Override
                                                public void onSuccess(String lrcContent, Integer lrcId) {
                                                    runOnUiThread(() -> {
                                                        // 如果获取到了lrc_id，保存到数据库
                                                        if (lrcId != null && lrcId > 0 && song.lrcId == null) {
                                                            new Thread(() -> {
                                                                song.lrcId = lrcId;
                                                                AppDatabase.getInstance(MainActivity.this).songDao().updateSong(song);
                                                                android.util.Log.d("MainActivity", "保存lrc_id到数据库: " + lrcId);
                                                            }).start();
                                                        }
                                                        
                                                        lyricList = LyricUtils.parseLrc(lrcContent);
                                                        if (lyricList.isEmpty()) {
                                                            loadNoLyricsState();
                                                        } else {
                                                            viewModel.setLyrics(lyricList);
                                                            if (!lyricList.isEmpty()) {
                                                                String firstLine = lyricList.get(0).text;
                                                                viewModel.setCurrentLyric(firstLine);
                                                                if (lyricSwitcher != null) {
                                                                    lyricSwitcher.setText(firstLine);
                                                                }
                                                            }
                                                        }
                                                    });
                                                }

                                                @Override
                                                public void onError(String message) {
                                                    runOnUiThread(() -> loadNoLyricsState());
                                                }
                                            });
                                        }
                                    });
                                }
                            }).start();
                        }
                        
                        // 只有用户主动播放过歌曲后，才添加到最近播放
                        if (songId != -1 && hasUserPlayedSong) {
                            viewModel.setCurrentSongId(songId);
                            viewModel.addToRecent(songId);
                        }
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    viewModel.setIsPlaying(isPlaying);
                    updateRotation(isPlaying);
                }
            });

            viewModel.getIsPlaying().observe(this, isPlaying -> {
                if (mediaController != null) {
                    if (isPlaying && !mediaController.isPlaying()) {
                        // 如果用户直接点击播放按钮（没有通过点击歌曲列表）
                        if (!hasUserPlayedSong && mediaController.getMediaItemCount() > 0) {
                            // 标记用户主动播放歌曲
                            hasUserPlayedSong = true;
                            // 设置第一首歌的信息
                            playFirstSongAndShowInfo();
                        } else {
                            mediaController.play();
                        }
                    } else if (!isPlaying && mediaController.isPlaying()) {
                        mediaController.pause();
                    }
                }
            });

            viewModel.getSeekToPosition().observe(this, position -> {
                if (mediaController != null && position >= 0) {
                    mediaController.seekTo(position);
                    viewModel.clearSeek();
                }
            });

            viewModel.getNextRequest().observe(this, request -> {
                if (mediaController != null && request > 0) {
                    if (mediaController.getMediaItemCount() > 0 && 
                        mediaController.getCurrentMediaItemIndex() == mediaController.getMediaItemCount() - 1) {
                        mediaController.seekTo(0, 0);
                    } else {
                        mediaController.seekToNext();
                    }
                }
            });

            viewModel.getPrevRequest().observe(this, request -> {
                if (mediaController != null && request > 0) {
                    if (mediaController.getMediaItemCount() > 0 && 
                        mediaController.getCurrentMediaItemIndex() == 0) {
                        mediaController.seekTo(mediaController.getMediaItemCount() - 1, 0);
                    } else {
                        mediaController.seekToPrevious();
                    }
                }
            });

            viewModel.getLoopMode().observe(this, mode -> {
                if (mediaController != null) {
                    if (mode == 1) {
                        mediaController.setRepeatMode(Player.REPEAT_MODE_ONE);
                        if (!isInitialLoopMode) {
                            ToastHelper.showShort(this, "单曲循环");
                        }
                    } else {
                        mediaController.setRepeatMode(Player.REPEAT_MODE_ALL);
                        if (!isInitialLoopMode) {
                            ToastHelper.showShort(this, "列表循环");
                        }
                    }
                    isInitialLoopMode = false;
                }
            });

            boolean playing = mediaController.isPlaying();
            viewModel.setIsPlaying(playing);
            updateRotation(playing);
            
            viewModel.getCoverUrl().observe(this, url -> {
                loadRotatingDiskCover(url);
            });
        }
    }

    private int mediaIdFromItem(MediaItem item) {
        try {
            return Integer.parseInt(item.mediaId);
        } catch (Exception e) {
            return -1;
        }
    }

    public void downloadSong(Song song) {
        if (song.isLocal) {
            ToastHelper.showShort(this, "这首歌已经是本地歌曲");
            return;
        }
        
        android.util.Log.d("MainActivity", "downloadSong called for: " + song.title + ", id=" + song.id);
        
        // 检查是否已经下载过
        File musicDir = getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC);
        if (musicDir != null) {
            String fileName = sanitizeFileName(song.title) + ".mp3";
            File file = new File(musicDir, fileName);
            if (file.exists()) {
                // 文件已存在，直接更新数据库
                song.path = file.getAbsolutePath();
                song.isLocal = true;
                viewModel.updateSong(song);
                ToastHelper.showShort(this, "歌曲已下载: " + song.title);
                return;
            }
        }
        
        ToastHelper.showShort(this, "开始下载: " + song.title);
        
        DownloadManager.getInstance(this).downloadSong(song, new DownloadManager.DownloadCallback() {
            @Override
            public void onProgress(int progress) {
                // 可以在这里更新进度条
            }
            
            @Override
            public void onSuccess(String localPath) {
                android.util.Log.d("MainActivity", "onSuccess called, localPath=" + localPath);
                // 先更新数据库（即使 Activity 已经销毁也能执行）
                song.path = localPath;
                song.isLocal = true;
                viewModel.updateSong(song);
                
                // 然后在 UI 线程显示提示（如果 Activity 还在）
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        ToastHelper.showShort(MainActivity.this, "下载完成: " + song.title);
                        android.util.Log.d("MainActivity", "下载完成提示已显示");
                    }
                });
            }
            
            @Override
            public void onError(String message) {
                android.util.Log.e("MainActivity", "onError called: " + message);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        ToastHelper.showShort(MainActivity.this, message);
                    }
                });
            }
        });
    }
    
    private String sanitizeFileName(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void updateRotation(boolean isPlaying) {
        if (isPlaying) {
            if (diskAnimator != null) {
                if (diskAnimator.isPaused()) diskAnimator.resume();
                else if (!diskAnimator.isRunning()) diskAnimator.start();
            }
            progressHandler.scheduleUpdate();
        } else {
            if (diskAnimator != null) diskAnimator.pause();
            progressHandler.stopUpdates();
        }
    }

    private void loadRotatingDiskCover(String url) {
        if (rotatingDisk == null) return;
        ImageRequest request = new ImageRequest.Builder(this)
                .data(url)
                .crossfade(true)
                .allowHardware(false)
                .target(rotatingDisk)
                .build();
        Coil.imageLoader(this).enqueue(request);
    }


    private void loadNoLyricsState() {
        lyricList = new java.util.ArrayList<>();
        lyricList.add(new LyricEntry(0, "暂无歌词"));
        viewModel.setLyrics(lyricList);
        viewModel.setCurrentLyric("暂无歌词");
        if (lyricSwitcher != null) {
            lyricSwitcher.setText("暂无歌词");
        }
    }

    private void loadLyrics() {
        loadNoLyricsState();
    }

    private void updateProgress() {
        if (mediaController != null) {
            long position = mediaController.getCurrentPosition();
            long duration = mediaController.getDuration();
            
            if (duration > 0) {
                viewModel.setCurrentPosition(position);
                viewModel.setDuration(duration);
                
                int progress = (int) (position * 100 / duration);
                progressBar.setProgress(progress);
                updateLyricSync(position);
            }
        }
    }

    private void setupLyricSwitcher() {
        lyricSwitcher = findViewById(R.id.lyricTextSwitcher);
        if (lyricSwitcher == null) return;
        lyricSwitcher.setFactory(() -> {
            TextView tv = new TextView(MainActivity.this);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextColor(android.graphics.Color.WHITE);
            tv.setTextSize(14);
            return tv;
        });

        android.view.animation.Animation in = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        android.view.animation.Animation out = android.view.animation.AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        in.setDuration(300);
        out.setDuration(300);
        lyricSwitcher.setInAnimation(in);
        lyricSwitcher.setOutAnimation(out);
    }

    private void updateLyricSync(long currentPosition) {
        if (lyricList == null || lyricList.isEmpty()) return;

        String currentLine = "";
        for (int i = 0; i < lyricList.size(); i++) {
            if (currentPosition >= lyricList.get(i).time) {
                if (i == lyricList.size() - 1 || currentPosition < lyricList.get(i + 1).time) {
                    currentLine = lyricList.get(i).text;
                    break;
                }
            }
        }
        
        if (!currentLine.equals(viewModel.getCurrentLyric().getValue())) {
            viewModel.setCurrentLyric(currentLine);
            if (lyricSwitcher != null) {
                lyricSwitcher.setText(currentLine);
            }
        }
    }

    private void setupRotatingDisk() {
        rotatingDisk = findViewById(R.id.rotatingDisk);
        if (rotatingDisk == null) return;
        diskAnimator = ObjectAnimator.ofFloat(rotatingDisk, "rotation", 0f, 360f);
        diskAnimator.setDuration(10000);
        diskAnimator.setRepeatCount(ValueAnimator.INFINITE);
        diskAnimator.setInterpolator(new LinearInterpolator());

        findViewById(R.id.navPlayer).setOnClickListener(v -> {
            showPlayerFragment();
        });
    }

    private void showPlayerFragment() {
        updateNavAndMiniPlayer(false);
        PlayerFragment playerFragment = new PlayerFragment();
        
        // Setup Shared Element Transition
        playerFragment.setSharedElementEnterTransition(
            android.transition.TransitionInflater.from(this).inflateTransition(android.R.transition.move)
        );
        playerFragment.setEnterTransition(
            android.transition.TransitionInflater.from(this).inflateTransition(android.R.transition.fade)
        );

        getSupportFragmentManager().beginTransaction()
                .addSharedElement(rotatingDisk, "song_cover")
                .replace(R.id.fragment_container, playerFragment, "player")
                .addToBackStack(null)
                .commit();
        
        getSupportFragmentManager().addOnBackStackChangedListener(new androidx.fragment.app.FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                    updateNavAndMiniPlayer(true);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
            mediaController = null;
        }
        if (progressHandler != null) {
            progressHandler.stopUpdates();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        // 注意：不在这里关闭 DownloadManager，让下载任务能够继续执行
        // DownloadManager 是单例模式，会在应用退出时自动清理
        // 清理LrcLibService
        lrcLibService.cancelAll();
        // 清理ToastHelper
        ToastHelper.cancelAll();
    }
}
