package com.example.musicplayer.core.network;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RetryInterceptor implements Interceptor {
    private final int maxRetryCount;
    
    public RetryInterceptor() {
        this(3);
    }
    
    public RetryInterceptor(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException lastException = null;
        
        for (int i = 0; i < maxRetryCount; i++) {
            try {
                // If we have a previous response that wasn't successful, close it before retrying
                if (response != null) {
                    response.close();
                }
                
                response = chain.proceed(request);
                
                // If successful, return it immediately
                if (response.isSuccessful()) {
                    return response;
                }
                
                // If not successful and it's the last attempt, don't close it, return it
                if (i == maxRetryCount - 1) {
                    return response;
                }
                
            } catch (IOException e) {
                lastException = e;
                // On exception, if it's the last attempt, rethrow
                if (i == maxRetryCount - 1) {
                    throw e;
                }
            }
            
            // Wait before retry
            try {
                Thread.sleep(1000 * (i + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (response != null) response.close();
                throw new IOException("Retry interrupted", e);
            }
        }
        
        if (lastException != null) {
            throw lastException;
        }
        
        return response;
    }
}