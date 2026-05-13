package com.example.musicplayer;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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

import com.example.musicplayer.R;

import java.util.ArrayList;
import java.util.List;

import coil.Coil;
import coil.ImageLoader;
import coil.request.ImageRequest;
import coil.request.SuccessResult;

public class DiscoveryFragment extends Fragment {

    private CardView heroCard;
    private ImageView heroImage;
    private TextView heroLyric;
    private ImageView imgSong1, imgSong2, imgSong3;
    private ImageView btnHeroPlay;
    private View searchBar;
    private View layoutNewSongs;
    private TextView tvSongName1, tvArtistName1, tvSongName2, tvArtistName2, tvSongName3, tvArtistName3;
    private RecyclerView recommendedSongList;
    private MainViewModel viewModel;
    private SearchResultAdapter recommendedAdapter;
    
    private static final int SEARCH_REQUEST_CODE = 1001;
    
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> searchLauncher;
    
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
        heroImage = view.findViewById(R.id.heroImage);
        heroLyric = view.findViewById(R.id.heroLyric);

        imgSong1 = view.findViewById(R.id.imgSong1);
        imgSong2 = view.findViewById(R.id.imgSong2);
        imgSong3 = view.findViewById(R.id.imgSong3);

        btnHeroPlay = view.findViewById(R.id.btnHeroPlay);
        searchBar = view.findViewById(R.id.searchBar);
        recommendedSongList = view.findViewById(R.id.recommendedSongList);
        layoutNewSongs = view.findViewById(R.id.layoutNewSongs);
        tvSongName1 = view.findViewById(R.id.tvSongName1);
        tvArtistName1 = view.findViewById(R.id.tvArtistName1);
        tvSongName2 = view.findViewById(R.id.tvSongName2);
        tvArtistName2 = view.findViewById(R.id.tvArtistName2);
        tvSongName3 = view.findViewById(R.id.tvSongName3);
        tvArtistName3 = view.findViewById(R.id.tvArtistName3);

        setupSearch();
        setupRecommendedList();
        setupClickListeners();
        
        // Observe remote songs for the main discovery content
        viewModel.getRemoteSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null && !songs.isEmpty()) {
                if (isAdded()) { // 检查 Fragment 是否已添加
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
            updateHeroCard(title, viewModel.getCoverUrl().getValue());
        });

        viewModel.getCoverUrl().observe(getViewLifecycleOwner(), url -> {
            updateHeroCard(viewModel.getSongTitle().getValue(), url);
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
        
        ImageLoader imageLoader = Coil.imageLoader(requireContext());
        if (recommendedAdapter != null) {
            recommendedAdapter.setSongs(songs, "");
        }

        ImageView[] songViews = {imgSong1, imgSong2, imgSong3};
        TextView[] nameViews = {tvSongName1, tvSongName2, tvSongName3};
        TextView[] artistViews = {tvArtistName1, tvArtistName2, tvArtistName3};

        for (int i = 0; i < songViews.length; i++) {
            if (songViews[i] != null) {
                final Song song = (i < songs.size()) ? songs.get(i) : null;
                String url = (song != null) ? song.coverUrl : null;
                
                android.util.Log.d("DiscoveryFragment", "加载图片 URL [" + i + "]: " + url);
                
                imageLoader.enqueue(new ImageRequest.Builder(requireContext())
                        .data(url != null && !url.isEmpty() ? url : R.drawable.music)
                        .target(songViews[i])
                        .error(R.drawable.music)
                        .placeholder(R.drawable.music)
                        .crossfade(true)
                        .build());
                
                if (song != null) {
                    if (nameViews[i] != null) nameViews[i].setText(song.title);
                    if (artistViews[i] != null) artistViews[i].setText(song.artist);
                    
                    // 获取父容器并设置点击监听
                    try {
                        View parentContainer = (View) songViews[i].getParent();
                        if (parentContainer != null) {
                            parentContainer.setOnClickListener(v -> {
                                int index = songs.indexOf(song);
                                if (index >= 0) {
                                    ((MainActivity) requireActivity()).playSongList(songs, index);
                                }
                            });
                        }
                    } catch (Exception e) {
                        android.util.Log.e("DiscoveryFragment", "设置点击监听失败", e);
                    }
                }
            }
        }
    }

    private void updateHeroCard(String title, String coverUrl) {
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
                            // 加载成功后从列表中移除
                            pendingImageRequests.remove(request);
                        }
                    })
                    .build();
            // 保存请求以便在Fragment销毁时取消
            pendingImageRequests.add(request);
            imageLoader.enqueue(request);
        }
        
        if (heroLyric != null) {
            // 显示正在播放状态
            if (title != null && !title.isEmpty() && !title.equals("未知歌曲")) {
                heroLyric.setText("正在播放：" + title);
                heroLyric.setVisibility(View.VISIBLE);
            } else {
                // 没有歌曲播放时显示"正在播放："
                heroLyric.setText("正在播放：");
                heroLyric.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupClickListeners() {
        View.OnClickListener playListener = v -> {
            Boolean currentlyPlaying = viewModel.getIsPlaying().getValue();
            if (currentlyPlaying != null) {
                viewModel.setIsPlaying(!currentlyPlaying);
            }
        };
        if (btnHeroPlay != null) btnHeroPlay.setOnClickListener(playListener);
    }

    private void extractColor(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int dominantColor = palette.getDarkMutedColor(Color.parseColor("#1E2228"));
                if (heroCard != null) {
                    heroCard.setCardBackgroundColor(dominantColor);
                }
            }
        });
    }
    
    @Override
    public void onDestroyView() {
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
        super.onDestroyView();
    }
}
