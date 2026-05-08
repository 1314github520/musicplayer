package com.example.musicplayer;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "songs")
public class Song {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String title;
    public String artist;  // 作曲家，用于搜索歌词
    public String singer;  // 歌手，用于显示
    public String path;
    public String coverUrl;
    public String album;
    public int duration; // in seconds
    public boolean isLocal;
    public boolean isFavorite;
    public Integer lrcId; // lrclib API返回的歌词ID

    public Song(String title, String artist, String path, String coverUrl, boolean isLocal) {
        this.title = title;
        this.artist = artist;
        this.singer = artist; // 默认歌手和作曲家相同
        this.path = path;
        this.coverUrl = coverUrl;
        this.isLocal = isLocal;
        this.album = "Unknown Album";
        this.duration = 0;
        this.isFavorite = false;
        this.lrcId = null;
    }
}
