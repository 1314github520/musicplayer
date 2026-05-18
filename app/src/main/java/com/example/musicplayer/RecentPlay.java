package com.example.musicplayer;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recent_plays",
        indices = {
                @Index("songId"),
                @Index("timestamp"),
                @Index(value = {"songId", "timestamp"})
        }
)
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
