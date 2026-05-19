package com.example.musicplayer.feature.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.R;
import com.example.musicplayer.core.ToastHelper;
import com.example.musicplayer.data.model.Song;
import com.example.musicplayer.feature.main.MainActivity;
import com.example.musicplayer.feature.main.MainViewModel;
import java.util.List;

public class RecentPlayFragment extends Fragment {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private TextView tvSongCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent_play, container, false);
        
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        recyclerView = view.findViewById(R.id.recentRv);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new SongAdapter(new java.util.ArrayList<>(), (song, position) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playSongList(adapter.getData(), position);
            }
        });
        
        // 设置删除按钮点击监听
        adapter.setOnDeleteClickListener(song -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("删除确认")
                    .setMessage("确定要从最近播放中删除 \"" + song.title + "\" 吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        viewModel.deleteRecentPlay(song.id);
                        ToastHelper.showShort(getContext(), "已删除: " + song.title);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        
        // 设置下载按钮点击监听
        adapter.setOnDownloadClickListener(song -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).downloadSong(song);
            }
        });
        recyclerView.setAdapter(adapter);
        
        viewModel.getRecentSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                tvSongCount.setText(songs.size() + "首歌曲");
                adapter.updateData(songs);
            }
        });

        view.findViewById(R.id.btnPlayAll).setOnClickListener(v -> {
            List<Song> songs = viewModel.getRecentSongs().getValue();
            if (songs != null && !songs.isEmpty() && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playSongList(songs, 0);
            }
        });

        return view;
    }
}
