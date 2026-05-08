package com.example.musicplayer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.ImageView;
import android.widget.TextView;

import coil.Coil;
import coil.request.ImageRequest;
import com.airbnb.lottie.LottieAnimationView;
import com.example.musicplayer.R;

public class MineFragment extends Fragment {

    private MainViewModel viewModel;
    private ImageView profileImage;
    private TextView nicknameText;
    private TextView signatureText;
    private TextView tvTotalSongs;
    private TextView vipTag;
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
        assistantView = view.findViewById(R.id.assistantView);
        assistantBubble = view.findViewById(R.id.assistantBubble);
        assistantText = view.findViewById(R.id.assistantText);
        
        setupProfileInfo();
        setupPlaylist(view);
        setupGridListeners(view);
        setupAssistant();
        loadProfileImage();
        return view;
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

        String quote = assistantQuotes[random.nextInt(assistantQuotes.length)];
        assistantText.setText(quote);

        if (hideBubbleRunnable != null) {
            assistantBubble.removeCallbacks(hideBubbleRunnable);
        }

        assistantBubble.animate()
                .alpha(1f)
                .translationY(-10f)
                .setDuration(300)
                .withStartAction(() -> assistantBubble.setVisibility(View.VISIBLE))
                .start();

        hideBubbleRunnable = () -> {
            assistantBubble.animate()
                    .alpha(0f)
                    .translationY(0f)
                    .setDuration(500)
                    .withEndAction(() -> assistantBubble.setVisibility(View.GONE))
                    .start();
        };
        assistantBubble.postDelayed(hideBubbleRunnable, 3000);
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
        viewModel.getUserAvatarUri().observe(getViewLifecycleOwner(), uri -> {
            if (profileImage != null) {
                ImageRequest request = new ImageRequest.Builder(requireContext())
                        .data(uri)
                        .target(profileImage)
                        .build();
                Coil.imageLoader(requireContext()).enqueue(request);
            }
        });

        viewModel.getTotalPlayCount().observe(getViewLifecycleOwner(), count -> {
            if (tvTotalSongs != null) {
                String text = "累计听歌 " + (count != null ? count : 0) + " 首";
                tvTotalSongs.setText(text);
            }
        });
    }

    private void setupPlaylist(View rootView) {
        viewModel.getFavoriteCount().observe(getViewLifecycleOwner(), count -> {
            View grid = rootView.findViewById(R.id.functionGrid);
            if (grid instanceof android.view.ViewGroup) {
                updateStatCard((android.view.ViewGroup) grid, 0, count);
            }
        });
        viewModel.getDownloadedCount().observe(getViewLifecycleOwner(), count -> {
            View grid = rootView.findViewById(R.id.functionGrid);
            if (grid instanceof android.view.ViewGroup) {
                updateStatCard((android.view.ViewGroup) grid, 2, count);
            }
        });
        viewModel.getImportedCount().observe(getViewLifecycleOwner(), count -> {
            View grid = rootView.findViewById(R.id.functionGrid);
            if (grid instanceof android.view.ViewGroup) {
                updateStatCard((android.view.ViewGroup) grid, 3, count);
            }
        });

        viewModel.getRecentCount().observe(getViewLifecycleOwner(), count -> {
            View grid = rootView.findViewById(R.id.functionGrid);
            if (grid instanceof android.view.ViewGroup) {
                updateStatCard((android.view.ViewGroup) grid, 1, count);
            }
        });
    }

    private void updateStatCard(android.view.ViewGroup grid, int index, int value) {
        if (grid != null && index < grid.getChildCount()) {
            View card = grid.getChildAt(index);
            if (card instanceof androidx.cardview.widget.CardView) {
                View inner = ((androidx.cardview.widget.CardView) card).getChildAt(0);
                if (inner instanceof android.widget.LinearLayout) {
                    android.widget.LinearLayout layout = (android.widget.LinearLayout) inner;
                    if (layout.getChildCount() > 3) {
                        View text = layout.getChildAt(3);
                        if (text instanceof android.widget.TextView) {
                            ((android.widget.TextView) text).setText(String.valueOf(value));
                        }
                    }
                }
            }
        }
    }

    private void setupGridListeners(View view) {
        android.view.ViewGroup grid = view.findViewById(R.id.functionGrid);
        if (grid != null) {
            for (int i = 0; i < grid.getChildCount(); i++) {
                View child = grid.getChildAt(i);
                final int index = i;
                child.setOnClickListener(v -> {
                    if (index == 0) {
                        getParentFragmentManager().beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.fragment_container, new FavoriteMusicFragment())
                                .addToBackStack(null)
                                .commit();
                    } else if (index == 1) {
                        getParentFragmentManager().beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.fragment_container, new RecentPlayFragment())
                                .addToBackStack(null)
                                .commit();
                    } else if (index == 2) {
                        getParentFragmentManager().beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.fragment_container, new LocalMusicFragment())
                                .addToBackStack(null)
                                .commit();
                    } else if (index == 3) {
                        getParentFragmentManager().beginTransaction()
                                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                                .replace(R.id.fragment_container, new ImportedMusicFragment())
                                .addToBackStack(null)
                                .commit();
                    }
                });
            }
        }

        view.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, new EditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.cardSettings).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out)
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadProfileImage() {
    }
}
