package com.example.musicplayer.feature.discovery;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.musicplayer.R;
import com.example.musicplayer.data.model.Song;
import com.example.musicplayer.feature.main.MainActivity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SearchResultAdapter extends ListAdapter<Song, SearchResultAdapter.ViewHolder> {
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

    private String searchQuery = "";
    private int currentPlayingId = -1;
    private final OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public SearchResultAdapter(OnSongClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setSongs(List<Song> songs, String query) {
        String newQuery = query != null ? query : "";
        boolean queryChanged = !Objects.equals(this.searchQuery, newQuery);
        this.searchQuery = newQuery;
        List<Song> newList = songs == null ? new ArrayList<>() : new ArrayList<>(songs);
        submitList(newList, () -> {
            if (queryChanged && getItemCount() > 0) {
                notifyItemRangeChanged(0, getItemCount());
            }
        });
    }

    public void setCurrentPlayingId(int songId) {
        if (currentPlayingId == songId) {
            return;
        }
        int previousPosition = findPositionBySongId(currentPlayingId);
        this.currentPlayingId = songId;
        int currentPosition = findPositionBySongId(songId);
        if (previousPosition >= 0) {
            notifyItemChanged(previousPosition);
        }
        if (currentPosition >= 0) {
            notifyItemChanged(currentPosition);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_detailed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = getItem(position);
        
        boolean isCurrent = song.id == currentPlayingId;
        int activeColor = 0xFF1DB954; // Spotify Green
        int inactiveColor = holder.itemView.getContext().getColor(R.color.text_secondary);

        if (isCurrent) {
            holder.index.setText(""); // Hide number
            holder.index.setBackgroundResource(android.R.drawable.ic_media_play);
            if (holder.index.getBackground() != null) {
                holder.index.getBackground().setTint(activeColor);
            }
            holder.index.setPadding(12, 12, 12, 12);
        } else {
            holder.index.setText(String.valueOf(position + 1));
            holder.index.setBackground(null);
            holder.index.setPadding(0, 0, 0, 0);
            holder.index.setTextColor(inactiveColor);
        }

        holder.title.setText(getHighlightedText(song.title, searchQuery));
        holder.title.setTextColor(isCurrent ? activeColor : holder.itemView.getContext().getColor(R.color.text_primary));

        // 显示歌手而不是作曲家，并高亮显示匹配的文字
        holder.artist.setText(getHighlightedText(song.singer != null ? song.singer : song.artist, searchQuery));
        
        // 高亮显示专辑名称
        holder.album.setText(getHighlightedText(song.album != null ? song.album : "Unknown Album", searchQuery));
        holder.duration.setText(formatDuration(song.duration));
        
        // Plus button visibility - Spotify often shows it for saved/addable songs
        holder.btnAdd.setVisibility(View.VISIBLE);
        holder.btnAdd.setColorFilter(isCurrent ? activeColor : inactiveColor);
        
        if (song.coverUrl != null && !song.coverUrl.isEmpty()) {
            coil.Coil.imageLoader(holder.itemView.getContext()).enqueue(
                new coil.request.ImageRequest.Builder(holder.itemView.getContext())
                    .data(song.coverUrl)
                    .target(holder.cover)
                    .crossfade(true)
                    .placeholder(R.drawable.music)
                    .error(R.drawable.music)
                    .build()
            );
        } else {
            holder.cover.setImageResource(R.drawable.music);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(song);
            }
        });

        holder.btnAdd.setOnClickListener(v -> {
            // Optional: Implement add to playlist or favorite
            if (holder.itemView.getContext() instanceof MainActivity) {
                ((MainActivity) holder.itemView.getContext()).downloadSong(song);
            }
        });

        holder.btnMore.setOnClickListener(v -> {
            // Show more options menu
        });
    }

    private String formatDuration(int seconds) {
        if (seconds <= 0) return "--:--";
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", m, s);
    }

    private CharSequence getHighlightedText(String text, String query) {
        String safeText = text != null ? text : "";
        if (query.isEmpty() || !safeText.toLowerCase().contains(query.toLowerCase())) {
            return safeText;
        }
        SpannableString spannable = new SpannableString(safeText);
        String lowerText = safeText.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int start = lowerText.indexOf(lowerQuery);
        while (start >= 0) {
            int end = start + query.length();
            spannable.setSpan(new ForegroundColorSpan(0xFF26D1D1), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = lowerText.indexOf(lowerQuery, end);
        }
        return spannable;
    }

    private int findPositionBySongId(int songId) {
        if (songId == -1) {
            return -1;
        }
        List<Song> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).id == songId) {
                return i;
            }
        }
        return -1;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, artist, album, duration, index;
        ImageView btnMore, btnAdd;

        ViewHolder(View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.songCover);
            title = itemView.findViewById(R.id.songTitle);
            artist = itemView.findViewById(R.id.songArtist);
            album = itemView.findViewById(R.id.songAlbum);
            duration = itemView.findViewById(R.id.songDuration);
            index = itemView.findViewById(R.id.songIndex);
            btnMore = itemView.findViewById(R.id.btnMore);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}
