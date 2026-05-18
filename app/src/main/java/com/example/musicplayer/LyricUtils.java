package com.example.musicplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricUtils {
    // 更加宽松且支持多标签的正则
    private static final Pattern TIME_TAG_PATTERN = Pattern.compile("\\[(\\d{1,2}):(\\d{1,2})(?:\\.(\\d{1,3}))?\\]");

    public static List<LyricEntry> parseLrc(String lrcContent) {
        List<LyricEntry> entries = new ArrayList<>();
        if (lrcContent == null || lrcContent.trim().isEmpty()) {
            return entries;
        }

        String[] lines = lrcContent.split("\\n");
        for (String line : lines) {
            if (line == null) continue;
            line = line.trim();
            if (line.isEmpty()) continue;

            // 查找这一行中所有的 [mm:ss.ms] 标签
            List<Long> times = new ArrayList<>();
            Matcher matcher = TIME_TAG_PATTERN.matcher(line);
            int lastTagEnd = 0;
            while (matcher.find()) {
                try {
                    long min = Long.parseLong(matcher.group(1));
                    long sec = Long.parseLong(matcher.group(2));
                    long ms = 0;
                    String msGroup = matcher.group(3);
                    if (msGroup != null) {
                        ms = Long.parseLong(msGroup);
                        if (msGroup.length() == 1) ms *= 100;
                        else if (msGroup.length() == 2) ms *= 10;
                    }
                    times.add(min * 60 * 1000 + sec * 1000 + ms);
                    lastTagEnd = matcher.end();
                } catch (Exception ignored) {}
            }

            if (!times.isEmpty()) {
                // 标签之后的内容是歌词文本
                String text = line.substring(lastTagEnd).trim();
                // 过滤常见的元数据标签，如 [by:xxx]
                if (text.isEmpty() && line.contains(":")) {
                    continue;
                }
                
                // 转换为简体中文
                text = ChnConverter.toSimplified(text);
                for (long time : times) {
                    entries.add(new LyricEntry(time, text));
                }
            }
        }

        // 兜底处理：如果不是标准 LRC 格式（没有匹配到任何时间戳），将纯文本作为单行展示
        if (entries.isEmpty() && !lrcContent.trim().isEmpty()) {
            String cleanContent = ChnConverter.toSimplified(lrcContent.trim());
            String[] rawLines = cleanContent.split("\\n");
            int validLineCount = 0;
            for (int i = 0; i < rawLines.length; i++) {
                String text = rawLines[i].trim();
                // 过滤掉 [ti:xxx] 等头部元数据
                if (!text.isEmpty() && !text.startsWith("[") && !text.contains("]")) {
                    entries.add(new LyricEntry(validLineCount * 3000L, text));
                    validLineCount++;
                }
            }
        }

        try {
            Collections.sort(entries);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entries;
    }

    public static int findCurrentLyricIndex(List<LyricEntry> lyrics, long currentPosition) {
        if (lyrics == null || lyrics.isEmpty()) {
            return -1;
        }

        int low = 0;
        int high = lyrics.size() - 1;
        int index = -1;
        while (low <= high) {
            int mid = low + (high - low) / 2;
            if (lyrics.get(mid).time <= currentPosition) {
                index = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return index;
    }
}
