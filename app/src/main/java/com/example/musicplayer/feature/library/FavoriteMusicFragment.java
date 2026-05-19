package com.example.musicplayer.feature.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.R;
import com.example.musicplayer.core.ToastHelper;
import com.example.musicplayer.feature.main.MainActivity;
import com.example.musicplayer.feature.main.MainViewModel;
import java.util.ArrayList;
import java.util.List;

public class FavoriteMusicFragment extends Fragment {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    
    // 标记是否已经设置了收藏列表观察者（避免重复设置）
    private boolean favoriteObserverSet = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local_music, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        ((TextView)view.findViewById(R.id.tvTitle)).setText("我的收藏");
        view.findViewById(R.id.btnBatchDelete).setVisibility(View.GONE);
        
        recyclerView = view.findViewById(R.id.localMusicRv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        tvEmpty = view.findViewById(android.R.id.empty); // 使用系统空视图
        if (tvEmpty == null) {
            tvEmpty = new TextView(getContext());
            tvEmpty.setText("暂无收藏");
            tvEmpty.setTextSize(16);
            tvEmpty.setGravity(android.view.Gravity.CENTER);
        }
        
        progressBar = new ProgressBar(getContext());
        progressBar.setVisibility(View.GONE);
        
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            requireActivity().onBackPressed();
        });

        // 初始化Adapter（只创建一次）
        adapter = new SongAdapter(new ArrayList<>(), (song, position) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playSongList(adapter.getData(), position);
            }
        });
        
        setupAdapterListeners();
        recyclerView.setAdapter(adapter);

        // 监听数据同步状态
        observeSyncStatus();
        
        return view;
    }

    /**
     * 设置Adapter的监听器
     */
    private void setupAdapterListeners() {
        adapter.setOnDeleteClickListener(song -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("取消收藏")
                    .setMessage("确定要取消收藏这首歌吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        song.isFavorite = false;
                        viewModel.updateSong(song);
                        viewModel.syncFavoriteToServer(song.id, false);
                        ToastHelper.showShort(getContext(), "已取消收藏");
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        adapter.setOnDownloadClickListener(song -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).downloadSong(song);
            }
        });
    }

    /**
     * 监听数据同步状态，确保在数据就绪后再显示收藏列表
     */
    private void observeSyncStatus() {
        viewModel.isDataSynced().observe(getViewLifecycleOwner(), synced -> {
            if (synced != null && synced) {
                // 数据已同步完成，开始观察收藏列表
                if (!favoriteObserverSet) {
                    observeFavoriteSongs();
                    favoriteObserverSet = true;
                }
                
                // 如果正在同步中，显示进度条
                if (viewModel.isSyncInProgress()) {
                    showLoadingState();
                } else {
                    hideLoadingState();
                }
            } else {
                // 数据尚未同步完成
                if (viewModel.isSyncInProgress()) {
                    showLoadingState();
                    
                    // 如果已经有本地缓存数据，也显示出来
                    if (!favoriteObserverSet) {
                        observeFavoriteSongs();
                        favoriteObserverSet = true;
                    }
                } else {
                    // 同步失败或未开始，直接加载数据
                    if (!favoriteObserverSet) {
                        observeFavoriteSongs();
                        favoriteObserverSet = true;
                    }
                    hideLoadingState();
                }
            }
        });
    }

    /**
     * 观察收藏歌曲列表LiveData
     */
    private void observeFavoriteSongs() {
        viewModel.getFavoriteSongs().observe(getViewLifecycleOwner(), songs -> {
            hideLoadingState();
            
            if (songs != null && !songs.isEmpty()) {
                // 更新Adapter数据（而不是新建Adapter）
                adapter.updateData(songs);
                recyclerView.setVisibility(View.VISIBLE);
                tvEmpty.setVisibility(View.GONE);
            } else {
                // 显示空状态
                recyclerView.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("暂无收藏\n快去添加喜欢的音乐吧~");
            }
        });
    }

    /**
     * 显示加载状态
     */
    private void showLoadingState() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (recyclerView != null && adapter.getItemCount() == 0) {
            recyclerView.setVisibility(View.GONE);
        }
        if (tvEmpty != null) {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    /**
     * 隐藏加载状态
     */
    private void hideLoadingState() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到这个页面时，检查是否需要重新触发数据同步
        if (viewModel != null && !viewModel.isSyncInProgress()) {
            // 如果数据已标记为未同步，触发一次同步
            Boolean synced = viewModel.isDataSynced().getValue();
            if (synced == null || !synced) {
                viewModel.syncAllSongs();
            }
        }
    }
}
