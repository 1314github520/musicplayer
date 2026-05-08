package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.musicplayer.R;

import coil.Coil;
import coil.request.ImageRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class EditProfileFragment extends Fragment {

    private MainViewModel viewModel;
    private ImageView ivAvatar;
    private TextView tvNickname, tvSignature, tvUserId, tvGender, tvBirthday;
    private String currentAvatarUri;
    private Uri pendingUri; // 临时记录用户选取的相册URI

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        ivAvatar = view.findViewById(R.id.ivAvatar);
        tvNickname = view.findViewById(R.id.tvNickname);
        tvSignature = view.findViewById(R.id.tvSignature);
        tvUserId = view.findViewById(R.id.tvUserId);
        tvGender = view.findViewById(R.id.tvGender);
        tvBirthday = view.findViewById(R.id.tvBirthday);

        setupObservers();

        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());
        view.findViewById(R.id.btnSave).setOnClickListener(v -> saveProfile());

        view.findViewById(R.id.layoutAvatar).setOnClickListener(v -> pickImage());
        view.findViewById(R.id.layoutNickname).setOnClickListener(v -> showEditDialog("修改昵称", tvNickname));
        view.findViewById(R.id.layoutSignature).setOnClickListener(v -> showEditDialog("修改签名", tvSignature));
        view.findViewById(R.id.layoutGender).setOnClickListener(v -> showGenderDialog());
        view.findViewById(R.id.layoutBirthday).setOnClickListener(v -> showBirthdayDialog());

        return view;
    }

    private void setupObservers() {
        viewModel.getUserNickname().observe(getViewLifecycleOwner(), name -> tvNickname.setText(name));
        viewModel.getUserSignature().observe(getViewLifecycleOwner(), sig -> tvSignature.setText(sig));
        viewModel.getUserId().observe(getViewLifecycleOwner(), id -> tvUserId.setText(id));
        viewModel.getUserGender().observe(getViewLifecycleOwner(), gender -> tvGender.setText(gender));
        viewModel.getUserBirthday().observe(getViewLifecycleOwner(), birthday -> tvBirthday.setText(birthday));
        viewModel.getUserAvatarUri().observe(getViewLifecycleOwner(), uri -> {
            currentAvatarUri = uri;
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(uri)
                    .target(ivAvatar)
                    .build();
            Coil.imageLoader(requireContext()).enqueue(request);
        });
    }

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    pendingUri = result.getData().getData();
                    ivAvatar.setImageURI(pendingUri); // 先预览
                }
            }
    );

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void showEditDialog(String title, TextView targetView) {
        android.widget.EditText editText = new android.widget.EditText(getContext());
        editText.setText(targetView.getText());
        new android.app.AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> targetView.setText(editText.getText().toString()))
                .setNegativeButton("取消", null)
                .show();
    }

    private void showGenderDialog() {
        String[] genders = {"男", "女"};
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("选择性别")
                .setItems(genders, (dialog, which) -> tvGender.setText(genders[which]))
                .show();
    }

    private void showBirthdayDialog() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(getContext(), (view, year, month, dayOfMonth) -> {
            String date = year + "-" + (month + 1) + "-" + dayOfMonth;
            tvBirthday.setText(date);
        }, calendar.get(java.util.Calendar.YEAR), calendar.get(java.util.Calendar.MONTH), calendar.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    private void saveProfile() {
        String finalAvatar = currentAvatarUri;
        if (pendingUri != null) {
            // 将选取的相册图片拷贝到App私有目录，实现永久存储
            finalAvatar = copyUriToInternalStorage(pendingUri);
        }

        viewModel.saveProfile(
                tvNickname.getText().toString(),
                tvSignature.getText().toString(),
                finalAvatar,
                tvGender.getText().toString(),
                tvBirthday.getText().toString()
        );
        ToastHelper.showShort(getContext(), "资料已保存");
        requireActivity().onBackPressed();
    }

    private String copyUriToInternalStorage(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            File file = new File(requireContext().getFilesDir(), "user_avatar.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.close();
            is.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return uri.toString();
        }
    }
}
