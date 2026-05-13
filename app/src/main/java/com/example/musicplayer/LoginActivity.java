package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegister = findViewById(R.id.tv_register);

        etUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnLogin.setOnClickListener(v -> attemptLogin());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        UserManager userManager = UserManager.getInstance(this);
        if (userManager.isLoggedIn()) {
            navigateToMain();
        }
    }

    private void updateLoginButtonState() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        btnLogin.setEnabled(!username.isEmpty() && !password.isEmpty() && !isLoading);
    }

    private void attemptLogin() {
        final String username = etUsername.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();

        if (username.isEmpty()) {
            ToastHelper.showShort(this, "请输入用户名或邮箱");
            return;
        }

        if (password.isEmpty()) {
            ToastHelper.showShort(this, "请输入密码");
            return;
        }

        isLoading = true;
        btnLogin.setEnabled(false);
        btnLogin.setText("登录中...");

        UserManager.getInstance(this).login(username, password, new UserManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    isLoading = false;
                    btnLogin.setEnabled(true);
                    btnLogin.setText("登录");
                    ToastHelper.showShort(LoginActivity.this, "登录成功");
                    getSharedPreferences("avatar_prefs", MODE_PRIVATE)
                            .edit()
                            .putLong("avatar_update_time", System.currentTimeMillis())
                            .apply();
                    navigateToMain();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    isLoading = false;
                    btnLogin.setEnabled(true);
                    btnLogin.setText("登录");
                    ToastHelper.showShort(LoginActivity.this, message);
                });
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}


