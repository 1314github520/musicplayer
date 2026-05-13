package com.example.musicplayer;

import android.content.Intent;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class PlaybackService extends MediaSessionService {
    private MediaSession mediaSession = null;
    private ExoPlayer player = null;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // 构建播放器，开启音频焦点管理和拔出耳机自动暂停
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(AudioAttributes.DEFAULT,  /* handleAudioFocus= */ true)
                .setHandleAudioBecomingNoisy(true)
                .build();
        
        mediaSession = new MediaSession.Builder(this, player).build();
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
            // 如果不在播放，或者播放已结束，则完全停止服务
            if (!player.getPlayWhenReady() || player.getPlaybackState() == Player.STATE_IDLE || player.getPlaybackState() == Player.STATE_ENDED) {
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
