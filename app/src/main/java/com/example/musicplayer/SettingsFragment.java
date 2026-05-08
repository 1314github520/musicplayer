package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;

public class SettingsFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView tvCurrentVersion;
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

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        tvCurrentVersion.setText("当前版本: " + getAppVersionName());

        view.findViewById(R.id.layoutCheckUpdate).setOnClickListener(v -> checkForUpdate());

        return view;
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
        tvUpdateLog.setText(versionInfo.updateLog);

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
