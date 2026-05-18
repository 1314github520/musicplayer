package com.example.musicplayer;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SongDao {
    @Query("SELECT * FROM songs")
    List<Song> getAllSongsSync();

    @Query("SELECT * FROM songs")
    LiveData<List<Song>> getAllSongs();

    @Query("SELECT * FROM songs WHERE isLocal = 1")
    LiveData<List<Song>> getLocalSongs();

    @Query("SELECT * FROM songs WHERE isLocal = 1 AND (path NOT LIKE '%/Android/data/%' OR path LIKE '%content://%')")
    LiveData<List<Song>> getImportedSongs();

    @Query("SELECT COUNT(*) FROM songs WHERE isLocal = 1 AND (path NOT LIKE '%/Android/data/%' OR path LIKE '%content://%')")
    LiveData<Integer> getImportedSongsCount();

    @Query("SELECT * FROM songs WHERE isLocal = 1 AND path LIKE '%/Android/data/%' AND path NOT LIKE '%content://%'")
    LiveData<List<Song>> getDownloadedSongs();

    @Query("SELECT COUNT(*) FROM songs WHERE isLocal = 1 AND path LIKE '%/Android/data/%' AND path NOT LIKE '%content://%'")
    LiveData<Integer> getDownloadedSongsCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Song song);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSongs(List<Song> songs);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertSongsIgnore(List<Song> songs);
    
    @Query("UPDATE songs SET " +
            "title = :title, " +
            "artist = :artist, " +
            "singer = :singer, " +
            "coverUrl = :coverUrl, " +
            "album = :album, " +
            "duration = :duration, " +
            "lrcId = :lrcId, " +
            "path = CASE WHEN isLocal = 1 THEN path ELSE :path END " +
            "WHERE id = :id")
    void updateRemoteSongFields(int id, String title, String artist, String singer, String coverUrl, String album, int duration, Long lrcId, String path);

    @Update
    void updateSong(Song song);

    @androidx.room.Delete
    void delete(Song song);

    @Query("SELECT * FROM songs WHERE id = :id")
    Song getSongById(int id);

    @Query("SELECT lyrics FROM songs WHERE lrcId = :lrcId AND lyrics IS NOT NULL AND lyrics != '' LIMIT 1")
    String getLyricsByLrcId(long lrcId);

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    LiveData<List<Song>> getFavoriteSongs();

    @Query("SELECT COUNT(*) FROM songs WHERE isFavorite = 1")
    LiveData<Integer> getFavoriteSongsCount();

    /**
     * 本地搜索歌曲（支持歌曲名、歌手、专辑）
     * 使用LOWER进行不区分大小写的搜索
     */
    @Query("SELECT * FROM songs WHERE LOWER(title) LIKE :searchPattern " +
           "OR LOWER(singer) LIKE :searchPattern " +
           "OR LOWER(album) LIKE :searchPattern " +
           "ORDER BY id DESC")
    List<Song> searchSongsLocal(String searchPattern);

    @Query("DELETE FROM songs WHERE isLocal = 0 AND isFavorite = 0")
    void deleteRemoteSongs();

    @Query("DELETE FROM songs WHERE isLocal = 0 AND isFavorite = 0 AND id NOT IN (:remoteIds)")
    void deleteRemoteSongsNotIn(List<Integer> remoteIds);

    @Transaction
    default void mergeRemoteSongs(List<Song> songs) {
        if (songs == null || songs.isEmpty()) {
            return;
        }
        insertSongsIgnore(songs);
        java.util.List<Integer> remoteIds = new java.util.ArrayList<>(songs.size());
        for (Song song : songs) {
            if (song == null) {
                continue;
            }
            remoteIds.add(song.id);
            updateRemoteSongFields(
                    song.id,
                    song.title,
                    song.artist,
                    song.singer,
                    song.coverUrl,
                    song.album,
                    song.duration,
                    song.lrcId,
                    song.path
            );
        }
        deleteRemoteSongsNotIn(remoteIds);
    }

    @Query("UPDATE songs SET isFavorite = 0")
    void clearFavorites();
}
