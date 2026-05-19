package com.example.musicplayer.feature.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import coil.Coil;
import coil.request.ImageRequest;
import com.airbnb.lottie.LottieAnimationView;
import com.example.musicplayer.R;
import com.example.musicplayer.core.Constants;
import com.example.musicplayer.data.model.User;
import com.example.musicplayer.feature.auth.LoginActivity;
import com.example.musicplayer.feature.auth.UserManager;
import com.example.musicplayer.feature.library.FavoriteMusicFragment;
import com.example.musicplayer.feature.library.ImportedMusicFragment;
import com.example.musicplayer.feature.library.LocalMusicFragment;
import com.example.musicplayer.feature.library.RecentPlayFragment;
import com.example.musicplayer.feature.main.MainViewModel;

public class MineFragment extends Fragment {

    private MainViewModel viewModel;
    private ImageView profileImage;
    private TextView nicknameText;
    private TextView signatureText;
    private TextView tvTotalSongs;
    private TextView vipTag;
    private TextView tvUserId;
    private TextView tvGender;
    private TextView tvBirthday;
    private Button btnLogin;
    private View userInfoContainer;
    private static final String PROFILE_IMAGE_URL = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&h=200&fit=crop";

    private LottieAnimationView assistantView;
    private View assistantBubble;
    private TextView assistantText;
    private final String[] assistantQuotes = {
            "愿音乐治愈你 ✨",
            "今天也要听歌哦 ~",
            "发现心动旋律了吗？",
            "音乐是灵魂的避风港 🎵",
            "戴上耳机，世界就是你的 🎧",
            "这首歌真的很好听，不信你听听",
            "摸鱼中... 被你发现啦 🙊",
            "每一个音符都在跳舞 💃"
    };
    private final java.util.Random random = new java.util.Random();
    private Runnable hideBubbleRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mine, container, false);
        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(MainViewModel.class);
        profileImage = view.findViewById(R.id.profileImage);
        nicknameText = view.findViewById(R.id.nickname);
        signatureText = view.findViewById(R.id.signature);
        tvTotalSongs = view.findViewById(R.id.tvTotalSongs);
        vipTag = view.findViewById(R.id.vipTag);
        tvUserId = view.findViewById(R.id.tvUserId);
        tvGender = view.findViewById(R.id.tvGender);
        tvBirthday = view.findViewById(R.id.tvBirthday);
        btnLogin = view.findViewById(R.id.btnLogin);
        userInfoContainer = view.findViewById(R.id.userInfoContainer);
        assistantView = view.findViewById(R.id.assistantView);
        assistantBubble = view.findViewById(R.id.assistantBubble);
        assistantText = view.findViewById(R.id.assistantText);
        
        setupProfileInfo();
        setupPlaylist(view);
        setupGridListeners(view);
        setupAssistant();
        return view;
    }

    private void updateLoginState() {
        UserManager userManager = UserManager.getInstance(requireContext());
        boolean isLoggedIn = userManager.isLoggedIn();
        
        btnLogin.setVisibility(isLoggedIn ? View.GONE : View.VISIBLE);
        userInfoContainer.setVisibility(isLoggedIn ? View.VISIBLE : View.GONE);
        
        if (!isLoggedIn) {
            btnLogin.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                startActivity(intent);
            });
            // 清除旧的头像
            profileImage.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.refreshProfile(); // 确保从本地/内存同步最新数据
        updateLoginState();
        
        // 检查是否需要重新同步数据（解决重新登录后收藏不显示的问题）
        if (viewModel != null) {
            Boolean synced = viewModel.isDataSynced().getValue();
            if (synced == null || !synced) {
                // 数据未同步或标记为未完成，触发同步
                android.util.Log.d("MineFragment", "Triggering data sync on resume");
                viewModel.syncAllSongs();
            }
        }
    }
    
    private void refreshUserInfo() {
        // 该方法逻辑已整合到 updateLoginState 和 setupProfileInfo 的观察者中
        updateLoginState();
    }

    private void setupAssistant() {
        if (assistantView != null) {
            assistantView.setOnClickListener(v -> {
                showAssistantQuote();
            });
        }
    }

    private void showAssistantQuote() {
        if (assistantBubble == null || assistantText == null) return;

        // 取消之前的动画和延时任务，防止冲突
        assistantBubble.animate().cancel();
        if (hideBubbleRunnable != null) {
            assistantBubble.removeCallbacks(hideBubbleRunnable);
        }

        String quote = assistantQuotes[random.nextInt(assistantQuotes.length)];
        assistantText.setText(quote);

        assistantBubble.setVisibility(View.VISIBLE);
        assistantBubble.animate()
                .alpha(1f)
                .translationY(-20f) // 稍微加大位移，动感更强
                .setDuration(300)
                .start();

        hideBubbleRunnable = () -> {
            if (assistantBubble != null) {
                assistantBubble.animate()
                        .alpha(0f)
                        .translationY(0f)
                        .setDuration(500)
                        .withEndAction(() -> assistantBubble.setVisibility(View.GONE))
                        .start();
            }
        };
        assistantBubble.postDelayed(hideBubbleRunnable, 3000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理延时任务，防止内存泄漏
        if (assistantBubble != null && hideBubbleRunnable != null) {
            assistantBubble.removeCallbacks(hideBubbleRunnable);
        }
    }

    private void setupProfileInfo() {
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        if (vipTag != null) {
            vipTag.setText("VIP " + year);
        }

        viewModel.getUserNickname().observe(getViewLifecycleOwner(), name -> {
            if (nicknameText != null) nicknameText.setText(name);
        });
        viewModel.getUserSignature().observe(getViewLifecycleOwner(), sig -> {
            if (signatureText != null) signatureText.setText(sig);
        });
        viewModel.getUserId().observe(getViewLifecycleOwner(), id -> {
            if (tvUserId != null) tvUserId.setText(id);
        });
        viewModel.getUserGender().observe(getViewLifecycleOwner(), gender -> {
            if (tvGender != null) tvGender.setText(gender);
        });
        viewModel.getUserBirthday().observe(getViewLifecycleOwner(), birthday -> {
            if (tvBirthday != null) tvBirthday.setText(birthday);
        });
        viewModel.getUserAvatarUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null && !uri.isEmpty()) {
                loadUserAvatar();
            }
        });

        viewModel.getTotalPlayCount().observe(getViewLifecycleOwner(), count -> {
            if (tvTotalSongs != null) {
                String text = "累计听歌 " + (count != null ? count : 0) + " 首";
                tvTotalSongs.setText(text);
            }
        });
    }
    
    private void loadUserAvatar() {
        UserManager userManager = UserManager.getInstance(requireContext());
        User user = userManager.getCurrentUser();
        if (user != null && user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            String avatarUrl = user.getAvatar().startsWith("http") ? user.getAvatar() : Constants.API.BASE_URL + user.getAvatar();
            
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("avatar_prefs", android.content.Context.MODE_PRIVATE);
            long savedUpdateTime = prefs.getLong("avatar_update_time", 0);
            long lastLoadTime = prefs.getLong("last_load_time", 0);
            
            boolean shouldReload = savedUpdateTime > lastLoadTime;
            
            String finalAvatarUrl;
            if (shouldReload) {
                prefs.edit()
                        .putLong("last_load_time", savedUpdateTime)
                        .apply();
                
                finalAvatarUrl = avatarUrl + (avatarUrl.contains("?") ? "&t=" : "?t=") + System.currentTimeMillis();
            } else {
                finalAvatarUrl = avatarUrl;
            }
            
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(finalAvatarUrl)
                    .target(profileImage)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .crossfade(true)
                    .build();
            Coil.imageLoader(requireContext()).enqueue(request);
        }
    }

    private void setupPlaylist(View rootView) {
        TextView tvCollection = rootView.findViewById(R.id.tvCollectionCount);
        TextView tvRecent = rootView.findViewById(R.id.tvRecentCount);
        TextView tvDownload = rootView.findViewById(R.id.tvDownloadCount);
        TextView tvImport = rootView.findViewById(R.id.tvImportCount);

        viewModel.getFavoriteCount().observe(getViewLifecycleOwner(), count -> {
            if (tvCollection != null) tvCollection.setText(String.valueOf(count));
        });
        viewModel.getRecentCount().observe(getViewLifecycleOwner(), count -> {
            if (tvRecent != null) tvRecent.setText(String.valueOf(count));
        });
        viewModel.getDownloadedCount().observe(getViewLifecycleOwner(), count -> {
            if (tvDownload != null) tvDownload.setText(String.valueOf(count));
        });
        viewModel.getImportedCount().observe(getViewLifecycleOwner(), count -> {
            if (tvImport != null) tvImport.setText(String.valueOf(count));
        });
    }

    private void setupGridListeners(View view) {
        View cardCollection = view.findViewById(R.id.cardCollection);
        if (cardCollection != null) {
            cardCollection.setOnClickListener(v -> navigateTo(new FavoriteMusicFragment()));
        }

        View cardRecent = view.findViewById(R.id.cardRecent);
        if (cardRecent != null) {
            cardRecent.setOnClickListener(v -> navigateTo(new RecentPlayFragment()));
        }

        View cardDownload = view.findViewById(R.id.cardDownload);
        if (cardDownload != null) {
            cardDownload.setOnClickListener(v -> navigateTo(new LocalMusicFragment()));
        }

        View cardImport = view.findViewById(R.id.cardImport);
        if (cardImport != null) {
            cardImport.setOnClickListener(v -> navigateTo(new ImportedMusicFragment()));
        }

        view.findViewById(R.id.btnEdit).setOnClickListener(v -> navigateTo(new EditProfileFragment()));
        view.findViewById(R.id.cardSettings).setOnClickListener(v -> navigateTo(new SettingsFragment()));

        // 为设置右侧的更多符号添加点击事件，显示小助理文案
        View ivSettingsMore = view.findViewById(R.id.ivSettingsMore);
        if (ivSettingsMore != null) {
            ivSettingsMore.setOnClickListener(v -> showAssistantQuote());
        }
    }

    private void navigateTo(androidx.fragment.app.Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void loadProfileImage() {
    }
}
