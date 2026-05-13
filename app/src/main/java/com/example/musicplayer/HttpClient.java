package com.example.musicplayer;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class HttpClient {
    private static volatile OkHttpClient instance;
    
    private HttpClient() {}
    
    public static OkHttpClient getInstance() {
        if (instance == null) {
            synchronized (HttpClient.class) {
                if (instance == null) {
                    instance = new OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .retryOnConnectionFailure(true)
                        .addInterceptor(new RetryInterceptor(3))
                        .build();
                }
            }
        }
        return instance;
    }
}
