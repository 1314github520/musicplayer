package com.example.musicplayer.feature.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.musicplayer.R;
import com.example.musicplayer.core.ToastHelper;
import com.example.musicplayer.data.model.User;
import com.example.musicplayer.feature.main.MainActivity;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnRegister = findViewById(R.id.btn_register);
        tvLogin = findViewById(R.id.tv_login);

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRegisterButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        etUsername.addTextChangedListener(textWatcher);
        etEmail.addTextChangedListener(textWatcher);
        etPassword.addTextChangedListener(textWatcher);
        etConfirmPassword.addTextChangedListener(textWatcher);

        btnRegister.setOnClickListener(v -> attemptRegister());

        tvLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void updateRegisterButtonState() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        boolean enabled = !username.isEmpty() && !email.isEmpty() && 
                         !password.isEmpty() && !confirmPassword.isEmpty() && 
                         password.equals(confirmPassword) && !isLoading;
        btnRegister.setEnabled(enabled);
    }

    private void attemptRegister() {
        final String username = etUsername.getText().toString().trim();
        final String email = etEmail.getText().toString().trim();
        final String password = etPassword.getText().toString().trim();
        final String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (username.isEmpty()) {
            ToastHelper.showShort(this, "请输入用户名");
            return;
        }

        if (username.length() < 3 || username.length() > 50) {
            ToastHelper.showShort(this, "用户名长度必须在3-50个字符之间");
            return;
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            ToastHelper.showShort(this, "用户名只能包含字母、数字和下划线");
            return;
        }

        if (email.isEmpty()) {
            ToastHelper.showShort(this, "请输入邮箱");
            return;
        }

        if (!isValidEmail(email)) {
            ToastHelper.showShort(this, "请输入有效的邮箱地址");
            return;
        }

        if (password.isEmpty()) {
            ToastHelper.showShort(this, "请输入密码");
            return;
        }

        if (password.length() < 6) {
            ToastHelper.showShort(this, "密码长度至少6位");
            return;
        }

        if (!password.equals(confirmPassword)) {
            ToastHelper.showShort(this, "两次输入的密码不一致");
            return;
        }

        isLoading = true;
        btnRegister.setEnabled(false);
        btnRegister.setText("注册中...");

        UserManager.getInstance(this).register(username, email, password, new UserManager.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(() -> {
                    isLoading = false;
                    btnRegister.setEnabled(true);
                    btnRegister.setText("注册");
                    ToastHelper.showShort(RegisterActivity.this, "注册成功");
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
                    btnRegister.setEnabled(true);
                    btnRegister.setText("注册");
                    ToastHelper.showShort(RegisterActivity.this, message);
                });
            }
        });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }
}