package com.example.musicplayer.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import com.example.musicplayer.data.model.RecentPlay;
import com.example.musicplayer.data.model.Song;

@Database(entities = {Song.class, RecentPlay.class}, version = 10, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE INDEX IF NOT EXISTS index_songs_isLocal ON songs(isLocal)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_songs_isFavorite ON songs(isFavorite)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_songs_lrcId ON songs(lrcId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_songs_path ON songs(path)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_recent_plays_songId ON recent_plays(songId)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_recent_plays_timestamp ON recent_plays(timestamp)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_recent_plays_songId_timestamp ON recent_plays(songId, timestamp)");
        }
    };

    private static AppDatabase instance;

    public abstract SongDao songDao();
    public abstract RecentPlayDao recentPlayDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            try {
                instance = Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, "music_db")
                        .addMigrations(MIGRATION_9_10)
                        .build();
                android.util.Log.d("AppDatabase", "数据库初始化成功");
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "数据库初始化失败", e);
                throw new RuntimeException("无法初始化数据库", e);
            }
        }
        return instance;
    }
}
