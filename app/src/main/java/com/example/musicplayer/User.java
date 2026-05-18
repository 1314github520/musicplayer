package com.example.musicplayer;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class User {
    private int userId;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String phone;
    private String gender;
    private String birthday;
    private String token;
    private long createdAt;

    public User() {}

    public User(int userId, String username, String email, String nickname, String avatar, String phone, String gender, String birthday, String token) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.nickname = nickname;
        this.avatar = avatar;
        this.phone = phone;
        this.gender = gender;
        this.birthday = birthday;
        this.token = token;
    }

    public static User fromJson(JSONObject json) throws JSONException {
        User user = new User();
        user.setUserId(json.getInt("userId"));
        user.setUsername(json.getString("username"));
        user.setEmail(json.getString("email"));
        user.setNickname(getOptString(json, "nickname"));
        user.setAvatar(getOptString(json, "avatar"));
        user.setPhone(getOptString(json, "phone"));
        user.setGender(getOptString(json, "gender"));
        user.setBirthday(getOptString(json, "birthday"));
        
        String token = getOptString(json, "token");
        if (token.isEmpty()) {
            token = getOptString(json, "accessToken");
            android.util.Log.w("User", "token字段为空, 尝试读取accessToken: " + 
                              (token.isEmpty() ? "未找到" : "找到"));
        }
        if (token.isEmpty()) {
            android.util.Log.w("User", "警告: JSON中未找到有效的token字段! 可用的keys: " + json.keys());
        }
        user.setToken(token);
        
        if (json.has("createdAt")) {
            user.setCreatedAt(json.getLong("createdAt"));
        }
        
        android.util.Log.d("User", "解析用户数据: userId=" + user.getUserId() + 
                           ", username=" + user.getUsername() + 
                           ", token长度=" + token.length());
        
        return user;
    }

    private static String getOptString(JSONObject json, String key) {
        if (json.isNull(key)) {
            return "";
        }
        String val = json.optString(key, "");
        return "null".equals(val) ? "" : val;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }

    @NonNull
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", nickname='" + nickname + '\'' +
                ", avatar='" + avatar + '\'' +
                ", phone='" + phone + '\'' +
                ", gender='" + gender + '\'' +
                ", birthday='" + birthday + '\'' +
                ", token='" + (token != null ? "***" : null) + '\'' +
                '}';
    }
}

