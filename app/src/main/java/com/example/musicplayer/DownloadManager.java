package com.example.musicplayer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManager {
    private static volatile DownloadManager instance;
    private final ExecutorService executorService;
    private final OkHttpClient client;
    private final Handler mainHandler;
    private final Context context;
    private final DownloadNotificationManager notificationManager;
    private final AtomicInteger notificationIdGenerator = new AtomicInteger(1000);
    private final ConcurrentHashMap<Integer, Boolean> downloadingSongs = new ConcurrentHashMap<>();
    
    public interface DownloadCallback {
        void onProgress(int progress);
        void onSuccess(String localPath);
        void onError(String message);
    }
    
    private DownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(3);
        this.client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.notificationManager = new DownloadNotificationManager(context);
    }
    
    public static DownloadManager getInstance(Context context) {
        if (instance == null) {
            synchronized (DownloadManager.class) {
                if (instance == null) {
                    instance = new DownloadManager(context);
                }
            }
        }
        return instance;
    }
    
    public synchronized void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
            try {
                if (!executorService.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    android.util.Log.w("DownloadManager", "ExecutorService did not terminate in time");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        instance = null;
    }
    
    public void downloadSong(Song song, DownloadCallback callback) {
        if (downloadingSongs.putIfAbsent(song.id, true) != null) {
            mainHandler.post(() -> callback.onError("该歌曲正在下载中"));
            return;
        }
        
        int notificationId = notificationIdGenerator.incrementAndGet();
        
        executorService.execute(() -> {
            try {
                File musicDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC);
                if (musicDir != null && !musicDir.exists()) {
                    musicDir.mkdirs();
                }
                
                String fileName = sanitizeFileName(song.title) + ".mp3";
                File file = new File(musicDir, fileName);
                
                android.util.Log.d("DownloadManager", "开始下载: " + song.title + " 到 " + file.getAbsolutePath());
                
                notificationManager.showDownloadStart(song.title, notificationId);
                
                Request request = new Request.Builder()
                        .url(song.path)
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        android.util.Log.e("DownloadManager", "下载失败: " + response.code());
                        notificationManager.showDownloadError(song.title, "HTTP " + response.code(), notificationId);
                        mainHandler.post(() -> callback.onError("下载失败: " + response.code()));
                        return;
                    }
                    
                    long contentLength = response.body().contentLength();
                    long lastProgressUpdate = 0;
                    
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long totalBytesRead = 0;
                        
                        while ((bytesRead = response.body().byteStream().read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            
                            if (contentLength > 0) {
                                int progress = (int) (totalBytesRead * 100 / contentLength);
                                final int finalProgress = progress;
                                
                                if (System.currentTimeMillis() - lastProgressUpdate > 500 || progress == 100) {
                                    lastProgressUpdate = System.currentTimeMillis();
                                    notificationManager.updateProgress(song.title, progress, notificationId);
                                    mainHandler.post(() -> callback.onProgress(finalProgress));
                                }
                            }
                        }
                    }
                    
                    String localPath = file.getAbsolutePath();
                    android.util.Log.d("DownloadManager", "下载完成: " + localPath);
                    
                    notificationManager.showDownloadComplete(song.title, notificationId);
                    
                    mainHandler.post(() -> {
                        android.util.Log.d("DownloadManager", "回调 onSuccess");
                        callback.onSuccess(localPath);
                    });
                }
            } catch (IOException e) {
                android.util.Log.e("DownloadManager", "下载异常", e);
                notificationManager.showDownloadError(song.title, e.getMessage(), notificationId);
                mainHandler.post(() -> callback.onError("下载失败: " + e.getMessage()));
            } finally {
                downloadingSongs.remove(song.id);
            }
        });
    }
    
    private String sanitizeFileName(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
