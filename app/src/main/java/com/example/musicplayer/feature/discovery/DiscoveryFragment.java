package com.example.musicplayer.feature.discovery;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import coil.Coil;
import coil.ImageLoader;
import coil.request.ImageRequest;
import coil.request.SuccessResult;
import com.example.musicplayer.R;
import com.example.musicplayer.adapter.CarouselAdapter;
import com.example.musicplayer.core.theme.ThemeManager;
import com.example.musicplayer.data.local.AppDatabase;
import com.example.musicplayer.data.model.Song;
import com.example.musicplayer.feature.main.MainActivity;
import com.example.musicplayer.feature.main.MainViewModel;
import com.example.musicplayer.feature.search.SearchActivity;
import java.util.ArrayList;
import java.util.List;

public class DiscoveryFragment extends Fragment {
    private CardView heroCard;
    private View heroContainer;
    private ImageView heroImage;
    private TextView heroLyric;
    private TextView heroSubtitle;
    private TextView tabCategory;
    private ImageView btnHeroPlay;
    private View searchBar;
    private View layoutNewSongs;
    private RecyclerView recommendedSongList;
    private RecyclerView carouselRecyclerView;
    private View indicator1, indicator2, indicator3;
    private MainViewModel viewModel;
    private SearchResultAdapter recommendedAdapter;
    private CarouselAdapter carouselAdapter;
    
    private static final int SEARCH_REQUEST_CODE = 1001;
    private static final long CAROUSEL_INTERVAL = 3000; // 轮播间隔3秒
    
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> searchLauncher;
    private Handler carouselHandler;
    private Runnable carouselRunnable;
    private int currentCarouselPosition = 0;
    
