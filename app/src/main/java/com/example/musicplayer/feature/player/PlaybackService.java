package com.example.musicplayer.feature.player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.ui.PlayerNotificationManager;
import com.example.musicplayer.R;
import com.example.musicplayer.feature.main.MainActivity;

public class PlaybackService extends MediaSessionService {
    private static final String NOTIFICATION_CHANNEL_ID = "playback_channel";
    private static final int NOTIFICATION_ID = 1001;

    private MediaSession mediaSession = null;
    private ExoPlayer player = null;
    private PlayerNotificationManager notificationManager = null;
    private boolean isForegroundService = false;

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            updateForegroundState();
            if (playbackState == Player.STATE_ENDED && !player.getPlayWhenReady()) {
                stopForegroundIfNeeded(true);
            }
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updateForegroundState();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        
        // 构建播放器，开启音频焦点管理和拔出耳机自动暂停
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */ true)
                .setHandleAudioBecomingNoisy(true)
                .build();
        player.addListener(playerListener);
        
        mediaSession = new MediaSession.Builder(this, player).build();
        initializeNotificationManager();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    /**
     * 当应用从最近任务列表中被划掉时触发
     */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (player != null) {
            boolean shouldKeepAlive = shouldKeepMediaNotification();
            if (!shouldKeepAlive) {
                stopForegroundIfNeeded(true);
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        stopForegroundIfNeeded(true);
        if (notificationManager != null) {
            notificationManager.setPlayer(null);
            notificationManager = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.removeListener(playerListener);
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    private void initializeNotificationManager() {
        notificationManager = new PlayerNotificationManager.Builder(this, NOTIFICATION_ID, NOTIFICATION_CHANNEL_ID)
                .setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
                    @Override
                    public CharSequence getCurrentContentTitle(Player player) {
                        MediaMetadata metadata = player.getMediaMetadata();
                        CharSequence title = metadata.title;
                        return title != null ? title : getString(R.string.app_name);
                    }

                    @Override
                    public PendingIntent createCurrentContentIntent(Player player) {
                        Intent intent = new Intent(PlaybackService.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        return PendingIntent.getActivity(
                                PlaybackService.this,
                                0,
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                        );
                    }

                    @Override
                    public CharSequence getCurrentContentText(Player player) {
                        MediaMetadata metadata = player.getMediaMetadata();
                        CharSequence artist = metadata.artist;
                        return artist != null ? artist : "正在播放";
                    }

                    @Override
                    public android.graphics.Bitmap getCurrentLargeIcon(
                            Player player,
                            PlayerNotificationManager.BitmapCallback callback
                    ) {
                        return null;
                    }
                })
                .setNotificationListener(new PlayerNotificationManager.NotificationListener() {
                    @Override
                    public void onNotificationPosted(int notificationId, Notification notification, boolean ongoing) {
                        if (ongoing && !isForegroundService) {
                            startForeground(notificationId, notification);
                            isForegroundService = true;
                            return;
                        }
                        if (!ongoing && isForegroundService) {
                            stopForeground(false);
                            isForegroundService = false;
                        }
                    }

                    @Override
                    public void onNotificationCancelled(int notificationId, boolean dismissedByUser) {
                        stopForegroundIfNeeded(true);
                        stopSelf();
                    }
                })
                .build();
        if (mediaSession != null) {
            notificationManager.setMediaSessionToken(mediaSession.getSessionCompatToken());
        }
        notificationManager.setSmallIcon(android.R.drawable.ic_media_play);
        notificationManager.setUseFastForwardAction(false);
        notificationManager.setUseRewindAction(false);
        notificationManager.setUseNextActionInCompactView(true);
        notificationManager.setUsePreviousActionInCompactView(true);
        notificationManager.setPlayer(player);
    }

    private void updateForegroundState() {
        if (player == null) {
            return;
        }
        boolean shouldRunInForeground = shouldKeepMediaNotification();
        if (!shouldRunInForeground) {
            stopForegroundIfNeeded(false);
        }
    }

    private boolean shouldKeepMediaNotification() {
        if (player == null) {
            return false;
        }
        int playbackState = player.getPlaybackState();
        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            return false;
        }
        return player.getCurrentMediaItem() != null;
    }

    private void stopForegroundIfNeeded(boolean removeNotification) {
        if (!isForegroundService) {
            return;
        }
        stopForeground(removeNotification);
        isForegroundService = false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("显示当前播放的歌曲和控制按钮");
        notificationManager.createNotificationChannel(channel);
    }
}
