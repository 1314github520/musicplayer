package com.example.musicplayer.core.lyrics;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import java.util.LinkedHashMap;
import java.util.Map;

public class ChnConverter {

    // 台湾繁体特殊字补充表，opencc4j 词库未收录的漏网字
    // 用 LinkedHashMap 保证词组（长的）先匹配，再匹配单字
    private static final Map<String, String> TW_SPECIAL_MAP = new LinkedHashMap<>();

    static {
        // 词组优先（长的优先匹配，防止拆字导致转换错误）
        TW_SPECIAL_MAP.put("妳們", "你们");
        TW_SPECIAL_MAP.put("牠們", "它们");
        TW_SPECIAL_MAP.put("祂們", "他们");
        
        // 歌词中极高频但 OpenCC 偶尔转换不彻底的字
        TW_SPECIAL_MAP.put("妳", "你");
        TW_SPECIAL_MAP.put("著", "着"); // 唱著 -> 唱着
        TW_SPECIAL_MAP.put("裏", "里"); // 心裏 -> 心里
        TW_SPECIAL_MAP.put("爲", "为"); // 爲了 -> 为了
        TW_SPECIAL_MAP.put("繫", "系"); // 連繫 -> 联系
        TW_SPECIAL_MAP.put("佈", "布"); // 發佈 -> 发布
        TW_SPECIAL_MAP.put("妳", "你");
        TW_SPECIAL_MAP.put("牠", "它");
        TW_SPECIAL_MAP.put("祂", "他");
        TW_SPECIAL_MAP.put("噁", "恶");
        TW_SPECIAL_MAP.put("齣", "出");
        TW_SPECIAL_MAP.put("麼", "么");
    }

    /**
     * 私有构造，工具类不允许实例化
     */
    private ChnConverter() {}

    /**
     * 繁体（含台湾繁体）→ 简体
     * 两步策略：
     *   1. ZhConverterUtil 处理繁体词汇
     *   2. 补充映射表兜底处理词库未收录的字
     */
    public static String toSimplified(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 性能优化：如果不包含繁体且没有特殊字，直接返回原字符串
        if (!containsTraditional(text)) {
            return text;
        }

        try {
            // 第一步：使用通用繁体词库进行智能转换
            String result = ZhConverterUtil.toSimple(text);
            
            // 第二步：使用特殊映射表兜底（处理 OpenCC 无法 100% 覆盖的单字）
            for (Map.Entry<String, String> entry : TW_SPECIAL_MAP.entrySet()) {
                if (result.contains(entry.getKey())) {
                    result = result.replace(entry.getKey(), entry.getValue());
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return text;
        }
    }
    /**
     * 判断字符串是否包含繁体字
     * 可用于决定是否需要触发转换，避免不必要的开销
     */
    public static boolean containsTraditional(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            return ZhConverterUtil.containsTraditional(text);
        } catch (Exception e) {
            return false;
        }
    }
}

