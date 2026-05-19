package com.example.musicplayer.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RewriteQueriesToDropUnusedColumns;
import com.example.musicplayer.data.model.RecentPlay;
import com.example.musicplayer.data.model.Song;
import java.util.List;

@Dao
public interface RecentPlayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecentPlay recentPlay);

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT songs.*, MAX(recent_plays.timestamp) as lastPlayTime FROM songs INNER JOIN recent_plays ON songs.id = recent_plays.songId WHERE recent_plays.timestamp >= :sevenDaysAgo GROUP BY songs.id ORDER BY lastPlayTime DESC")
    LiveData<List<Song>> getRecentSongs(long sevenDaysAgo);

    @Query("SELECT COUNT(*) FROM (SELECT DISTINCT songId FROM recent_plays WHERE timestamp >= :sevenDaysAgo)")
    LiveData<Integer> getRecentCount(long sevenDaysAgo);
    
    @Query("DELETE FROM recent_plays WHERE songId = :songId")
    void deleteBySongId(int songId);
    
    @Query("SELECT COUNT(*) FROM recent_plays")
    LiveData<Integer> getTotalPlayCount();
    
    @Query("DELETE FROM recent_plays WHERE timestamp < :timestamp")
    void deleteOldRecords(long timestamp);

    @Query("DELETE FROM recent_plays")
    void deleteAll();
}
