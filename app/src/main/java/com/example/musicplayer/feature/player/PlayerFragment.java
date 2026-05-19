package com.example.musicplayer.feature.player;

import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import coil.Coil;
import coil.request.ImageRequest;
import coil.request.SuccessResult;
import com.example.musicplayer.R;
import com.example.musicplayer.core.ToastHelper;
import com.example.musicplayer.core.lyrics.LyricUtils;
import com.example.musicplayer.data.local.AppDatabase;
import com.example.musicplayer.data.model.LyricEntry;
import com.example.musicplayer.data.model.Song;
import com.example.musicplayer.feature.main.MainViewModel;
import java.util.List;
import java.util.Locale;

public class PlayerFragment extends Fragment {

    private MainViewModel viewModel;
    private LyricAdapter lyricAdapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private ImageView backgroundIv;
    private SeekBar seekBar;
    private TextView titleTv, artistTv;
    private TextView tvCurrentTime, tvTotalTime;
    private ImageView btnPlayPause, btnPrev, btnNext, btnLoopMode, btnFavorite, btnDownload;
    private ImageView albumCoverIv;
    private View vinylContainer;
    private android.animation.ObjectAnimator rotateAnimator;
    
    private boolean isUserScrolling = false;
    private java.util.concurrent.ExecutorService executorService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_player, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        backgroundIv = view.findViewById(R.id.playerBackground);
        recyclerView = view.findViewById(R.id.lyricRecyclerView);
        seekBar = view.findViewById(R.id.playerSeekBar);
        titleTv = view.findViewById(R.id.playerSongTitle);
        artistTv = view.findViewById(R.id.playerArtistName);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);
        btnLoopMode = view.findViewById(R.id.btnLoopMode);
        btnFavorite = view.findViewById(R.id.btnFavorite);
        btnDownload = view.findViewById(R.id.btnDownload);
        albumCoverIv = view.findViewById(R.id.playerAlbumCover);
        vinylContainer = view.findViewById(R.id.vinylContainer);

        setupRotationAnimation();
        
        recyclerView.setVisibility(View.VISIBLE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            backgroundIv.setRenderEffect(RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.CLAMP));
        }
        backgroundIv.setAlpha(0.6f);

        setupRecyclerView();
        setupListeners(view);
        observeViewModel();
        setupButtonAnimations();

        return view;
    }

    private void setupRotationAnimation() {
        rotateAnimator = android.animation.ObjectAnimator.ofFloat(albumCoverIv, "rotation", 0f, 360f);
        rotateAnimator.setDuration(20000); // 20 seconds per rotation
        rotateAnimator.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        rotateAnimator.setInterpolator(new android.view.animation.LinearInterpolator());
    }

    private void updateRotation(boolean isPlaying) {
        if (isPlaying) {
            if (rotateAnimator.isPaused()) {
                rotateAnimator.resume();
            } else if (!rotateAnimator.isRunning()) {
                rotateAnimator.start();
            }
        } else {
            rotateAnimator.pause();
        }
    }

    private void setupButtonAnimations() {
        android.view.View.OnTouchListener scaleListener = (v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                    v.performClick();
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                    break;
            }
            return true;
        };
        btnPlayPause.setOnTouchListener(scaleListener);
        btnPrev.setOnTouchListener(scaleListener);
        btnNext.setOnTouchListener(scaleListener);
        btnLoopMode.setOnTouchListener(scaleListener);
        btnFavorite.setOnTouchListener(scaleListener);
        btnDownload.setOnTouchListener(scaleListener);
    }

    private void setupRecyclerView() {
        lyricAdapter = new LyricAdapter();
        lyricAdapter.setOnLyricClickListener(time -> {
            viewModel.setSeekToPosition(time);
        });
        layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(lyricAdapter);
        
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                isUserScrolling = (newState != RecyclerView.SCROLL_STATE_IDLE);
            }
        });
    }

    private void setupListeners(View view) {
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        btnPlayPause.setOnClickListener(v -> {
            viewModel.requestTogglePlayback();
        });

        btnPrev.setOnClickListener(v -> viewModel.playPrevious());
        btnNext.setOnClickListener(v -> viewModel.playNext());

        btnLoopMode.setOnClickListener(v -> {
            viewModel.toggleLoopMode();
        });

        btnFavorite.setOnClickListener(v -> {
            toggleFavorite();
        });

        btnDownload.setOnClickListener(v -> {
            downloadCurrentSong();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Long duration = viewModel.getDuration().getValue();
                    if (duration != null && duration > 0) {
                        long seekPos = (long) (progress * duration / 1000);
                        tvCurrentTime.setText(formatTime(seekPos));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserScrolling = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Long duration = viewModel.getDuration().getValue();
                if (duration != null && duration > 0) {
                    long seekPos = (long) (seekBar.getProgress() * duration / 1000);
                    viewModel.setSeekToPosition(seekPos);
                }
                isUserScrolling = false;
            }
        });
    }

    private String formatTime(long ms) {
        int totalSeconds = (int) (ms / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void toggleFavorite() {
        Integer songId = viewModel.getCurrentSongId().getValue();
        if (songId != null && songId != -1) {
            getExecutorService().execute(() -> {
                Song song = AppDatabase.getInstance(requireContext()).songDao().getSongById(songId);
                if (song != null) {
                    song.isFavorite = !song.isFavorite;
                    AppDatabase.getInstance(requireContext()).songDao().updateSong(song);
                    viewModel.syncFavoriteToServer(song.id, song.isFavorite);
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> {
                            updateFavoriteIcon(song.isFavorite);
                            ToastHelper.showShort(getContext(), 
                                song.isFavorite ? "已加入收藏" : "已取消收藏");
                        });
                    }
                }
            });
        }
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        btnFavorite.setImageResource(isFavorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
        // 使用主题资源色，自动适配浅色/深色模式
        int color = ContextCompat.getColor(requireContext(), 
            isFavorite ? R.color.player_btn_favorite_active : R.color.player_btn_favorite_inactive);
        btnFavorite.getDrawable().setTint(color);
    }

    private void downloadCurrentSong() {
        Integer songId = viewModel.getCurrentSongId().getValue();
        if (songId == null || songId == -1) {
            ToastHelper.showShort(getContext(), "无法获取歌曲信息");
            return;
        }
        
        getExecutorService().execute(() -> {
            Song song = AppDatabase.getInstance(requireContext()).songDao().getSongById(songId);
            if (song == null) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> 
                        ToastHelper.showShort(getContext(), "歌曲不存在"));
                }
                return;
            }
            
            if (song.isLocal) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> 
                        ToastHelper.showShort(getContext(), "这首歌已经是本地歌曲"));
                }
                return;
            }
            
            // 检查是否已经下载过
            java.io.File musicDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC);
            if (musicDir != null) {
                String fileName = sanitizeFileName(song.title) + ".mp3";
                java.io.File file = new java.io.File(musicDir, fileName);
                if (file.exists()) {
                    song.path = file.getAbsolutePath();
                    song.isLocal = true;
                    viewModel.updateSong(song);
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> {
                            ToastHelper.showShort(getContext(), "歌曲已下载: " + song.title);
                            updateDownloadIcon(true);
                        });
                    }
                    return;
                }
            }
            
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> 
                    ToastHelper.showShort(getContext(), "开始下载: " + song.title));
            }
            
            DownloadManager.getInstance(requireContext()).downloadSong(song, new DownloadManager.DownloadCallback() {
                @Override
                public void onProgress(int progress) {
                    // 可以在这里更新进度条，暂时不实现
                }
                
                @Override
                public void onSuccess(String localPath) {
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> {
                            song.path = localPath;
                            song.isLocal = true;
                            viewModel.updateSong(song);
                            ToastHelper.showShort(getContext(), "下载完成: " + song.title);
                            updateDownloadIcon(true);
                        });
                    }
                }
                
                @Override
                public void onError(String message) {
                    if (getActivity() != null && !getActivity().isFinishing()) {
                        getActivity().runOnUiThread(() -> 
                            ToastHelper.showShort(getContext(), message));
                    }
                }
            });
        });
    }
    
    private String sanitizeFileName(String title) {
        return title.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
    
    private void updateDownloadIcon(boolean isDownloaded) {
        btnDownload.setImageResource(isDownloaded ? 
            android.R.drawable.stat_sys_download_done : 
            android.R.drawable.stat_sys_download);
        // 使用主题资源色，自动适配浅色/深色模式
        int color = ContextCompat.getColor(requireContext(), 
            isDownloaded ? R.color.player_btn_download_active : R.color.player_btn_download_inactive);
        btnDownload.getDrawable().setTint(color);
    }

    private void observeViewModel() {
        viewModel.getLyrics().observe(getViewLifecycleOwner(), lyrics -> {
            lyricAdapter.setLyrics(lyrics);
            // 确保在找到歌词后滚动回顶部
            recyclerView.post(() -> recyclerView.scrollToPosition(0));
        });

        viewModel.getCurrentLyric().observe(getViewLifecycleOwner(), lyric -> {
            // Already updated via currentPosition observer for better precision
        });

        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            updateRotation(isPlaying);
        });

        viewModel.getCurrentPosition().observe(getViewLifecycleOwner(), pos -> {
            updateLyricPosition();
            if (!isUserScrolling) {
                Long duration = viewModel.getDuration().getValue();
                if (duration != null && duration > 0) {
                    int progress = (int) (pos * 1000 / duration);
                    seekBar.setProgress(progress);
                    tvCurrentTime.setText(formatTime(pos));
                }
            }
        });

        viewModel.getDuration().observe(getViewLifecycleOwner(), duration -> {
            tvTotalTime.setText(formatTime(duration));
        });

        viewModel.getSongTitle().observe(getViewLifecycleOwner(), title -> {
            titleTv.setText(title);
            // Update favorite icon for current song
            updateFavoriteStatus();
            // Update download icon for current song
            updateDownloadStatus();
        });

        viewModel.getSongArtist().observe(getViewLifecycleOwner(), artist -> {
            artistTv.setText(artist);
        });

        viewModel.getCoverUrl().observe(getViewLifecycleOwner(), this::loadCover);

        viewModel.getLoopMode().observe(getViewLifecycleOwner(), mode -> {
            // 0: List Loop, 1: Single Loop
            if (mode == 1) {
                btnLoopMode.setImageResource(R.drawable.cycle);
            } else {
                btnLoopMode.setImageResource(R.drawable.order);
            }
        });
    }

    private void updateFavoriteStatus() {
        Integer songId = viewModel.getCurrentSongId().getValue();
        if (songId != null && songId != -1) {
            getExecutorService().execute(() -> {
                Song song = AppDatabase.getInstance(requireContext()).songDao().getSongById(songId);
                if (song != null && getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> updateFavoriteIcon(song.isFavorite));
                }
            });
        }
    }
    
    private void updateDownloadStatus() {
        Integer songId = viewModel.getCurrentSongId().getValue();
        if (songId != null && songId != -1) {
            getExecutorService().execute(() -> {
                Song song = AppDatabase.getInstance(requireContext()).songDao().getSongById(songId);
                if (song != null && getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> updateDownloadIcon(song.isLocal));
                }
            });
        }
    }
    
    private java.util.concurrent.ExecutorService getExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = java.util.concurrent.Executors.newSingleThreadExecutor();
        }
        return executorService;
    }

    private int lastScrollToLine = -1;
    
    private void updateLyricPosition() {
        List<LyricEntry> lyrics = viewModel.getLyrics().getValue();
        Long currentPos = viewModel.getCurrentPosition().getValue();
        if (lyrics == null || currentPos == null || layoutManager == null) return;

        int targetLine = Math.max(0, LyricUtils.findCurrentLyricIndex(lyrics, currentPos));

        lyricAdapter.setCurrentLine(targetLine);
        
        if (!isUserScrolling && targetLine != lastScrollToLine) {
            int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
            int lastVisiblePosition = layoutManager.findLastCompletelyVisibleItemPosition();
            
            if (targetLine < firstVisiblePosition || targetLine > lastVisiblePosition) {
                int scrollPosition = Math.max(0, targetLine - 2);
                recyclerView.smoothScrollToPosition(scrollPosition);
                lastScrollToLine = targetLine;
            }
        }
    }

    private void loadCover(String url) {
        ImageRequest request = new ImageRequest.Builder(requireContext())
                .data(url)
                .allowHardware(false)
                .crossfade(true)
                .target(new coil.target.Target() {
                    @Override
                    public void onStart(@Nullable android.graphics.drawable.Drawable placeholder) {}
                    @Override
                    public void onError(@Nullable android.graphics.drawable.Drawable error) {}
                    @Override
                    public void onSuccess(@NonNull android.graphics.drawable.Drawable result) {
                        backgroundIv.setImageDrawable(result);
                        albumCoverIv.setImageDrawable(result);
                        
                        if (result instanceof BitmapDrawable) {
                            Bitmap bitmap = ((BitmapDrawable) result).getBitmap();
                            Palette.from(bitmap).generate(palette -> {
                                if (palette != null) {
                                    int dominantColor = palette.getDominantColor(0x4DFFFFFF);
                                    updateUIColors(dominantColor);
                                }
                            });
                        }
                    }
                })
                .build();
        Coil.imageLoader(requireContext()).enqueue(request);
    }

    private void updateUIColors(int color) {
        // Keep the interactive background but apply a subtle tint if needed
        int alphaColor = (color & 0x00FFFFFF) | 0x22000000;
        // The buttons now use btn_interactive_bg which has its own solid color.
        // We can dynamically tint the background if we want more "spirit".
        btnPlayPause.getBackground().setTint(alphaColor);
        btnPrev.getBackground().setTint(alphaColor);
        btnNext.getBackground().setTint(alphaColor);
    }
    
    @Override
    public void onDestroyView() {
        if (rotateAnimator != null) {
            rotateAnimator.cancel();
            rotateAnimator = null;
        }
        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(null);
        }
        if (recyclerView != null) {
            recyclerView.clearOnScrollListeners();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        super.onDestroyView();
    }
}
