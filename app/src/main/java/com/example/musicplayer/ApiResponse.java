package com.example.musicplayer;

public class ApiResponse<T> {
    public int code;
    public String message;
    public T data;
    
    public ApiResponse() {}
    
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }
    
    public boolean isSuccess() {
        return code == 0;
    }
    
    public boolean isError() {
        return code != 0;
    }
    
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, null, data);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(400, message, null);
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
