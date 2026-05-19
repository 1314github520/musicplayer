package com.example.musicplayer.data.model;

public class LyricEntry implements Comparable<LyricEntry> {
    public final long time; // 毫秒
    public final String text;

    public LyricEntry(long time, String text) {
        this.time = time;
        this.text = text;
    }

    @Override
    public int compareTo(LyricEntry other) {
        return Long.compare(this.time, other.time);
    }
}