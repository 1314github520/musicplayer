package com.example.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserManager {
    private static final String TAG = "UserManager";
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_USER_DATA = "user_data";
    private static final String KEY_TOKEN = "token";

    private static volatile UserManager instance;
    private final Context context;
    private final SharedPreferences preferences;
    private final OkHttpClient httpClient;
    private volatile User currentUser;
    
    // 统一线程池管理，避免频繁创建销毁线程
    private final ExecutorService networkExecutor;

    private UserManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.httpClient = HttpClient.getInstance();
        // 初始化网络请求线程池（3个线程足够处理并发请求）
        this.networkExecutor = Executors.newFixedThreadPool(3);
        loadUserFromPrefs();
    }

    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context);
        }
        return instance;
    }

    private void loadUserFromPrefs() {
        String userData = preferences.getString(KEY_USER_DATA, null);
        if (userData != null) {
            try {
                JSONObject json = new JSONObject(userData);
                currentUser = User.fromJson(json);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to parse user data", e);
                clearUserData();
            }
        }
    }

    private void saveUserToPrefs(User user) {
        try {
            JSONObject json = new JSONObject();
            json.put("userId", user.getUserId());
            json.put("username", user.getUsername());
            json.put("email", user.getEmail());
            json.put("nickname", user.getNickname() != null ? user.getNickname() : "");
            json.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            json.put("phone", user.getPhone() != null ? user.getPhone() : "");
            json.put("gender", user.getGender() != null ? user.getGender() : "");
            json.put("birthday", user.getBirthday() != null ? user.getBirthday() : "");
            json.put("token", user.getToken() != null ? user.getToken() : "");
            
            String jsonString = json.toString();
            preferences.edit()
                    .putString(KEY_USER_DATA, jsonString)
                    .putString(KEY_TOKEN, user.getToken())
                    .apply();
            currentUser = user;
        } catch (JSONException e) {
            Log.e(TAG, "Failed to save user data", e);
        }
    }

    private void clearUserData() {
        preferences.edit()
                .remove(KEY_USER_DATA)
                .remove(KEY_TOKEN)
                .apply();
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null && currentUser.isLoggedIn();
    }

    public String getToken() {
        return currentUser != null ? currentUser.getToken() : null;
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public void register(String username, String email, String password, @NonNull AuthCallback callback) {
        networkExecutor.execute(() -> {
            try (Response response = executeRegisterRequest(username, email, password)) {
                if (response == null) return;
                
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    String errorMessage = parseErrorMessage(responseBody);
                    callback.onError(errorMessage);
                    return;
                }

                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.getInt("code") == 0) {
                    User user = User.fromJson(jsonResponse.getJSONObject("data"));
                    saveUserToPrefs(user);
                    callback.onSuccess(user);
                } else {
                    String errorMessage = jsonResponse.optString("message", "注册失败");
                    callback.onError(errorMessage);
                }
            } catch (IOException e) {
                Log.e(TAG, "Registration network error", e);
                callback.onError(ErrorHandler.handleNetworkError(e));
            } catch (JSONException e) {
                Log.e(TAG, "Registration parse error", e);
                callback.onError("解析服务器响应失败");
            }
        });
    }
    
    private Response executeRegisterRequest(String username, String email, String password) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(Constants.API.BASE_URL + "/api/user/register")
                .post(requestBody)
                .build();

        Response response = httpClient.newCall(request).execute();
        return response;
    }

    public void login(String username, String password, @NonNull AuthCallback callback) {
        networkExecutor.execute(() -> {
            try (Response response = executeLoginRequest(username, password)) {
                if (response == null) return;
                
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    String errorMessage = parseErrorMessage(responseBody);
                    callback.onError(errorMessage);
                    return;
                }

                if (responseBody.isEmpty()) {
                    callback.onError("服务器返回空响应");
                    return;
                }

                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.getInt("code") == 0) {
                    JSONObject data = jsonResponse.optJSONObject("data");
                    if (data == null) {
                        callback.onError("响应格式错误");
                        return;
                    }
                    User user = User.fromJson(data);
                    saveUserToPrefs(user);
                    callback.onSuccess(user);
                } else {
                    String errorMessage = jsonResponse.optString("message", "登录失败");
                    callback.onError(errorMessage);
                }
            } catch (IOException e) {
                Log.e(TAG, "Network error during login", e);
                callback.onError(ErrorHandler.handleNetworkError(e));
            } catch (JSONException e) {
                Log.e(TAG, "JSON parse error during login", e);
                callback.onError("服务器响应格式错误");
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error during login", e);
                callback.onError("登录异常: " + e.getMessage());
            }
        });
    }
    
    private Response executeLoginRequest(String username, String password) throws IOException, JSONException {
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(Constants.API.BASE_URL + "/api/user/login")
                .post(requestBody)
                .build();

        return httpClient.newCall(request).execute();
    }

    public interface ProfileCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public void getProfile(@NonNull ProfileCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("未登录");
            return;
        }

        networkExecutor.execute(() -> {
            try (Response response = executeGetProfileRequest()) {
                if (response == null) return;
                
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    if (response.code() == 401) {
                        logout();
                    }
                    String errorMessage = parseErrorMessage(responseBody);
                    callback.onError(errorMessage);
                    return;
                }

                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.getInt("code") == 0) {
                    User user = User.fromJson(jsonResponse.getJSONObject("data"));
                    user.setToken(currentUser.getToken());
                    saveUserToPrefs(user);
                    callback.onSuccess(user);
                } else {
                    String errorMessage = jsonResponse.optString("message", "获取个人信息失败");
                    callback.onError(errorMessage);
                }
            } catch (IOException e) {
                Log.e(TAG, "Get profile network error", e);
                callback.onError(ErrorHandler.handleNetworkError(e));
            } catch (JSONException e) {
                Log.e(TAG, "Get profile parse error", e);
                callback.onError("解析服务器响应失败");
            }
        });
    }
    
    private Response executeGetProfileRequest() throws IOException {
        Request request = new Request.Builder()
                .url(Constants.API.BASE_URL + "/api/user/profile")
                .header("Authorization", "Bearer " + getToken())
                .get()
                .build();

        return httpClient.newCall(request).execute();
    }

    public interface UpdateCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    public interface UploadCallback {
        void onSuccess(String avatarUrl);
        void onError(String message);
    }

    public void uploadAvatar(String base64Image, @NonNull UploadCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("未登录");
            return;
        }

        networkExecutor.execute(() -> {
            try (Response response = executeUploadAvatarRequest(base64Image)) {
                if (response == null) return;
                
                String responseBody = response.body() != null ? response.body().string() : "";
                
                Log.d(TAG, "Upload response code: " + response.code());
                Log.d(TAG, "Upload response body: " + responseBody);

                if (!response.isSuccessful()) {
                    String errorMsg = parseErrorMessage(responseBody);
                    Log.e(TAG, "Upload failed: " + errorMsg);
                    callback.onError(errorMsg);
                    return;
                }

                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.getInt("code") == 0) {
                    String avatarUrl = jsonResponse.getJSONObject("data").getString("url");
                    Log.d(TAG, "Upload success, avatar URL: " + avatarUrl);
                    callback.onSuccess(avatarUrl);
                } else {
                    String errorMsg = jsonResponse.optString("message", "上传失败");
                    Log.e(TAG, "Upload failed with message: " + errorMsg);
                    callback.onError(errorMsg);
                }
            } catch (IOException e) {
                Log.e(TAG, "Upload avatar IO error", e);
                callback.onError(ErrorHandler.handleNetworkError(e));
            } catch (JSONException e) {
                Log.e(TAG, "Upload avatar JSON error", e);
                callback.onError("服务器响应格式错误");
            }
        });
    }
    
    private Response executeUploadAvatarRequest(String base64Image) throws IOException, JSONException {
        Log.d(TAG, "Uploading avatar, base64 length: " + base64Image.length());
        
        JSONObject body = new JSONObject();
        body.put("image", base64Image);

        RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.parse("application/json")
        );

        String url = Constants.API.BASE_URL + "/api/user/upload-avatar";
        Log.d(TAG, "Upload URL: " + url);

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + getToken())
                .post(requestBody)
                .build();

        return httpClient.newCall(request).execute();
    }

    public void updateProfile(String nickname, String phone, String gender, String birthday, String avatar, String email, @NonNull UpdateCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("未登录");
            return;
        }

        new Thread(() -> {
            Response response = null;
            try {
                JSONObject body = new JSONObject();
                if (nickname != null && !nickname.isEmpty()) {
                    body.put("nickname", nickname);
                }
                if (email != null && !email.isEmpty()) {
                    body.put("email", email);
                }
                if (phone != null) {
                    body.put("phone", phone);
                }
                if (gender != null) {
                    body.put("gender", gender);
                }
                if (birthday != null) {
                    body.put("birthday", birthday);
                }
                if (avatar != null) {
                    body.put("avatar", avatar);
                }

                RequestBody requestBody = RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(Constants.API.BASE_URL + "/api/user/profile")
                        .header("Authorization", "Bearer " + getToken())
                        .put(requestBody)
                        .build();

                response = httpClient.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    if (response.code() == 401) {
                        logout();
                    }
                    String errorMessage = parseErrorMessage(responseBody);
                    callback.onError(errorMessage);
                    return;
                }

                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.getInt("code") == 0) {
                    User user = User.fromJson(jsonResponse.getJSONObject("data"));
                    user.setToken(currentUser.getToken());
                    saveUserToPrefs(user);
                    currentUser = user;
                    callback.onSuccess(user);
                } else {
                    String errorMessage = jsonResponse.optString("message", "更新个人信息失败");
                    callback.onError(errorMessage);
                }

            } catch (IOException e) {
                Log.e(TAG, "Update profile network error", e);
                callback.onError("网络错误: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Update profile parse error", e);
                callback.onError("解析服务器响应失败");
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }).start();
    }

    public interface PasswordCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface DeleteAccountCallback {
        void onSuccess();
        void onError(String message);
    }

    public void deleteAccount(@NonNull DeleteAccountCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("未登录");
            return;
        }

        new Thread(() -> {
            Response response = null;
            try {
                Request request = new Request.Builder()
                        .url(Constants.API.BASE_URL + "/api/user/delete")
                        .header("Authorization", "Bearer " + getToken())
                        .post(RequestBody.create("", MediaType.parse("application/json")))
                        .build();

                response = httpClient.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    String errorMessage = parseErrorMessage(responseBody);
                    callback.onError(errorMessage);
                    return;
                }

                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.getInt("code") == 0) {
                    logout();
                    callback.onSuccess();
                } else {
                    String errorMessage = jsonResponse.optString("message", "注销账号失败");
                    callback.onError(errorMessage);
                }

            } catch (IOException e) {
                Log.e(TAG, "Delete account network error", e);
                callback.onError("网络错误: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Delete account parse error", e);
                callback.onError("解析服务器响应失败");
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }).start();
    }

    public void changePassword(String newPassword, @NonNull PasswordCallback callback) {
        if (!isLoggedIn()) {
            callback.onError("未登录");
            return;
        }

        new Thread(() -> {
            Response response = null;
            try {
                JSONObject body = new JSONObject();
                body.put("newPassword", newPassword);

                RequestBody requestBody = RequestBody.create(
                        body.toString(),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(Constants.API.BASE_URL + "/api/user/change-password")
                        .header("Authorization", "Bearer " + getToken())
                        .post(requestBody)
                        .build();

                response = httpClient.newCall(request).execute();
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    if (response.code() == 401) {
                        logout();
                    }
                    String errorMessage = parseErrorMessage(responseBody);
                    callback.onError(errorMessage);
                    return;
                }

                JSONObject jsonResponse = new JSONObject(responseBody);
                if (jsonResponse.getInt("code") == 0) {
                    callback.onSuccess();
                } else {
                    String errorMessage = jsonResponse.optString("message", "修改密码失败");
                    callback.onError(errorMessage);
                }

            } catch (IOException e) {
                Log.e(TAG, "Change password network error", e);
                callback.onError("网络错误: " + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Change password parse error", e);
                callback.onError("解析服务器响应失败");
            } finally {
                if (response != null) {
                    response.close();
                }
            }
        }).start();
    }

    public void logout() {
        clearUserData();
        // 清理 UI 层使用的用户信息缓存
        context.getSharedPreferences("user_profile", Context.MODE_PRIVATE).edit().clear().apply();
        // 清理头像更新时间缓存
        context.getSharedPreferences("avatar_prefs", Context.MODE_PRIVATE).edit().clear().apply();
        
        // 清理临时缓存（保留用户的个人数据：最近播放、收藏）
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            // ❌ 已移除：不再清空最近播放记录（db.recentPlayDao().deleteAll()）
            // 原因：这是用户的个人数据，重新登录后应该保留
            
            // ✅ 保留：只删除远程歌曲（需要重新从服务器获取）
            db.songDao().deleteRemoteSongs();
            
            // ❌ 已移除：不再清空收藏数据（db.songDao().clearFavorites()）
            // 原因：收藏是用户的个人偏好，跨登录会话应保持一致
            
            // ✅ 保留：清理歌词缓存（可按需重新下载）
            LyricCacheManager.getInstance(context).clearCache();
            
            Log.d(TAG, "Logout completed: cleared temporary cache, preserved user data");
        }).start();
    }

    private String parseErrorMessage(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json.optString("message", "Unknown error");
        } catch (JSONException e) {
            return "Server error";
        }
    }
}
