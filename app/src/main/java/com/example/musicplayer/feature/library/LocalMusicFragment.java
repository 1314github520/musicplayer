package com.example.musicplayer.feature.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.ArrayList;

public class LocalMusicFragment extends Fragment {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private android.widget.TextView btnBatchDelete;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_local_music, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        recyclerView = view.findViewById(R.id.localMusicRv);
        btnBatchDelete = view.findViewById(R.id.btnBatchDelete);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (adapter != null && adapter.isEditMode()) {
                exitEditMode();
            } else {
                requireActivity().onBackPressed();
            }
        });

        btnBatchDelete.setOnClickListener(v -> {
            if (adapter == null) return;
            if (adapter.isEditMode()) {
                performBatchDelete();
            } else {
                enterEditMode();
            }
        });

        adapter = new SongAdapter(new ArrayList<>(), (song, position) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playSongList(adapter.getData(), position);
            }
        });
        adapter.setOnDeleteClickListener(song -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("删除确认")
                    .setMessage("确定要删除这首歌吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        // 如果是下载的文件，也删除物理文件
                        if (song.path != null && song.path.startsWith("/")) {
                            java.io.File file = new java.io.File(song.path);
                            if (file.exists()) {
                                boolean deleted = file.delete();
                                if (!deleted) {
                                    ToastHelper.showShort(getContext(),
                                            "文件删除失败");
                                    return;
                                }
                            }
                        }
                        viewModel.deleteSong(song);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        adapter.setOnDownloadClickListener(song -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).downloadSong(song);
            }
        });
        recyclerView.setAdapter(adapter);

        viewModel.getDownloadedSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                adapter.updateData(songs);
            }
        });

        return view;
    }

    private void enterEditMode() {
        if (adapter != null) {
            adapter.setEditMode(true);
            btnBatchDelete.setText("确认删除");
        }
    }

    private void exitEditMode() {
        if (adapter != null) {
            adapter.setEditMode(false);
            btnBatchDelete.setText("批量删除");
        }
    }

    private void performBatchDelete() {
        java.util.List<Song> selected = adapter.getSelectedSongs();
        if (selected.isEmpty()) {
            exitEditMode();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("批量删除确认")
                .setMessage("确定要删除选中的 " + selected.size() + " 首歌吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    for (Song song : selected) {
                        if (song.path != null && song.path.startsWith("/")) {
                            java.io.File file = new java.io.File(song.path);
                            if (file.exists()) file.delete();
                        }
                        viewModel.deleteSong(song);
                    }
                    exitEditMode();
                    ToastHelper.showShort(getContext(), "已删除 " + selected.size() + " 首歌曲");
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
