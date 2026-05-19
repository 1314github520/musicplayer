package com.example.musicplayer.core;

import java.io.IOException;

/**
 * 统一错误处理工具类
 * 提供网络错误、用户输入错误等的标准化处理
 */
public class ErrorHandler {
    
    private ErrorHandler() {}
    
    /**
     * 处理网络请求异常并返回用户友好的错误信息
     * @param e 网络异常
     * @return 用户友好的错误提示
     */
    public static String handleNetworkError(IOException e) {
        if (e == null || e.getMessage() == null) {
            return "网络连接失败";
        }
        
        String message = e.getMessage().toLowerCase();
        
        if (message.contains("failed to connect") || message.contains("refused")) {
            return "无法连接到服务器，请检查网络或服务器状态";
        } else if (message.contains("timeout")) {
            return "连接超时，请重试";
        } else if (message.contains("closed") || message.contains("end of stream")) {
            return "连接被意外关闭，请重试";
        } else if (message.contains("unknownhost")) {
            return "无法解析服务器地址";
        } else if (!message.isEmpty()) {
            return "网络错误: " + e.getMessage();
        }
        
        return "网络连接失败";
    }
    
    /**
     * 验证邮箱格式
     * @param email 邮箱地址
     * @return 是否有效
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return email.matches(regex);
    }
    
    /**
     * 验证手机号格式（中国大陆）
     * @param phone 手机号
     * @return 是否有效
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return phone.matches("^1[3-9]\\d{9}$");
    }
    
    /**
     * 验证密码强度
     * @param password 密码
     * @param minLength 最小长度
     * @return 错误信息，null表示验证通过
     */
    public static String validatePassword(String password, int minLength) {
        if (password == null || password.trim().isEmpty()) {
            return "密码不能为空";
        }
        
        if (password.length() < minLength) {
            return "密码至少需要" + minLength + "位字符";
        }
        
        // 可选：检查密码复杂度
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        
        if (!hasLetter || !hasDigit) {
            return "密码需包含字母和数字";
        }
        
        return null; // 验证通过
    }
    
    /**
     * 验证昵称
     * @param nickname 昵称
     * @param maxLength 最大长度
     * @return 错误信息，null表示验证通过
     */
    public static String validateNickname(String nickname, int maxLength) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return "昵称不能为空";
        }
        
        if (nickname.length() > maxLength) {
            return "昵称不能超过" + maxLength + "个字符";
        }
        
        // 检查特殊字符
        if (nickname.contains("<") || nickname.contains(">") || 
            nickname.contains("&") || nickname.contains("\"")) {
            return "昵称包含非法字符";
        }
        
        return null; // 验证通过
    }
}
