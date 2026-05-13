package com.example.musicplayer;

public final class Constants {
    private Constants() {}
    
    public static final class API {
        public static final String BASE_URL = "http://8.162.14.195:3000";
        public static final int CONNECT_TIMEOUT = 15;
        public static final int READ_TIMEOUT = 30;
        public static final int WRITE_TIMEOUT = 30;
        public static final int MAX_RETRY_COUNT = 3;
    }
    
    public static final class Database {
        public static final String DATABASE_NAME = "musicplayer_db";
        public static final int DATABASE_VERSION = 1;
    }
    
    public static final class Cache {
        public static final int LYRIC_CACHE_SIZE = 500;
        public static final long LYRIC_CACHE_EXPIRE_TIME = 30L * 24 * 60 * 60 * 1000;
        public static final long MAX_LOG_SIZE = 5L * 1024 * 1024;
    }
    
    public static final class Playback {
        public static final int PROGRESS_UPDATE_INTERVAL = 200;
        public static final int DISK_ROTATION_DURATION = 10000;
    }
    
    public static final class Notification {
        public static final String CHANNEL_ID_PLAYBACK = "playback_channel";
        public static final String CHANNEL_ID_DOWNLOAD = "download_channel";
        public static final String CHANNEL_ID_UPDATE = "update_channel";
        public static final int NOTIFICATION_ID_PLAYBACK = 1001;
        public static final int NOTIFICATION_ID_DOWNLOAD = 1002;
        public static final int NOTIFICATION_ID_UPDATE = 9999;
    }
    
    public static final class Preference {
        public static final String USER_PROFILE = "user_profile";
        public static final String KEY_NICKNAME = "nickname";
        public static final String KEY_SIGNATURE = "signature";
        public static final String KEY_AVATAR = "avatar";
        public static final String KEY_GENDER = "gender";
        public static final String KEY_BIRTHDAY = "birthday";
    }
    
    public static final class Default {
        public static final String NICKNAME = "听乐人";
        public static final String SIGNATURE = "音乐让生活更美好";
        public static final String GENDER = "男";
        public static final String BIRTHDAY = "2000-01-01";
        public static final String USER_ID = "ID: 20240001";
    }
    
    public static final class RequestCode {
        public static final int PERMISSION_POST_NOTIFICATIONS = 100;
        public static final int PERMISSION_READ_EXTERNAL_STORAGE = 101;
        public static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 102;
    }
}
