package com.example.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricUtils {
    private static final Pattern PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)");

    public static List<LyricEntry> parseLrc(String lrcContent) {
        List<LyricEntry> entries = new ArrayList<>();
        String[] lines = lrcContent.split("\\n");
        for (String line : lines) {
            Matcher matcher = PATTERN.matcher(line);
            if (matcher.matches()) {
                long min = Long.parseLong(matcher.group(1));
                long sec = Long.parseLong(matcher.group(2));
                long ms = Long.parseLong(matcher.group(3));
                if (matcher.group(3).length() == 2) ms *= 10;
                
                long time = min * 60 * 1000 + sec * 1000 + ms;
                String text = matcher.group(4).trim();
                // 转换为简体中文
                text = ChnConverter.toSimplified(text);
                entries.add(new LyricEntry(time, text));
            }
        }
        Collections.sort(entries);
        return entries;
    }
}