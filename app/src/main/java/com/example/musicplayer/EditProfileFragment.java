package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import coil.Coil;
import coil.request.ImageRequest;

public class EditProfileFragment extends Fragment {

    private ImageView ivAvatar;
    private TextView tvNickname, tvEmail, tvPhone, tvGender, tvBirthday;
    private Button btnSave;
    private String currentAvatar;
    private Uri pendingUri;
    private MainViewModel mainViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingUri != null) {
            outState.putParcelable("pendingUri", pendingUri);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            pendingUri = savedInstanceState.getParcelable("pendingUri");
        }
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvNickname = view.findViewById(R.id.tvNickname);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvGender = view.findViewById(R.id.tvGender);
        tvBirthday = view.findViewById(R.id.tvBirthday);
        btnSave = view.findViewById(R.id.btnSave);

        loadUserInfo();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());
        btnSave.setOnClickListener(v -> saveProfile());

        view.findViewById(R.id.layoutAvatar).setOnClickListener(v -> pickImage());
        view.findViewById(R.id.layoutNickname).setOnClickListener(v -> showEditDialog("修改昵称", tvNickname));
        view.findViewById(R.id.layoutEmail).setOnClickListener(v -> showEditDialog("修改邮箱", tvEmail));
        view.findViewById(R.id.layoutPhone).setOnClickListener(v -> showEditDialog("修改手机号", tvPhone));
        view.findViewById(R.id.layoutGender).setOnClickListener(v -> showGenderPicker());
        view.findViewById(R.id.layoutBirthday).setOnClickListener(v -> showDatePicker());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserInfo();
    }

    private void showGenderPicker() {
        String[] genders = {"男", "女", "保密"};
        int checkedItem = 2;
        String currentGender = tvGender.getText().toString();
        for (int i = 0; i < genders.length; i++) {
            if (genders[i].equals(currentGender)) {
                checkedItem = i;
                break;
            }
        }

        new android.app.AlertDialog.Builder(getContext(), R.style.DarkDialog)
                .setTitle("选择性别")
                .setSingleChoiceItems(genders, checkedItem, (dialog, which) -> {
                    tvGender.setText(genders[which]);
                    dialog.dismiss();
                })
                .show();
    }

    private void showDatePicker() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        String currentBirthday = tvBirthday.getText().toString();
        if (!"未设置".equals(currentBirthday) && !"null".equals(currentBirthday)) {
            try {
                String[] parts = currentBirthday.split("-");
                calendar.set(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
            } catch (Exception ignored) {}
        }

        new android.app.DatePickerDialog(getContext(), R.style.BlueDatePicker, (view, year, month, dayOfMonth) -> {
            String date = String.format(java.util.Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
            tvBirthday.setText(date);
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private String formatBirthday(String birthday) {
        if (birthday == null || birthday.isEmpty() || "null".equals(birthday)) {
            return "未设置";
        }
        if (birthday.contains("T")) {
            birthday = birthday.split("T")[0];
        }
        if (birthday.contains("Z")) {
            birthday = birthday.replace("Z", "");
        }
        if (birthday.contains(".")) {
            birthday = birthday.split("\\.")[0];
        }
        return birthday;
    }

    private void loadUserInfo() {
        UserManager userManager = UserManager.getInstance(requireContext());
        User user = userManager.getCurrentUser();
        
        if (user != null) {
            tvNickname.setText(user.getNickname());
            tvEmail.setText(user.getEmail());
            tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "未绑定");
            tvGender.setText(user.getGender() != null && !user.getGender().isEmpty() ? user.getGender() : "保密");
            tvBirthday.setText(formatBirthday(user.getBirthday()));
            currentAvatar = user.getAvatar();
            
            if (pendingUri != null) {
                ImageRequest request = new ImageRequest.Builder(requireContext())
                        .data(pendingUri)
                        .target(ivAvatar)
                        .placeholder(R.drawable.music)
                        .error(R.drawable.music)
                        .crossfade(true)
                        .build();
                Coil.imageLoader(requireContext()).enqueue(request);
            } else if (currentAvatar != null && !currentAvatar.isEmpty()) {
                String avatarUrl = currentAvatar.startsWith("http") ? currentAvatar : Constants.API.BASE_URL + currentAvatar;
                if (!avatarUrl.contains("?")) {
                    avatarUrl = avatarUrl + "?t=" + System.currentTimeMillis();
                }
                ImageRequest request = new ImageRequest.Builder(requireContext())
                        .data(avatarUrl)
                        .target(ivAvatar)
                        .placeholder(R.drawable.music)
                        .error(R.drawable.music)
                        .crossfade(true)
                        .build();
                Coil.imageLoader(requireContext()).enqueue(request);
            }
        }
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    pendingUri = result.getData().getData();
                    if (pendingUri != null) {
                        Log.d("EditProfile", "Selected image uri: " + pendingUri);
                        ImageRequest request = new ImageRequest.Builder(requireContext())
                                .data(pendingUri)
                                .target(ivAvatar)
                                .placeholder(R.drawable.music)
                                .error(R.drawable.music)
                                .crossfade(true)
                                .build();
                        Coil.imageLoader(requireContext()).enqueue(request);
                    }
                }
            }
    );

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void showEditDialog(String title, TextView targetView) {
        android.widget.FrameLayout container = new android.widget.FrameLayout(getContext());
        android.widget.EditText editText = new android.widget.EditText(getContext());
        editText.setText(targetView.getText());
        editText.setTextColor(android.graphics.Color.WHITE);
        editText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.accent_teal, null)));

        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (24 * getResources().getDisplayMetrics().density);
        params.setMargins(margin, (int) (8 * getResources().getDisplayMetrics().density), margin, (int) (8 * getResources().getDisplayMetrics().density));
        editText.setLayoutParams(params);
        container.addView(editText);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext(), R.style.DarkDialog)
                .setTitle(title)
                .setView(container)
                .setPositiveButton("确定", (d, which) -> targetView.setText(editText.getText().toString()))
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        // 强制对话框宽度
        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setAttributes(lp);
        }
    }

    private void saveProfile() {
        btnSave.setEnabled(false);
        btnSave.setText("保存中...");

        String nickname = tvNickname.getText().toString().trim();
        String email = tvEmail.getText().toString().trim();
        String phone = tvPhone.getText().toString().trim();
        if ("未绑定".equals(phone) || "null".equals(phone)) phone = "";
        
        String gender = tvGender.getText().toString();
        if ("保密".equals(gender) || "null".equals(gender)) gender = "";
        
        String birthday = tvBirthday.getText().toString();
        if ("未设置".equals(birthday) || "null".equals(birthday) || birthday.isEmpty()) {
            birthday = null;
        }
        
        final String finalEmail = email;
        final String finalPhone = phone;
        final String finalGender = gender;
        final String finalBirthday = birthday;
        if (pendingUri != null) {
            uploadAvatarAndSaveProfile(nickname, finalEmail, finalPhone, finalGender, finalBirthday);
        } else {
            updateProfileInfo(nickname, finalEmail, finalPhone, finalGender, finalBirthday, null);
        }
    }

    private void uploadAvatarAndSaveProfile(String nickname, String email, String phone, String gender, String birthday) {
        String base64Image = uriToBase64(pendingUri);
        if (base64Image == null) {
            btnSave.setEnabled(true);
            btnSave.setText("保存");
            ToastHelper.showShort(getContext(), "图片处理失败");
            return;
        }

        UserManager.getInstance(requireContext()).uploadAvatar(base64Image, new UserManager.UploadCallback() {
            @Override
            public void onSuccess(String avatarUrl) {
                updateProfileInfo(nickname, email, phone, gender, birthday, avatarUrl);
            }
// ... (rest of the method unchanged, will be handled by the replacement)

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("保存");
                        ToastHelper.showShort(getContext(), "头像上传失败: " + message);
                    });
                }
            }
        });
    }

    private void updateProfileInfo(String nickname, String email, String phone, String gender, String birthday, String avatarUrl) {
        UserManager.getInstance(requireContext()).updateProfile(nickname, phone, gender, birthday, avatarUrl, email, new UserManager.UpdateCallback() {
            @Override
            public void onSuccess(User user) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("保存");
                        ToastHelper.showShort(getContext(), "资料已保存");
                        
                        pendingUri = null;
                        
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            currentAvatar = avatarUrl;
                            long updateTime = System.currentTimeMillis();
                            requireContext().getSharedPreferences("avatar_prefs", android.content.Context.MODE_PRIVATE)
                                    .edit()
                                    .putLong("avatar_update_time", updateTime)
                                    .putLong("last_load_time", 0)
                                    .apply();
                            
                            Coil.imageLoader(requireContext()).getMemoryCache().clear();
                        }
                        
                        // 同步更新全局 ViewModel
                        if (mainViewModel != null) {
                            mainViewModel.refreshProfile();
                        }
                        
                        tvNickname.setText(user.getNickname());
                        tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "未绑定");
                        tvGender.setText(user.getGender() != null && !user.getGender().isEmpty() ? user.getGender() : "保密");
                        tvBirthday.setText(formatBirthday(user.getBirthday()));
                        
                        requireActivity().onBackPressed();
                    });
                }
            }

            @Override
            public void onError(String message) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("保存");
                        ToastHelper.showShort(getContext(), message);
                    });
                }
            }
        });
    }

    private String uriToBase64(Uri uri) {
        InputStream inputStream = null;
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            inputStream = requireContext().getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(inputStream, null, options);
            if (inputStream != null) inputStream.close();

            // Calculate inSampleSize to avoid OOM for large images
            options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
            options.inJustDecodeBounds = false;

            // Decode bitmap with inSampleSize set
            inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            
            if (bitmap == null) {
                Log.e("EditProfile", "Failed to decode bitmap from uri: " + uri);
                return null;
            }
            
            // Resize to standard avatar size (500x500)
            int targetSize = 500;
            if (bitmap.getWidth() > targetSize || bitmap.getHeight() > targetSize) {
                float scale = Math.min((float) targetSize / bitmap.getWidth(), (float) targetSize / bitmap.getHeight());
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() * scale), (int) (bitmap.getHeight() * scale), true);
                if (scaledBitmap != bitmap) {
                    bitmap.recycle();
                    bitmap = scaledBitmap;
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
            byte[] bytes = outputStream.toByteArray();
            String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
            Log.d("EditProfile", "Base64 image length: " + base64.length());
            return base64;
        } catch (Exception e) {
            Log.e("EditProfile", "uriToBase64 error", e);
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
