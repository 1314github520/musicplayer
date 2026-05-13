package com.example.musicplayer;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

public class ErrorHandler {
    private static final String TAG = "ErrorHandler";
    
    public static String getErrorMessage(Context context, Throwable throwable) {
        if (throwable == null) {
            return "未知错误";
        }
        
        String message;
        
        if (throwable instanceof SocketTimeoutException) {
            message = "网络连接超时，请检查网络设置";
            Log.w(TAG, "Network timeout", throwable);
        } else if (throwable instanceof UnknownHostException) {
            message = "无法连接到服务器，请检查网络连接";
            Log.w(TAG, "Unknown host", throwable);
        } else if (throwable instanceof SSLException) {
            message = "安全连接失败，请检查网络设置";
            Log.e(TAG, "SSL error", throwable);
        } else if (throwable instanceof IOException) {
            message = "网络请求失败: " + throwable.getMessage();
            Log.e(TAG, "IO error", throwable);
        } else if (throwable instanceof SecurityException) {
            message = "权限不足，请授予相应权限";
            Log.e(TAG, "Security error", throwable);
        } else if (throwable instanceof OutOfMemoryError) {
            message = "内存不足，请关闭其他应用后重试";
            Log.e(TAG, "Out of memory", throwable);
        } else {
            message = "操作失败: " + throwable.getMessage();
            Log.e(TAG, "Unknown error", throwable);
        }
        
        return message;
    }
    
    public static void logException(String tag, String message, Throwable throwable) {
        Log.e(tag, message, throwable);
    }
    
    public static void logWarning(String tag, String message) {
        Log.w(tag, message);
    }
    
    public static void logInfo(String tag, String message) {
        Log.i(tag, message);
    }
    
    public static void logDebug(String tag, String message) {
        Log.d(tag, message);
    }
}