    // 用于保存正在加载的图片请求，以便在Fragment销毁时取消
    private List<ImageRequest> pendingImageRequests = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_discovery, container, false);
        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(MainViewModel.class);

        // 初始化搜索启动器
        searchLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    int songId = result.getData().getIntExtra("song_id", -1);
                    if (songId != -1 && getActivity() instanceof MainActivity) {
                        // 从数据库获取歌曲并播放
                        viewModel.executorService.execute(() -> {
                            Song song = AppDatabase.getInstance(requireContext()).songDao().getSongById(songId);
                            if (song != null) {
                                getActivity().runOnUiThread(() -> {
                                    ((MainActivity) getActivity()).playSong(song);
                                });
                            }
                        });
                    }
                }
            }
        );

        heroCard = view.findViewById(R.id.heroCardContainer);
        heroContainer = view.findViewById(R.id.heroContainer);
        heroImage = view.findViewById(R.id.heroImage);
        heroLyric = view.findViewById(R.id.heroLyric);
        heroSubtitle = view.findViewById(R.id.heroSubtitle);
        tabCategory = view.findViewById(R.id.tabCategory);
        btnHeroPlay = view.findViewById(R.id.btnHeroPlay);
        searchBar = view.findViewById(R.id.searchBar);
        recommendedSongList = view.findViewById(R.id.recommendedSongList);
        carouselRecyclerView = view.findViewById(R.id.carouselRecyclerView);
        layoutNewSongs = view.findViewById(R.id.layoutNewSongs);
        
        // 轮播图指示器
        indicator1 = view.findViewById(R.id.indicator1);
        indicator2 = view.findViewById(R.id.indicator2);
        indicator3 = view.findViewById(R.id.indicator3);

        setupSearch();
        setupRecommendedList();
        setupCarousel();
        setupClickListeners();
        
        // Observe remote songs for the main discovery content
        viewModel.getRemoteSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null && !songs.isEmpty()) {
                if (isAdded()) {
                    updateDiscoveryContent(songs);
                }
            }
        });

        // Observe current playing song to highlight in the list and update Hero Card
        viewModel.getCurrentSongId().observe(getViewLifecycleOwner(), id -> {
            if (id != null) {
                if (recommendedAdapter != null) recommendedAdapter.setCurrentPlayingId(id);
            }
        });

        viewModel.getSongTitle().observe(getViewLifecycleOwner(), title -> {
            updateHeroCard(title, viewModel.getSongArtist().getValue(), viewModel.getCoverUrl().getValue());
        });

        viewModel.getSongArtist().observe(getViewLifecycleOwner(), artist -> {
            updateHeroCard(viewModel.getSongTitle().getValue(), artist, viewModel.getCoverUrl().getValue());
        });

        viewModel.getCoverUrl().observe(getViewLifecycleOwner(), url -> {
            updateHeroCard(viewModel.getSongTitle().getValue(), viewModel.getSongArtist().getValue(), url);
        });

        viewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if (btnHeroPlay != null) {
                btnHeroPlay.setImageResource(isPlaying ? 
                    android.R.drawable.ic_media_pause : 
                    android.R.drawable.ic_media_play);
            }
        });

        return view;
    }
    
    private void setupCarousel() {
        // 设置横向滚动的RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        carouselRecyclerView.setLayoutManager(layoutManager);
        
        // 使用ItemDecoration设置间距
        carouselRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, 
                                      @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                int spacing = 16;
                int position = parent.getChildAdapterPosition(view);
                
                if (position == 0) {
                    outRect.left = spacing;
                }
                outRect.right = spacing;
                outRect.top = 8;
                outRect.bottom = 8;
            }
        });
        
        // 初始化CarouselAdapter
        carouselAdapter = new CarouselAdapter(new ArrayList<>(), song -> {
            List<Song> currentSongs = viewModel.getRemoteSongs().getValue();
            if (currentSongs != null) {
                int index = currentSongs.indexOf(song);
                if (index >= 0) {
                    ((MainActivity) requireActivity()).playSongList(currentSongs, index);
                } else {
                    ((MainActivity) requireActivity()).playSong(song);
                }
            } else {
                ((MainActivity) requireActivity()).playSong(song);
            }
        });
        carouselRecyclerView.setAdapter(carouselAdapter);
        
        // 添加滚动监听来更新指示器
        carouselRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                updateCarouselIndicator();
            }
        });
    }
    
    private void startCarouselAutoScroll() {
        stopCarouselAutoScroll();
        
        carouselHandler = new Handler(Looper.getMainLooper());
        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                if (carouselAdapter != null && carouselAdapter.getItemCount() > 0) {
                    currentCarouselPosition = (currentCarouselPosition + 1) % carouselAdapter.getItemCount();
                    carouselRecyclerView.smoothScrollToPosition(currentCarouselPosition);
                }
                carouselHandler.postDelayed(this, CAROUSEL_INTERVAL);
            }
        };
        
        carouselHandler.postDelayed(carouselRunnable, CAROUSEL_INTERVAL);
    }
    
    private void stopCarouselAutoScroll() {
        if (carouselHandler != null && carouselRunnable != null) {
            carouselHandler.removeCallbacks(carouselRunnable);
        }
    }
    
    private void updateCarouselIndicator() {
        if (carouselRecyclerView == null || carouselAdapter == null) return;
        
        LinearLayoutManager layoutManager = (LinearLayoutManager) carouselRecyclerView.getLayoutManager();
        if (layoutManager == null) return;
        
        int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
        
        // 更新指示器状态
        indicator1.setBackgroundResource(firstVisiblePosition == 0 ? 
            R.drawable.carousel_indicator_active : R.drawable.carousel_indicator_inactive);
        indicator2.setBackgroundResource(firstVisiblePosition == 1 ? 
            R.drawable.carousel_indicator_active : R.drawable.carousel_indicator_inactive);
        indicator3.setBackgroundResource(firstVisiblePosition == 2 ? 
            R.drawable.carousel_indicator_active : R.drawable.carousel_indicator_inactive);
        
        currentCarouselPosition = firstVisiblePosition;
    }
    
    private void setupSearch() {
        // 点击搜索栏时跳转到SearchActivity
        searchBar.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(requireActivity(), SearchActivity.class);
            searchLauncher.launch(intent);
            requireActivity().overridePendingTransition(R.anim.fade_in, 0);
        });
    }

    private void setupRecommendedList() {
        recommendedAdapter = new SearchResultAdapter(song -> {
            List<Song> currentSongs = viewModel.getRemoteSongs().getValue();
            if (currentSongs != null) {
                int index = -1;
                for (int i = 0; i < currentSongs.size(); i++) {
                    if (currentSongs.get(i).id == song.id) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    ((MainActivity) requireActivity()).playSongList(currentSongs, index);
                } else {
                    ((MainActivity) requireActivity()).playSong(song);
                }
            } else {
                ((MainActivity) requireActivity()).playSong(song);
            }
        });
        recommendedSongList.setLayoutManager(new LinearLayoutManager(getContext()));
        recommendedSongList.setAdapter(recommendedAdapter);
    }

    private void updateDiscoveryContent(List<Song> songs) {
        if (songs == null || songs.isEmpty()) {
            android.util.Log.d("DiscoveryFragment", "收到的歌曲列表为空");
            return;
        }
        android.util.Log.d("DiscoveryFragment", "开始刷新 UI，歌曲数量: " + songs.size());
        if (!songs.isEmpty()) {
            android.util.Log.d("DiscoveryFragment", "第一首: id=" + songs.get(0).id + " title=" + songs.get(0).title);
            android.util.Log.d("DiscoveryFragment", "最后一首: id=" + songs.get(songs.size()-1).id + " title=" + songs.get(songs.size()-1).title);
        }
        
        ImageLoader imageLoader = Coil.imageLoader(requireContext());
        if (recommendedAdapter != null) {
            recommendedAdapter.setSongs(songs, "");
        }

        // 更新轮播图数据
        if (carouselAdapter != null) {
            carouselAdapter.updateData(songs);
            // 启动自动轮播
            startCarouselAutoScroll();
        }
    }

    private void updateHeroCard(String title, String artist, String coverUrl) {
        if (heroImage != null) {
            ImageLoader imageLoader = Coil.imageLoader(requireContext());
            ImageRequest request = new ImageRequest.Builder(requireContext())
                    .data(coverUrl != null && !coverUrl.isEmpty() ? coverUrl : R.drawable.music)
                    .target(heroImage)
                    .error(R.drawable.music)
                    .placeholder(R.drawable.music)
                    .allowHardware(false)
                    .listener(new ImageRequest.Listener() {
                        @Override
                        public void onSuccess(@NonNull ImageRequest request, @NonNull SuccessResult result) {
                            Bitmap bitmap = ((BitmapDrawable) result.getDrawable()).getBitmap();
                            extractColor(bitmap);
                            pendingImageRequests.remove(request);
                        }
                    })
                    .build();
            pendingImageRequests.add(request);
            imageLoader.enqueue(request);
        }
        
        if (heroLyric != null) {
            if (title != null && !title.isEmpty() && !title.equals("未知歌曲")) {
                heroLyric.setText(title);
                if (heroSubtitle != null) {
                    heroSubtitle.setText(artist != null ? artist : "未知歌手");
                    heroSubtitle.setVisibility(View.VISIBLE);
                }
            } else {
                heroLyric.setText("发现好音乐");
                if (heroSubtitle != null) {
                    heroSubtitle.setText("开启你的听歌之旅");
                    heroSubtitle.setVisibility(View.VISIBLE);
                }
            }
            heroLyric.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        View.OnClickListener playListener = v -> viewModel.requestTogglePlayback();
        if (btnHeroPlay != null) btnHeroPlay.setOnClickListener(playListener);
        if (tabCategory != null) {
            tabCategory.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showCategoryFragment();
                }
            });
        }
        
        if (heroContainer != null) {
            heroContainer.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showPlayerFragment();
                }
            });
        }
    }

    private void extractColor(Bitmap bitmap) {
        if (!isAdded() || getContext() == null) return;
        
        Palette.from(bitmap).generate(palette -> {
            if (palette != null && isAdded()) {
                int mode = ThemeManager.getInstance(requireContext()).getThemeMode();
                boolean isDarkMode = (mode == ThemeManager.THEME_DARK);
                
                int color;
                if (isDarkMode) {
                    color = palette.getDarkMutedColor(Color.parseColor("#1E2228"));
                } else {
                    int lightVibrant = palette.getLightVibrantColor(Color.TRANSPARENT);
                    int lightMuted = palette.getLightMutedColor(Color.TRANSPARENT);
                    
                    if (lightMuted != Color.TRANSPARENT) {
                        color = lightMuted;
                    } else if (lightVibrant != Color.TRANSPARENT) {
                        color = lightVibrant;
                    } else {
                        int dominant = palette.getDominantColor(Color.parseColor("#F5F7F9"));
                        float[] hsv = new float[3];
                        Color.colorToHSV(dominant, hsv);
                        hsv[1] = Math.min(hsv[1], 0.12f);
                        hsv[2] = 0.98f;
                        color = Color.HSVToColor(hsv);
                    }
                }
                
                if (heroCard != null) {
                    heroCard.setCardBackgroundColor(color);
                }
                
                if (heroLyric != null) {
                    double luminance = androidx.core.graphics.ColorUtils.calculateLuminance(color);
                    int textColor;
                    int subTextColor;
                    if (luminance < 0.4) {
                        textColor = Color.WHITE;
                        subTextColor = Color.parseColor("#CCFFFFFF");
                    } else {
                        textColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary);
                        subTextColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_grey);
                    }
                    heroLyric.setTextColor(textColor);
                    if (heroSubtitle != null) {
                        heroSubtitle.setTextColor(subTextColor);
                    }
                }
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        stopCarouselAutoScroll();
        
        if (getContext() != null) {
            ImageLoader imageLoader = Coil.imageLoader(getContext());
            for (ImageRequest request : pendingImageRequests) {
                imageLoader.enqueue(request).dispose();
            }
            pendingImageRequests.clear();
        }
        if (recommendedSongList != null) {
            recommendedSongList.setAdapter(null);
        }
        if (carouselRecyclerView != null) {
            carouselRecyclerView.setAdapter(null);
        }
        super.onDestroyView();
    }
}