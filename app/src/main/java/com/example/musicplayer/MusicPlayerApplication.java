package com.example.musicplayer;

import android.app.Application;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;

public class MusicPlayerApplication extends Application {
    private static final String TAG = "MusicPlayerApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "未捕获的异常在线程: " + thread.getName(), throwable);
            
            String errorInfo = getStackTraceString(throwable);
            Log.e(TAG, "异常详情:\n" + errorInfo);
            
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
        
        Log.d(TAG, "Application初始化完成");
    }
    
    private String getStackTraceString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}
