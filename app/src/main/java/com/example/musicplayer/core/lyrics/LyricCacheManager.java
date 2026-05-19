package com.example.musicplayer.core.lyrics;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LyricCacheManager {
    private static final String CACHE_DIR = "lyrics_cache";
    private static final String PREF_NAME = "lyric_cache_prefs";
    private static final long CACHE_EXPIRE_TIME = 30 * 24 * 60 * 60 * 1000L;
    
    private final File cacheDir;
    private final SharedPreferences prefs;
    
    private static volatile LyricCacheManager instance;
    
    private LyricCacheManager(Context context) {
        this.cacheDir = new File(context.getCacheDir(), CACHE_DIR);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static LyricCacheManager getInstance(Context context) {
        if (instance == null) {
            synchronized (LyricCacheManager.class) {
                if (instance == null) {
                    instance = new LyricCacheManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    public void putLyric(String key, String lyric) {
        if (key == null || lyric == null) return;
        
        String fileName = generateFileName(key);
        File file = new File(cacheDir, fileName);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(lyric.getBytes("UTF-8"));
            prefs.edit()
                .putLong(fileName, System.currentTimeMillis())
                .apply();
        } catch (IOException e) {
            android.util.Log.e("LyricCacheManager", "Failed to cache lyric", e);
        }
    }
    
    public String getLyric(String key) {
        if (key == null) return null;
        
        String fileName = generateFileName(key);
        File file = new File(cacheDir, fileName);
        
        if (!file.exists()) return null;
        
        long cacheTime = prefs.getLong(fileName, 0);
        if (System.currentTimeMillis() - cacheTime > CACHE_EXPIRE_TIME) {
            file.delete();
            prefs.edit().remove(fileName).apply();
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return new String(data, "UTF-8");
        } catch (IOException e) {
            android.util.Log.e("LyricCacheManager", "Failed to read cached lyric", e);
            return null;
        }
    }
    
    public void clearCache() {
        File[] files = cacheDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        prefs.edit().clear().apply();
    }
    
    public void clearExpiredCache() {
        File[] files = cacheDir.listFiles();
        if (files == null) return;
        
        long currentTime = System.currentTimeMillis();
        for (File file : files) {
            long cacheTime = prefs.getLong(file.getName(), 0);
            if (currentTime - cacheTime > CACHE_EXPIRE_TIME) {
                file.delete();
                prefs.edit().remove(file.getName()).apply();
            }
        }
    }
    
    private String generateFileName(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString() + ".lrc";
        } catch (NoSuchAlgorithmException e) {
            return key.hashCode() + ".lrc";
        }
    }
}
