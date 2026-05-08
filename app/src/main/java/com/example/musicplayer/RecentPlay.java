package com.example.musicplayer;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recent_plays")
public class RecentPlay {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int songId;
    public long timestamp;

    public RecentPlay(int songId, long timestamp) {
        this.songId = songId;
        this.timestamp = timestamp;
    }
}
