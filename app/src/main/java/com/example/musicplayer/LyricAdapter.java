package com.example.musicplayer;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import java.util.ArrayList;
import java.util.List;

public class LyricAdapter extends RecyclerView.Adapter<LyricAdapter.LyricViewHolder> {

    private List<LyricEntry> lyrics = new ArrayList<>();
    private int currentLine = -1;

    public void setLyrics(List<LyricEntry> lyrics) {
        this.lyrics = lyrics;
        this.currentLine = 0; // 默认选中第一行，从头开始
        notifyDataSetChanged();
    }

    public void setCurrentLine(int line) {
        if (this.currentLine != line) {
            int oldLine = this.currentLine;
            this.currentLine = line;
            if (oldLine != -1) notifyItemChanged(oldLine);
            if (currentLine != -1) notifyItemChanged(currentLine);
        }
    }

    public interface OnLyricClickListener {
        void onLyricClick(long time);
    }

    private OnLyricClickListener clickListener;

    public void setOnLyricClickListener(OnLyricClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public LyricViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lyric, parent, false);
        return new LyricViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LyricViewHolder holder, int position) {
        LyricEntry entry = lyrics.get(position);
        holder.textView.setText(entry.text);
        
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onLyricClick(entry.time);
            }
        });
        
        if (position == currentLine) {
            // 当前播放行：使用主题资源色，自动适配浅色/深色模式
            Context context = holder.itemView.getContext();
            int currentColor = ContextCompat.getColor(context, R.color.lyric_current);
            holder.textView.setTextColor(currentColor);
            holder.textView.setTextSize(20);
            holder.textView.setAlpha(1.0f);
            holder.textView.setTypeface(null, Typeface.BOLD);
            holder.textView.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
        } else {
            // 其他行：使用主题资源色，自动适配浅色/深色模式
            Context context = holder.itemView.getContext();
            int normalColor = ContextCompat.getColor(context, R.color.lyric_normal);
            holder.textView.setTextColor(normalColor);
            holder.textView.setTextSize(16);
            holder.textView.setAlpha(0.6f);
            holder.textView.setTypeface(null, Typeface.NORMAL);
            holder.textView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        }
    }

    @Override
    public int getItemCount() {
        return lyrics.size();
    }

    static class LyricViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        LyricViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.lyricText);
        }
    }
}
