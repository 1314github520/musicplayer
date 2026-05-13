package com.example.musicplayer;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateManager {
    private static final String CHANNEL_ID = "update_channel";
    private static final String BASE_URL = "http://8.162.14.195:3000";
    private static final int NOTIFICATION_ID = 9999;
    
    private static volatile UpdateManager instance;
    private final Context context;
    private final ExecutorService executorService;
    private final OkHttpClient client;
    private final NotificationManager notificationManager;
    
    public interface CheckUpdateCallback {
        void onUpdateAvailable(VersionInfo versionInfo);
        void onNoUpdate();
        void onError(String message);
    }
    
    public interface DownloadCallback {
        void onProgress(int progress);
        void onSuccess(File apkFile);
        void onError(String message);
    }
    
    public static class VersionInfo {
        public int versionCode;
        public String versionName;
        public String downloadUrl;
        public String updateLog;
        public String publishTime;
    }
    
    private static class ApiResponse {
        int code;
        VersionInfo data;
    }
    
    private UpdateManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.client = HttpClient.getInstance();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
        cleanupOldApks();
    }
    
    public static UpdateManager getInstance(Context context) {
        if (instance == null) {
            synchronized (UpdateManager.class) {
                if (instance == null) {
                    instance = new UpdateManager(context);
                    instance.cleanupOldApks();
                }
            }
        }
        return instance;
    }

    private void cleanupOldApks() {
        executorService.execute(() -> {
            try {
                File dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (dir != null && dir.exists()) {
                    File[] files = dir.listFiles((d, name) -> name.endsWith(".apk"));
                    if (files != null) {
                        for (File file : files) {
                            file.delete();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("UpdateManager", "Failed to cleanup old APKs", e);
            }
        });
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                CHANNEL_ID,
                "应用更新",
                NotificationManager.IMPORTANCE_LOW
            );
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    public void checkUpdate(CheckUpdateCallback callback) {
        final CheckUpdateCallback finalCallback = callback;
        executorService.execute(() -> {
            try {
                // 后端 Node.js 服务器路径为 /api/app/version
                Request request = new Request.Builder()
                    .url(BASE_URL + "/api/app/version")
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String json = response.body().string();
                        ApiResponse apiResponse = new com.google.gson.Gson().fromJson(json, ApiResponse.class);
                        
                        if (apiResponse != null && apiResponse.data != null) {
                            int currentVersionCode = getCurrentVersionCode();
                            VersionInfo info = apiResponse.data;
                            
                            // 补全相对路径的下载链接
                            if (info.downloadUrl != null && !info.downloadUrl.startsWith("http")) {
                                if (info.downloadUrl.startsWith("/")) {
                                    info.downloadUrl = BASE_URL + info.downloadUrl;
                                } else {
                                    info.downloadUrl = BASE_URL + "/" + info.downloadUrl;
                                }
                            }

                            if (info.versionCode > currentVersionCode) {
                                notifyOnMainThread(() -> {
                                    if (finalCallback != null) {
                                        finalCallback.onUpdateAvailable(info);
                                    }
                                });
                            } else {
                                notifyOnMainThread(() -> {
                                    if (finalCallback != null) {
                                        finalCallback.onNoUpdate();
                                    }
                                });
                            }
                        } else {
                            notifyOnMainThread(() -> {
                                if (finalCallback != null) {
                                    finalCallback.onError("解析版本信息失败");
                                }
                            });
                        }
                    } else {
                        notifyOnMainThread(() -> {
                            if (finalCallback != null) {
                                finalCallback.onError("网络请求失败: " + response.code());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                notifyOnMainThread(() -> {
                    if (finalCallback != null) {
                        finalCallback.onError("检查更新失败: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    private int getCurrentVersionCode() {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 1;
        }
    }
    
    private void notifyOnMainThread(Runnable runnable) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(runnable);
    }
    
    public void downloadApk(VersionInfo versionInfo, DownloadCallback callback) {
        executorService.execute(() -> {
            try {
                File apkFile = new File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS), 
                    "MusicPlayer-" + versionInfo.versionName + ".apk");
                
                notifyProgress(callback, 0);
                showNotification("正在下载", versionInfo.versionName, 0, true);
                
                Request request = new Request.Builder()
                    .url(versionInfo.downloadUrl)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        notifyError(callback, "下载失败: HTTP " + response.code());
                        showNotification("下载失败", versionInfo.versionName, 0, false);
                        return;
                    }
                    
                    long contentLength = response.body().contentLength();
                    long lastProgressUpdate = 0;
                    
                    try (FileOutputStream fos = new FileOutputStream(apkFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        long totalBytesRead = 0;
                        
                        while ((bytesRead = response.body().byteStream().read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            
                            if (contentLength > 0) {
                                int progress = (int) (totalBytesRead * 100 / contentLength);
                                
                                if (System.currentTimeMillis() - lastProgressUpdate > 200 || progress == 100) {
                                    lastProgressUpdate = System.currentTimeMillis();
                                    notifyProgress(callback, progress);
                                    showNotification("正在下载", versionInfo.versionName, progress, true);
                                }
                            }
                        }
                    }
                    
                    notifyProgress(callback, 100);
                    showNotification("下载完成", versionInfo.versionName, 100, false);
                    notifySuccess(callback, apkFile);
                }
            } catch (Exception e) {
                notifyError(callback, "下载失败: " + e.getMessage());
                showNotification("下载失败", e.getMessage(), 0, false);
            }
        });
    }
    
    private void notifyProgress(DownloadCallback callback, int progress) {
        if (callback != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onProgress(progress));
        }
    }
    
    private void notifySuccess(DownloadCallback callback, File apkFile) {
        if (callback != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(apkFile));
        }
    }
    
    private void notifyError(DownloadCallback callback, String message) {
        if (callback != null) {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onError(message));
        }
    }
    
    private void showNotification(String title, String content, int progress, boolean ongoing) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ongoing ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(ongoing)
            .setPriority(NotificationCompat.PRIORITY_LOW);
        
        if (ongoing && progress > 0) {
            builder.setProgress(100, progress, false);
        }
        
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    
    public void installApk(File apkFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apkFile);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        } else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        }
        
        context.startActivity(intent);
    }
}
