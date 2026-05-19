package com.example.musicplayer.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class ToastHelper {
    private static final int SHORT_DURATION = 720; // 0.75秒
    private static final int LONG_DURATION = 2000; // 2秒
    private static Toast currentToast;
    private static Handler handler;
    
    private static Handler getHandler() {
        if (handler == null) {
            synchronized (ToastHelper.class) {
                if (handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return handler;
    }
    
    public static void showShort(Context context, String message) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        currentToast = Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT);
        currentToast.show();
        
        getHandler().postDelayed(() -> {
            if (currentToast != null) {
                currentToast.cancel();
            }
        }, SHORT_DURATION);
    }
    
    public static void showShort(Context context, int resId) {
        showShort(context, context.getString(resId));
    }
    
    public static void showLong(Context context, String message) {
        if (currentToast != null) {
            currentToast.cancel();
        }
        
        currentToast = Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG);
        currentToast.show();
        
        getHandler().postDelayed(() -> {
            if (currentToast != null) {
                currentToast.cancel();
            }
        }, LONG_DURATION);
    }
    
    public static void showLong(Context context, int resId) {
        showLong(context, context.getString(resId));
    }
    
    public static void cancelAll() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (currentToast != null) {
            currentToast.cancel();
            currentToast = null;
        }
    }
}
