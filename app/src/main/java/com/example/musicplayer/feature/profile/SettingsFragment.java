package com.example.musicplayer.feature.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.musicplayer.R;
import com.example.musicplayer.core.ToastHelper;
import com.example.musicplayer.core.theme.ThemeManager;
import com.example.musicplayer.feature.auth.LoginActivity;
import com.example.musicplayer.feature.auth.UserManager;
import com.example.musicplayer.feature.main.MainActivity;
import com.example.musicplayer.feature.update.UpdateManager;
import java.io.File;

public class SettingsFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView tvCurrentVersion;
    private TextView tvCurrentTheme;
    private androidx.appcompat.app.AlertDialog downloadDialog;
    private androidx.appcompat.app.AlertDialog updateDialog;
    private ProgressBar downloadProgressBar;
    private TextView tvProgress;
    private TextView tvStatus;
    private boolean isChecking = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        tvCurrentVersion = view.findViewById(R.id.tvCurrentVersion);
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        tvCurrentVersion.setText("当前版本: " + getAppVersionName());
        updateThemeSummary();

        view.findViewById(R.id.layoutTheme).setOnClickListener(v -> showThemeChooser());
        view.findViewById(R.id.layoutCheckUpdate).setOnClickListener(v -> checkForUpdate());
        view.findViewById(R.id.layoutChangePassword).setOnClickListener(v -> showChangePasswordDialog());
        view.findViewById(R.id.layoutAbout).setOnClickListener(v -> showAboutDialog());
        view.findViewById(R.id.layoutLogout).setOnClickListener(v -> showLogoutDialog());
        view.findViewById(R.id.layoutDeleteAccount).setOnClickListener(v -> confirmDeleteAccount());

        return view;
    }

    private void showThemeChooser() {
        String[] themes = {"浅色模式", "深色模式", "跟随系统"};
        int currentMode = ThemeManager.getInstance(requireContext()).getThemeMode();
        int selection = 2; // Default to System
        if (currentMode == ThemeManager.THEME_LIGHT) selection = 0;
        else if (currentMode == ThemeManager.THEME_DARK) selection = 1;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.DarkDialog)
                .setTitle("选择主题模式")
                .setSingleChoiceItems(themes, selection, (dialog, which) -> {
                    int mode;
                    if (which == 0) mode = ThemeManager.THEME_LIGHT;
                    else if (which == 1) mode = ThemeManager.THEME_DARK;
                    else mode = ThemeManager.THEME_SYSTEM;

                    ThemeManager.getInstance(requireContext()).setThemeMode(mode);
                    updateThemeSummary();
                    dialog.dismiss();
                    // 重启 Activity 以应用主题
                    requireActivity().recreate();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void updateThemeSummary() {
        if (tvCurrentTheme != null) {
            tvCurrentTheme.setText(ThemeManager.getInstance(requireContext()).getThemeName(requireContext()));
        }
    }

    private void confirmDeleteAccount() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm, null);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        tvMessage.setText("确定要注销并永久删除该账号吗？此操作不可撤销。");

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            deleteAccount();
        });

        dialog.show();
    }

    private void deleteAccount() {
        UserManager.getInstance(requireContext()).deleteAccount(new UserManager.DeleteAccountCallback() {
            @Override
            public void onSuccess() {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        ToastHelper.showShort(getContext(), "账号已注销");
                        requireActivity().finish();
                        android.content.Intent intent = new android.content.Intent(requireContext(), LoginActivity.class);
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        ToastHelper.showShort(getContext(), "注销失败: " + message);
                    });
                }
            }
        });
    }

    private void showAboutDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_about, null);
        TextView tvVersion = dialogView.findViewById(R.id.tvVersion);
        TextView tvDeveloper = dialogView.findViewById(R.id.tvDeveloper);
        TextView tvContact = dialogView.findViewById(R.id.tvContact);
        TextView tvCopyright = dialogView.findViewById(R.id.tvCopyright);
        View btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        tvVersion.setText(getAppVersionName());
        tvDeveloper.setText("xqf");
        tvContact.setText("email@584399.xyz");
        tvCopyright.setText("Copyright © 2026 xqf.\nAll Rights Reserved.");

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnConfirm.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showLogoutDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm, null);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        tvMessage.setText("确定要退出登录吗？");

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).pauseAllBackgroundTasks();
            }
            UserManager.getInstance(requireContext()).logout();
            android.content.Intent intent = new android.content.Intent(requireContext(), LoginActivity.class);
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        dialog.show();
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        com.google.android.material.textfield.TextInputLayout tilNew = dialogView.findViewById(R.id.tilNewPassword);
        com.google.android.material.textfield.TextInputLayout tilConfirm = dialogView.findViewById(R.id.tilConfirmPassword);
        
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);
        
        View btnCancel = dialogView.findViewById(R.id.btnCancel);
        View btnConfirm = dialogView.findViewById(R.id.btnConfirm);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            tilNew.setError(null);
            tilConfirm.setError(null);

            if (newPassword.isEmpty()) {
                tilNew.setError("请输入新密码");
                return;
            }

            if (newPassword.length() < 6) {
                tilNew.setError("新密码至少6位");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                tilConfirm.setError("两次输入的密码不一致");
                return;
            }

            changePassword(newPassword);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void changePassword(String newPassword) {
        UserManager.getInstance(requireContext()).changePassword(newPassword, new UserManager.PasswordCallback() {
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    ToastHelper.showShort(getContext(), "密码修改成功");
                });
            }

            @Override
            public void onError(String message) {
                requireActivity().runOnUiThread(() -> {
                    ToastHelper.showShort(getContext(), message);
                });
            }
        });
    }

    private String getAppVersionName() {
        try {
            return requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0";
        }
    }

    private void checkForUpdate() {
        if (isChecking) {
            return;
        }
        
        if (updateDialog != null && updateDialog.isShowing()) {
            return;
        }
        
        if (downloadDialog != null && downloadDialog.isShowing()) {
            return;
        }
        
        isChecking = true;
        progressBar.setVisibility(View.VISIBLE);

        UpdateManager.getInstance(requireContext()).checkUpdate(new UpdateManager.CheckUpdateCallback() {
            @Override
            public void onUpdateAvailable(UpdateManager.VersionInfo versionInfo) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        isChecking = false;
                        showUpdateDialog(versionInfo);
                    });
                }
            }

            @Override
            public void onNoUpdate() {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        isChecking = false;
                        ToastHelper.showShort(getContext(), "已是最新版本");
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        isChecking = false;
                        ToastHelper.showShort(getContext(), "检查更新失败: " + message);
                    });
                }
            }
        });
    }

    private void showUpdateDialog(UpdateManager.VersionInfo versionInfo) {
        if (updateDialog != null && updateDialog.isShowing()) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update, null);
        TextView tvVersionName = dialogView.findViewById(R.id.tvVersionName);
        TextView tvUpdateLog = dialogView.findViewById(R.id.tvUpdateLog);
        View btnLater = dialogView.findViewById(R.id.btnLater);
        View btnUpdate = dialogView.findViewById(R.id.btnUpdate);

        tvVersionName.setText(versionInfo.versionName);

        // 防御性处理：确保更新日志不为空
        String updateLogText = versionInfo.updateLog;
        if (updateLogText == null || updateLogText.trim().isEmpty()) {
            updateLogText = "1. 性能优化\n2. Bug修复\n3. 体验提升";
            android.util.Log.w("SettingsFragment", "updateLog为空,使用默认内容");
        }
        tvUpdateLog.setText(updateLogText);

        android.util.Log.d("SettingsFragment", "显示更新对话框: " + versionInfo.versionName +
                           ", 日志长度=" + updateLogText.length());

        updateDialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .setOnCancelListener(dialog -> updateDialog = null)
            .create();

        if (updateDialog.getWindow() != null) {
            updateDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnLater.setOnClickListener(v -> {
            if (updateDialog != null) {
                updateDialog.dismiss();
                updateDialog = null;
            }
        });

        btnUpdate.setOnClickListener(v -> {
            if (updateDialog != null) {
                updateDialog.dismiss();
                updateDialog = null;
            }
            startDownload(versionInfo);
        });

        updateDialog.show();
    }

    private void startDownload(UpdateManager.VersionInfo versionInfo) {
        if (downloadDialog != null && downloadDialog.isShowing()) {
            return;
        }
        
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).pauseAllBackgroundTasks();
        }
        
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_download_progress, null);
        downloadProgressBar = dialogView.findViewById(R.id.progressBar);
        tvProgress = dialogView.findViewById(R.id.tvProgress);
        tvStatus = dialogView.findViewById(R.id.tvStatus);

        downloadDialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create();

        if (downloadDialog.getWindow() != null) {
            downloadDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        downloadDialog.show();

        UpdateManager.getInstance(requireContext()).downloadApk(versionInfo, new UpdateManager.DownloadCallback() {
            @Override
            public void onProgress(int progress) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        downloadProgressBar.setProgress(progress);
                        tvProgress.setText(progress + "%");
                        if (progress < 100) {
                            tvStatus.setText("正在下载...");
                        } else {
                            tvStatus.setText("下载完成，正在安装...");
                        }
                    });
                }
            }

            @Override
            public void onSuccess(File apkFile) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        if (downloadDialog != null && downloadDialog.isShowing()) {
                            downloadDialog.dismiss();
                            downloadDialog = null;
                        }
                        ToastHelper.showShort(getContext(), "下载完成");
                        UpdateManager.getInstance(requireContext()).installApk(apkFile);
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        if (downloadDialog != null && downloadDialog.isShowing()) {
                            downloadDialog.dismiss();
                            downloadDialog = null;
                        }
                        ToastHelper.showLong(getContext(), message);
                    });
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (updateDialog != null && updateDialog.isShowing()) {
            updateDialog.dismiss();
        }
        if (downloadDialog != null && downloadDialog.isShowing()) {
            downloadDialog.dismiss();
        }
    }
}
