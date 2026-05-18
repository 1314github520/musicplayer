package com.example.musicplayer;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryFragment extends Fragment {
    private static final String CATEGORY_MODE_ARTIST = "artist";
    private static final String CATEGORY_MODE_ALBUM = "album";

    private ImageView btnBack;
    private TextView txtCategoryDescription;
    private TextView tvModeValue;
    private TextView tvGroupCount;
    private TextView tvSongCount;
    private TextView txtListCount;
    private ChipGroup categoryModeGroup;
    private ChipGroup categoryFilterGroup;
    private RecyclerView categorySongList;

    private MainViewModel viewModel;
    private SearchResultAdapter categoryAdapter;
    private final Map<String, List<Song>> currentCategoryGroups = new LinkedHashMap<>();
    private String selectedCategoryMode = CATEGORY_MODE_ARTIST;
    private String selectedCategoryValue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category, container, false);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        btnBack = view.findViewById(R.id.btnBack);
        txtCategoryDescription = view.findViewById(R.id.txtCategoryDescription);
        tvModeValue = view.findViewById(R.id.tvModeValue);
        tvGroupCount = view.findViewById(R.id.tvGroupCount);
        tvSongCount = view.findViewById(R.id.tvSongCount);
        txtListCount = view.findViewById(R.id.txtListCount);
        categoryModeGroup = view.findViewById(R.id.categoryModeGroup);
        categoryFilterGroup = view.findViewById(R.id.categoryFilterGroup);
        categorySongList = view.findViewById(R.id.categorySongList);

        setupCategorySection();
        setupClickListeners();

        viewModel.getRemoteSongs().observe(getViewLifecycleOwner(), this::updateCategoryContent);
        viewModel.getCurrentSongId().observe(getViewLifecycleOwner(), id -> {
            if (id != null && categoryAdapter != null) {
                categoryAdapter.setCurrentPlayingId(id);
            }
        });

        return view;
    }

    private void setupCategorySection() {
        categoryAdapter = new SearchResultAdapter(song -> {
            List<Song> filteredSongs = currentCategoryGroups.get(selectedCategoryValue);
            if (filteredSongs != null) {
                int index = filteredSongs.indexOf(song);
                if (index >= 0) {
                    ((MainActivity) requireActivity()).playSongList(filteredSongs, index);
                    return;
                }
            }
            ((MainActivity) requireActivity()).playSong(song);
        });
        categorySongList.setLayoutManager(new LinearLayoutManager(getContext()));
        categorySongList.setNestedScrollingEnabled(false);
        categorySongList.setAdapter(categoryAdapter);
        styleModeChip((Chip) categoryModeGroup.findViewById(R.id.chipArtistMode));
        styleModeChip((Chip) categoryModeGroup.findViewById(R.id.chipAlbumMode));

        categoryModeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                return;
            }
            String newMode = checkedIds.get(0) == R.id.chipAlbumMode ? CATEGORY_MODE_ALBUM : CATEGORY_MODE_ARTIST;
            if (!newMode.equals(selectedCategoryMode)) {
                selectedCategoryMode = newMode;
                selectedCategoryValue = null;
                updateCategoryContent(viewModel.getRemoteSongs().getValue());
            }
        });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void updateCategoryContent(List<Song> songs) {
        currentCategoryGroups.clear();
        if (songs == null || songs.isEmpty()) {
            categoryFilterGroup.removeAllViews();
            categoryAdapter.setSongs(new ArrayList<>(), "");
            txtCategoryDescription.setText(R.string.category_empty);
            tvGroupCount.setText("0");
            tvSongCount.setText("0");
            txtListCount.setText(getString(R.string.category_list_count_format, 0));
            return;
        }

        currentCategoryGroups.putAll(buildCategoryGroups(songs));
        tvModeValue.setText(CATEGORY_MODE_ALBUM.equals(selectedCategoryMode)
                ? R.string.category_by_album
                : R.string.category_by_artist);
        tvGroupCount.setText(String.valueOf(currentCategoryGroups.size()));
        tvSongCount.setText(String.valueOf(songs.size()));

        if (currentCategoryGroups.isEmpty()) {
            categoryFilterGroup.removeAllViews();
            categoryAdapter.setSongs(new ArrayList<>(), "");
            txtCategoryDescription.setText(R.string.category_empty);
            txtListCount.setText(getString(R.string.category_list_count_format, 0));
            return;
        }

        if (selectedCategoryValue == null || !currentCategoryGroups.containsKey(selectedCategoryValue)) {
            selectedCategoryValue = currentCategoryGroups.keySet().iterator().next();
        }

        renderCategoryChips();
        updateCategoryList();
    }

    private Map<String, List<Song>> buildCategoryGroups(List<Song> songs) {
        Map<String, List<Song>> groups = new LinkedHashMap<>();
        for (Song song : songs) {
            String key = getCategoryValue(song);
            if (!groups.containsKey(key)) {
                groups.put(key, new ArrayList<>());
            }
            groups.get(key).add(song);
        }

        List<Map.Entry<String, List<Song>>> entries = new ArrayList<>(groups.entrySet());
        Collections.sort(entries, (left, right) -> {
            int sizeCompare = Integer.compare(right.getValue().size(), left.getValue().size());
            if (sizeCompare != 0) {
                return sizeCompare;
            }
            return left.getKey().toLowerCase(Locale.getDefault())
                    .compareTo(right.getKey().toLowerCase(Locale.getDefault()));
        });

        Map<String, List<Song>> sortedGroups = new LinkedHashMap<>();
        for (Map.Entry<String, List<Song>> entry : entries) {
            entry.getValue().sort(Comparator.comparing(song -> song.title == null ? "" : song.title,
                    String.CASE_INSENSITIVE_ORDER));
            sortedGroups.put(entry.getKey(), entry.getValue());
        }
        return sortedGroups;
    }

    private String getCategoryValue(Song song) {
        String value;
        if (CATEGORY_MODE_ALBUM.equals(selectedCategoryMode)) {
            value = song.album;
            if (value == null || value.trim().isEmpty() || "Unknown Album".equalsIgnoreCase(value.trim())) {
                value = getString(R.string.unknown_album);
            }
        } else {
            value = song.singer != null && !song.singer.trim().isEmpty() ? song.singer : song.artist;
            if (value == null || value.trim().isEmpty()) {
                value = getString(R.string.unknown_artist);
            }
        }
        return value.trim();
    }

    private void renderCategoryChips() {
        categoryFilterGroup.removeAllViews();
        for (Map.Entry<String, List<Song>> entry : currentCategoryGroups.entrySet()) {
            Chip chip = new Chip(requireContext());
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setText(getCategoryLabel(entry.getKey(), entry.getValue().size()));
            chip.setTag(entry.getKey());
            chip.setEnsureMinTouchTargetSize(true);
            chip.setCheckedIconVisible(true);
            chip.setChipCornerRadius(18f);
            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.background_darker)));
            chip.setChipBackgroundColor(createChipBackgroundColors());
            chip.setTextColor(createChipTextColors());
            chip.setOnClickListener(v -> {
                selectedCategoryValue = (String) v.getTag();
                updateCategoryList();
            });
            if (entry.getKey().equals(selectedCategoryValue)) {
                chip.setChecked(true);
            }
            categoryFilterGroup.addView(chip);
        }
    }

    private String getCategoryLabel(String categoryName, int count) {
        return categoryName + " (" + count + ")";
    }

    private void updateCategoryList() {
        List<Song> selectedSongs = currentCategoryGroups.get(selectedCategoryValue);
        if (selectedSongs == null) {
            selectedSongs = new ArrayList<>();
        }
        categoryAdapter.setSongs(selectedSongs, "");

        if (selectedSongs.isEmpty()) {
            txtCategoryDescription.setText(R.string.category_empty);
            txtListCount.setText(getString(R.string.category_list_count_format, 0));
            return;
        }

        int currentPlayingId = viewModel.getCurrentSongId().getValue() != null
                ? viewModel.getCurrentSongId().getValue() : -1;
        categoryAdapter.setCurrentPlayingId(currentPlayingId);
        txtListCount.setText(getString(R.string.category_list_count_format, selectedSongs.size()));

        String prefix = CATEGORY_MODE_ALBUM.equals(selectedCategoryMode)
                ? getString(R.string.category_album_desc)
                : getString(R.string.category_artist_desc);
        txtCategoryDescription.setText(getString(
                R.string.category_result_format,
                prefix,
                selectedCategoryValue,
                selectedSongs.size()));
    }

    private void styleModeChip(Chip chip) {
        if (chip == null) {
            return;
        }
        chip.setCheckedIconVisible(true);
        chip.setChipCornerRadius(18f);
        chip.setChipStrokeWidth(1f);
        chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.background_darker)));
        chip.setChipBackgroundColor(createChipBackgroundColors());
        chip.setTextColor(createChipTextColors());
    }

    private ColorStateList createChipBackgroundColors() {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{
                ContextCompat.getColor(requireContext(), R.color.accent_teal),
                ContextCompat.getColor(requireContext(), R.color.search_bg)
        };
        return new ColorStateList(states, colors);
    }

    private ColorStateList createChipTextColors() {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] colors = new int[]{
                ContextCompat.getColor(requireContext(), R.color.white),
                ContextCompat.getColor(requireContext(), R.color.text_primary)
        };
        return new ColorStateList(states, colors);
    }

    @Override
    public void onDestroyView() {
        if (categorySongList != null) {
            categorySongList.setAdapter(null);
        }
        super.onDestroyView();
    }
}
