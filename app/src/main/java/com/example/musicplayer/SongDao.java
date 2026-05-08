package com.example.musicplayer;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertSongs(List<Song> songs);
    
    @Query("UPDATE songs SET title = :title, artist = :artist, singer = :singer, coverUrl = :coverUrl, album = :album, duration = :duration WHERE id = :id")
    void updateSongInfo(int id, String title, String artist, String singer, String coverUrl, String album, int duration);

    @Update
    void updateSong(Song song);

    @androidx.room.Delete
    void delete(Song song);

    @Query("SELECT * FROM songs WHERE id = :id")
    Song getSongById(int id);

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
}
