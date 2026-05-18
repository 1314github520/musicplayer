package com.example.musicplayer;

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

public class ImportedMusicFragment extends Fragment {

    private MainViewModel viewModel;
    private RecyclerView recyclerView;
    private SongAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_imported_music, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        
        recyclerView = view.findViewById(R.id.localMusicRv);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        view.findViewById(R.id.btnBack).setOnClickListener(v -> requireActivity().onBackPressed());

        view.findViewById(R.id.btnImport).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openFilePicker();
            }
        });

        adapter = new SongAdapter(new java.util.ArrayList<>(), (song, position) -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).playSongList(adapter.getData(), position);
            }
        });
        adapter.setOnDeleteClickListener(song -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("删除确认")
                    .setMessage("确定要删除这首歌吗？")
                    .setPositiveButton("删除", (dialog, which) -> viewModel.deleteSong(song))
                    .setNegativeButton("取消", null)
                    .show();
        });
        recyclerView.setAdapter(adapter);

        viewModel.getImportedSongs().observe(getViewLifecycleOwner(), songs -> {
            if (songs != null) {
                adapter.updateData(songs);
            }
        });

        return view;
    }
}
