package com.example.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import coil.Coil;
import coil.request.ImageRequest;
import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    private List<Song> songs;
    private OnSongClickListener listener;
    private boolean isEditMode = false;
    private java.util.Set<Integer> selectedSongs = new java.util.HashSet<>();

    public interface OnSongClickListener {
        void onSongClick(Song song, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Song song);
    }

    public interface OnDownloadClickListener {
        void onDownloadClick(Song song);
    }

    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    private OnDeleteClickListener deleteListener;
    private OnDownloadClickListener downloadListener;

    public void setOnDeleteClickListener(OnDeleteClickListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setOnDownloadClickListener(OnDownloadClickListener downloadListener) {
        this.downloadListener = downloadListener;
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        if (!editMode) selectedSongs.clear();
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public List<Song> getSelectedSongs() {
        List<Song> selectedList = new java.util.ArrayList<>();
        for (Song song : songs) {
            if (selectedSongs.contains(song.id)) {
                selectedList.add(song);
            }
        }
        return selectedList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.title.setText(song.title);
        // 显示歌手而不是作曲家
        holder.artist.setText(song.singer != null ? song.singer : song.artist);
        
        // 本地音乐显示music.png封面
        String coverUrl = song.coverUrl;
        if (song.isLocal && (coverUrl == null || coverUrl.trim().isEmpty())) {
            coverUrl = "android.resource://" + holder.itemView.getContext().getPackageName() + "/" + R.drawable.music;
        }
        
        ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                .data(coverUrl)
                .target(holder.cover)
                .crossfade(true)
                .build();
        Coil.imageLoader(holder.itemView.getContext()).enqueue(request);

        // Hide favorite button as feature is removed
        holder.btnFavorite.setVisibility(View.GONE);

        // Show/Hide delete/download button based on local status
        if (song.isLocal) {
            holder.btnDownload.setImageResource(android.R.drawable.ic_menu_delete);
        } else {
            holder.btnDownload.setImageResource(android.R.drawable.stat_sys_download);
        }

        // Edit Mode UI
        if (isEditMode) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(selectedSongs.contains(song.id));
            holder.btnDownload.setVisibility(View.GONE);
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.btnDownload.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (isEditMode) {
                if (selectedSongs.contains(song.id)) {
                    selectedSongs.remove(song.id);
                } else {
                    selectedSongs.add(song.id);
                }
                notifyItemChanged(position);
            } else {
                listener.onSongClick(song, position);
            }
        });

        holder.checkBox.setOnClickListener(v -> {
            if (selectedSongs.contains(song.id)) {
                selectedSongs.remove(song.id);
            } else {
                selectedSongs.add(song.id);
            }
        });

        holder.btnDownload.setOnClickListener(v -> {
            if (song.isLocal) {
                if (deleteListener != null) {
                    deleteListener.onDeleteClick(song);
                }
            } else {
                if (downloadListener != null) {
                    downloadListener.onDownloadClick(song);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title;
        TextView artist;
        ImageView btnFavorite;
        ImageView btnDownload;
        android.widget.CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.songCover);
            title = itemView.findViewById(R.id.songTitle);
            artist = itemView.findViewById(R.id.songArtist);
            btnFavorite = itemView.findViewById(R.id.btnItemFavorite);
            btnDownload = itemView.findViewById(R.id.btnItemDownload);
            checkBox = itemView.findViewById(R.id.songCheckBox);
        }
    }
}
