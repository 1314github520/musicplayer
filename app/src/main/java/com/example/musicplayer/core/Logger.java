package com.example.musicplayer.core;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {
    private static final String TAG = "Logger";
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE_PREFIX = "musicplayer_";
    private static final String LOG_FILE_EXTENSION = ".log";
    private static final long MAX_LOG_SIZE = 5 * 1024 * 1024;
    
    private static volatile Logger instance;
    private final File logDir;
    private final SimpleDateFormat dateFormat;
    private boolean enableFileLogging = false;
    private boolean enableDebugLogging = true;
    
    private Logger(Context context) {
        this.logDir = new File(context.getExternalFilesDir(null), LOG_DIR);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    }
    
    public static Logger getInstance(Context context) {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    instance = new Logger(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    public void setEnableFileLogging(boolean enable) {
        this.enableFileLogging = enable;
    }
    
    public void setEnableDebugLogging(boolean enable) {
        this.enableDebugLogging = enable;
    }
    
    public void d(String tag, String message) {
        if (enableDebugLogging) {
            Log.d(tag, message);
            if (enableFileLogging) {
                writeToFile("DEBUG", tag, message);
            }
        }
    }
    
    public void i(String tag, String message) {
        Log.i(tag, message);
        if (enableFileLogging) {
            writeToFile("INFO", tag, message);
        }
    }
    
    public void w(String tag, String message) {
        Log.w(tag, message);
        if (enableFileLogging) {
            writeToFile("WARN", tag, message);
        }
    }
    
    public void e(String tag, String message) {
        Log.e(tag, message);
        if (enableFileLogging) {
            writeToFile("ERROR", tag, message);
        }
    }
    
    public void e(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
        if (enableFileLogging) {
            writeToFile("ERROR", tag, message + "\n" + Log.getStackTraceString(throwable));
        }
    }
    
    private void writeToFile(String level, String tag, String message) {
        try {
            File logFile = getLogFile();
            if (logFile.length() > MAX_LOG_SIZE) {
                rotateLogFiles();
            }
            
            String timestamp = dateFormat.format(new Date());
            String logLine = String.format("%s [%s] %s: %s\n", timestamp, level, tag, message);
            
            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.append(logLine);
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to write log to file", e);
        }
    }
    
    private File getLogFile() {
        String date = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
        return new File(logDir, LOG_FILE_PREFIX + date + LOG_FILE_EXTENSION);
    }
    
    private void rotateLogFiles() {
        File[] files = logDir.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION));
        
        if (files != null && files.length > 7) {
            long oldestTime = Long.MAX_VALUE;
            File oldestFile = null;
            
            for (File file : files) {
                if (file.lastModified() < oldestTime) {
                    oldestTime = file.lastModified();
                    oldestFile = file;
                }
            }
            
            if (oldestFile != null) {
                oldestFile.delete();
            }
        }
    }
    
    public void clearOldLogs() {
        File[] files = logDir.listFiles((dir, name) -> 
            name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION));
        
        if (files != null) {
            long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
            for (File file : files) {
                if (file.lastModified() < cutoffTime) {
                    file.delete();
                }
            }
        }
    }
}
