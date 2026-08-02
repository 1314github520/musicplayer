package com.example.musicplayer.feature.profile;

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
import coil.Coil;
import coil.request.ImageRequest;
import com.example.musicplayer.R;
import com.example.musicplayer.core.Constants;
import com.example.musicplayer.core.ToastHelper;
import com.example.musicplayer.data.model.User;
import com.example.musicplayer.feature.auth.UserManager;
import com.example.musicplayer.feature.main.MainViewModel;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

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
        if (!"未设置".equals(currentBirthday) && !"null".equals(currentBirthday) && currentBirthday.contains("-")) {
            try {
                // 先用 formatBirthday 清洗可能的后缀（T/Z/时间部分），再解析
                String cleaned = formatBirthday(currentBirthday);
                String[] parts = cleaned.split("-");
                if (parts.length >= 3) {
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int day = Integer.parseInt(parts[2]);
                    // 校验日期合法性，避免非法值导致 DatePicker 异常
                    if (year >= 1900 && year <= 2100 && month >= 1 && month <= 12 && day >= 1 && day <= 31) {
                        calendar.set(year, month - 1, day);
                    }
                }
            } catch (Exception e) {
                Log.w("EditProfile", "解析生日失败: " + currentBirthday, e);
            }
        }

        int initYear = calendar.get(java.util.Calendar.YEAR);
        int initMonth = calendar.get(java.util.Calendar.MONTH);
        int initDay = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog dialog = new android.app.DatePickerDialog(
                requireContext(),
                R.style.BlueDatePicker,
                (view, year, month, dayOfMonth) -> {
                    String date = String.format(java.util.Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth);
                    Log.d("EditProfile", "DatePicker 回调: year=" + year + " month=" + (month + 1) + " day=" + dayOfMonth + " -> " + date);
                    tvBirthday.setText(date);
                },
                initYear, initMonth, initDay
        );

        // 给年份 NumberPicker 加《》格式化，视觉标识可独立滚动跨年
        setupYearPickerWithBrackets(dialog, initYear);

        dialog.show();

        // 双重保险：隐藏 CalendarView，强制只显示 Spinner（即使样式未生效也能切年）
        try {
            java.lang.reflect.Method[] methods = android.widget.DatePicker.class.getDeclaredMethods();
            for (java.lang.reflect.Method m : methods) {
                if (m.getName().equals("setCalendarViewShown")) {
                    m.invoke(dialog.getDatePicker(), false);
                    break;
                }
            }
        } catch (Exception ignored) {
            // 反射失败时由样式中的 android:datePickerMode=spinner 兜底
        }

        // 兜底：手动设置按钮颜色，确保浅色模式可见（即使样式未生效）
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.date_picker_accent, null));
        dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(getResources().getColor(R.color.date_picker_text_secondary, null));
    }

    /**
     * 给年份 NumberPicker 加《》格式化，作为可跨年滚动的视觉提示
     * spinner 模式下 DatePicker 内部按 年/月/日 顺序排列 NumberPicker
     */
    private void setupYearPickerWithBrackets(android.app.DatePickerDialog dialog, int currentYear) {
        try {
            android.widget.DatePicker datePicker = dialog.getDatePicker();
            // 遍历子视图找到所有 NumberPicker
            java.util.List<android.widget.NumberPicker> pickers = new java.util.ArrayList<>();
            findNumberPickers(datePicker, pickers);
            if (!pickers.isEmpty()) {
                // spinner 模式下第一个 NumberPicker 是年份
                android.widget.NumberPicker yearPicker = pickers.get(0);
                yearPicker.setFormatter(value -> "《 " + value + " 》");
                // 设置 formatter 后需要触发重绘才能生效
                yearPicker.invalidate();
            }
        } catch (Exception e) {
            Log.w("EditProfile", "设置年份《》格式失败", e);
        }
    }

    /**
     * 递归查找 ViewGroup 中的所有 NumberPicker
     */
    private void findNumberPickers(View view, java.util.List<android.widget.NumberPicker> out) {
        if (view instanceof android.widget.NumberPicker) {
            out.add((android.widget.NumberPicker) view);
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                findNumberPickers(group.getChildAt(i), out);
            }
        }
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
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
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
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
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
                        try {
                            requireContext().getContentResolver().takePersistableUriPermission(
                                    pendingUri, 
                                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                        } catch (Exception e) {
                            Log.d("EditProfile", "Could not take persistable permission: " + e.getMessage());
                        }
                        try {
                            // 释放旧的Bitmap防止内存泄漏
                            releaseAvatarBitmap();
                            
                            android.graphics.Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                                    requireContext().getContentResolver(), pendingUri);
                            
                            if (bitmap != null) {
                                ivAvatar.setImageBitmap(bitmap);
                                Log.d("EditProfile", "Successfully loaded bitmap, size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                            } else {
                                ivAvatar.setImageURI(pendingUri);
                            }
                        } catch (Exception e) {
                            Log.e("EditProfile", "Failed to load bitmap", e);
                            ivAvatar.setImageURI(pendingUri);
                        }
                    }
                }
            }
    );
    
    /**
     * 释放ImageView中当前的Bitmap资源
     * 防止多次选择图片导致内存泄漏和OOM
     */
    private void releaseAvatarBitmap() {
        if (ivAvatar == null) return;
        
        android.graphics.drawable.Drawable drawable = ivAvatar.getDrawable();
        if (drawable instanceof android.graphics.drawable.BitmapDrawable) {
            android.graphics.drawable.BitmapDrawable bitmapDrawable = (android.graphics.drawable.BitmapDrawable) drawable;
            android.graphics.Bitmap bitmap = bitmapDrawable.getBitmap();
            
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                Log.d("EditProfile", "Old avatar bitmap recycled");
            }
        }
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void showEditDialog(String title, TextView targetView) {
        android.widget.FrameLayout container = new android.widget.FrameLayout(getContext());
        android.widget.EditText editText = new android.widget.EditText(getContext());
        editText.setText(targetView.getText());
        editText.setTextColor(getResources().getColor(R.color.text_primary, null));
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

        // 统一设置按钮颜色（兜底方案，如果 Style 未生效）
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.accent_teal, null));
        dialog.getButton(android.content.DialogInterface.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.text_grey, null));

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
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Fragment销毁时释放Bitmap资源，防止内存泄漏
        releaseAvatarBitmap();
    }
}
