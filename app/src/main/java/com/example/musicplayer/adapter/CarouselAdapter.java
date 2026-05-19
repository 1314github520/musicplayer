package com.example.musicplayer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import coil.Coil;
import coil.ImageLoader;
import coil.request.ImageRequest;
import com.example.musicplayer.R;
import com.example.musicplayer.data.model.Song;

import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private List<Song> songs;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Song song);
    }

    public CarouselAdapter(List<Song> songs, OnItemClickListener listener) {
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_carousel, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvSongName.setText(song.title);
        holder.tvArtistName.setText(song.singer);

        ImageLoader imageLoader = Coil.imageLoader(holder.itemView.getContext());
        if (song.coverUrl != null && !song.coverUrl.isEmpty()) {
            ImageRequest request = new ImageRequest.Builder(holder.itemView.getContext())
                    .data(song.coverUrl)
                    .target(holder.imgSong)
                    .placeholder(R.drawable.music)
                    .error(R.drawable.music)
                    .build();
            imageLoader.enqueue(request);
        } else {
            holder.imgSong.setImageResource(R.drawable.music);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(song);
            }
        });
    }

    @Override
    public int getItemCount() {
        return songs != null ? songs.size() : 0;
    }

    public void updateData(List<Song> newSongs) {
        this.songs = newSongs;
        notifyDataSetChanged();
    }

    public static class CarouselViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSong;
        TextView tvSongName;
        TextView tvArtistName;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imgSong = itemView.findViewById(R.id.imgSong);
            tvSongName = itemView.findViewById(R.id.tvSongName);
            tvArtistName = itemView.findViewById(R.id.tvArtistName);
        }
    }
}