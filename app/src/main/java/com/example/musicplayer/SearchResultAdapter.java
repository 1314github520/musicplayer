package com.example.musicplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.example.musicplayer.R;

import java.util.ArrayList;
import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<Song> songs = new ArrayList<>();
    private String searchQuery = "";
    private int currentPlayingId = -1;
    private final OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClick(Song song);
    }

    public SearchResultAdapter(OnSongClickListener listener) {
        this.listener = listener;
    }

    public void setSongs(List<Song> songs, String query) {
        this.songs = songs;
        this.searchQuery = query != null ? query : "";
        notifyDataSetChanged();
    }

    public void setCurrentPlayingId(int songId) {
        this.currentPlayingId = songId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_song_detailed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        
        boolean isCurrent = song.id == currentPlayingId;
        int activeColor = 0xFF1DB954; // Spotify Green
        int inactiveColor = 0x66FFFFFF;

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
        holder.title.setTextColor(isCurrent ? activeColor : 0xFFFFFFFF);

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
                    .build()
            );
        } else {
            holder.cover.setImageResource(android.R.drawable.ic_menu_gallery);
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
        if (query.isEmpty() || !text.toLowerCase().contains(query.toLowerCase())) {
            return text;
        }
        SpannableString spannable = new SpannableString(text);
        int start = text.toLowerCase().indexOf(query.toLowerCase());
        while (start >= 0) {
            int end = start + query.length();
            spannable.setSpan(new ForegroundColorSpan(0xFF26D1D1), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = text.toLowerCase().indexOf(query.toLowerCase(), end);
        }
        return spannable;
    }

    @Override
    public int getItemCount() {
        return songs.size();
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
