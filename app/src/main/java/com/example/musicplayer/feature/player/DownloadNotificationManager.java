package com.example.musicplayer.feature.player;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.musicplayer.R;

public class DownloadNotificationManager {
    private static final String CHANNEL_ID = "download_channel";
    private static final String CHANNEL_NAME = "下载管理";
    private static final String CHANNEL_DESC = "显示音乐下载进度";
    
    private final NotificationManager notificationManager;
    private final Context context;
    
    public DownloadNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(CHANNEL_DESC);
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    public void showDownloadStart(String songTitle, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在下载")
            .setContentText(songTitle)
            .setProgress(100, 0, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW);
        
        notificationManager.notify(notificationId, builder.build());
    }
    
    public void updateProgress(String songTitle, int progress, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在下载")
            .setContentText(songTitle)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW);
        
        notificationManager.notify(notificationId, builder.build());
    }
    
    public void showDownloadComplete(String songTitle, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("下载完成")
            .setContentText(songTitle)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true);
        
        notificationManager.notify(notificationId, builder.build());
    }
    
    public void showDownloadError(String songTitle, String errorMessage, int notificationId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("下载失败")
            .setContentText(songTitle + ": " + errorMessage)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
        
        notificationManager.notify(notificationId, builder.build());
    }
    
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
    }
}
