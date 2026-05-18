package com.example.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.request.ImageRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SongAdapter extends ListAdapter<Song, SongAdapter.ViewHolder> {
    private static final DiffUtil.ItemCallback<Song> DIFF_CALLBACK = new DiffUtil.ItemCallback<Song>() {
        @Override
        public boolean areItemsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Song oldItem, @NonNull Song newItem) {
            return oldItem.duration == newItem.duration
                    && oldItem.isLocal == newItem.isLocal
                    && oldItem.isFavorite == newItem.isFavorite
                    && Objects.equals(oldItem.title, newItem.title)
                    && Objects.equals(oldItem.artist, newItem.artist)
                    && Objects.equals(oldItem.singer, newItem.singer)
                    && Objects.equals(oldItem.path, newItem.path)
                    && Objects.equals(oldItem.coverUrl, newItem.coverUrl)
                    && Objects.equals(oldItem.album, newItem.album)
                    && Objects.equals(oldItem.lrcId, newItem.lrcId)
                    && Objects.equals(oldItem.lyrics, newItem.lyrics);
        }
    };

    private final OnSongClickListener listener;
    private boolean isEditMode = false;
    private final java.util.Set<Integer> selectedSongs = new java.util.HashSet<>();

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
        super(DIFF_CALLBACK);
        this.listener = listener;
        updateData(songs);
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
        for (Song song : getCurrentList()) {
            if (selectedSongs.contains(song.id)) {
                selectedList.add(song);
            }
        }
        return selectedList;
    }

    /**
     * 获取当前数据列表
     */
    public List<Song> getData() {
        return new ArrayList<>(getCurrentList());
    }

    /**
     * 更新数据列表（用于LiveData观察时更新，避免重建Adapter）
     */
    public void updateData(List<Song> newSongs) {
        List<Song> updatedSongs = newSongs == null ? new ArrayList<>() : new ArrayList<>(newSongs);
        submitList(updatedSongs, this::syncSelectedSongs);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = getItem(position);
        holder.title.setText(song.title);
        // 显示歌手而不是作曲家
        holder.artist.setText(song.singer != null ? song.singer : song.artist);
        
        // 统一处理封面加载逻辑
        boolean hasRealCover = song.coverUrl != null && 
                              !song.coverUrl.trim().isEmpty() && 
                              !song.coverUrl.startsWith("android.resource");

        if (hasRealCover) {
            holder.cover.setPadding(0, 0, 0, 0);
            holder.cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            holder.cover.setColorFilter(null);
            holder.cover.setBackground(null);
            
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(song.coverUrl)
                    .target(holder.cover)
                    .crossfade(true)
                    .placeholder(R.drawable.music)
                    .error(R.drawable.music)
                    .build();
            Coil.imageLoader(holder.itemView.getContext()).enqueue(request);
        } else {
            holder.cover.setImageResource(R.drawable.music);
            holder.cover.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            int p = (int) (10 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            holder.cover.setPadding(p, p, p, p);
            holder.cover.setColorFilter(null);
            holder.cover.setBackground(null);
        }

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
        return getCurrentList().size();
    }

    private void syncSelectedSongs() {
        java.util.Set<Integer> validSongIds = new java.util.HashSet<>();
        for (Song song : getCurrentList()) {
            validSongIds.add(song.id);
        }
        selectedSongs.retainAll(validSongIds);
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
