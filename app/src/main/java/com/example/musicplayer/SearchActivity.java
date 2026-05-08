package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicplayer.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private EditText searchEditText;
    private ImageView btnClearSearch;
    private RecyclerView searchResultList;
    private View searchSuggestions;
    private View emptySearchView;
    private com.google.android.material.chip.ChipGroup recentSearchGroup;
    private com.google.android.material.chip.ChipGroup hotSearchGroup;
    private ImageView btnClearHistory;
    private SearchResultAdapter searchAdapter;
    private List<String> searchHistory = new ArrayList<>();

    private static final String PREF_NAME = "search_prefs";
    private static final String KEY_HISTORY = "search_history";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        initViews();
        loadSearchHistory();
        setupSearch();
        setupHotSearches();
        observeViewModel();
    }
    
    private void initViews() {
        searchEditText = findViewById(R.id.searchEditText);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        searchResultList = findViewById(R.id.searchResultList);
        searchSuggestions = findViewById(R.id.searchSuggestions);
        emptySearchView = findViewById(R.id.emptySearchView);
        recentSearchGroup = findViewById(R.id.recentSearchGroup);
        hotSearchGroup = findViewById(R.id.hotSearchGroup);
        btnClearHistory = findViewById(R.id.btnClearHistory);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
    
    private void setupSearch() {
        searchAdapter = new SearchResultAdapter(song -> {
            saveSearchQuery(song.title);
            // 返回结果给MainActivity
            Intent resultIntent = new Intent();
            resultIntent.putExtra("song_id", song.id);
            resultIntent.putExtra("song_title", song.title);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        searchResultList.setLayoutManager(new LinearLayoutManager(this));
        searchResultList.setAdapter(searchAdapter);

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || 
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String query = searchEditText.getText().toString();
                if (!query.isEmpty()) {
                    saveSearchQuery(query);
                    performSearch(query);
                }
                return true;
            }
            return false;
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    performSearch(s.toString());
                } else {
                    showSuggestions();
                }
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> searchEditText.setText(""));

        btnClearHistory.setOnClickListener(v -> clearSearchHistory());
        
        // 自动弹出键盘
        searchEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT);
    }
    
    private void observeViewModel() {
        viewModel.getSearchResults().observe(this, songs -> {
            String query = searchEditText.getText().toString();
            if (songs != null && !query.isEmpty()) {
                searchAdapter.setSongs(songs, query);
                if (songs.isEmpty()) {
                    showEmptySearchUI();
                } else {
                    showSearchResultUI();
                }
            }
        });
    }
    
    private void performSearch(String query) {
        if (query.isEmpty()) {
            showSuggestions();
            return;
        }
        // 使用新的搜索API，默认加载50条结果
        viewModel.searchSongs(query, 1, 50);
    }
    
    private void saveSearchQuery(String query) {
        if (query == null || query.trim().isEmpty()) return;
        query = query.trim();
        searchHistory.remove(query);
        searchHistory.add(0, query);
        if (searchHistory.size() > 6) {
            searchHistory = new ArrayList<>(searchHistory.subList(0, 6));
        }
        saveSearchHistoryToPrefs();
    }

    private void loadSearchHistory() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String historyStr = prefs.getString(KEY_HISTORY, "");
        if (!historyStr.isEmpty()) {
            searchHistory = new ArrayList<>(Arrays.asList(historyStr.split(",")));
        }
        updateRecentSearchUI();
    }

    private void saveSearchHistoryToPrefs() {
        android.content.SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putString(KEY_HISTORY, String.join(",", searchHistory)).apply();
        updateRecentSearchUI();
    }

    private void clearSearchHistory() {
        searchHistory.clear();
        saveSearchHistoryToPrefs();
    }

    private void updateRecentSearchUI() {
        if (recentSearchGroup == null) return;
        recentSearchGroup.removeAllViews();
        for (int i = 0; i < Math.min(searchHistory.size(), 10); i++) {
            String query = searchHistory.get(i);
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(query);
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#25292E")));
            chip.setTextColor(android.graphics.Color.parseColor("#EEEEEE"));
            chip.setChipStrokeWidth(0f);
            chip.setChipCornerRadius(40f);
            chip.setChipMinHeight(0f);
            chip.setPadding(12, 8, 12, 8);
            chip.setTextSize(13);
            chip.setOnClickListener(v -> {
                searchEditText.setText(query);
                searchEditText.setSelection(query.length());
                performSearch(query);
            });
            recentSearchGroup.addView(chip);
        }
    }
    
    private void setupHotSearches() {
        if (hotSearchGroup == null) return;
        hotSearchGroup.removeAllViews();
        String[] hotTags = {"2026的最后100天", "动漫 · 城", "国产古装剧场", "Popular✨流行歌曲", "十年前的番剧之旅", "拜托，rap也会浪漫的好吧", "浪漫极致✨☁"};
        int[] colors = {
            android.graphics.Color.parseColor("#8A66E5"),
            android.graphics.Color.parseColor("#2E99D3"),
            android.graphics.Color.parseColor("#2E99D3"),
            android.graphics.Color.parseColor("#2E99D3"),
            android.graphics.Color.parseColor("#2E99D3"),
            android.graphics.Color.parseColor("#2E99D3"),
            android.graphics.Color.parseColor("#2E99D3")
        };

        for (int i = 0; i < hotTags.length; i++) {
            String tag = hotTags[i];
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            if (i == 0) {
                chip.setText("⭐ " + tag);
            } else {
                chip.setText("# " + tag);
            }
            chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(colors[i % colors.length]));
            chip.setTextColor(android.graphics.Color.WHITE);
            chip.setChipStrokeWidth(0f);
            chip.setChipCornerRadius(40f);
            chip.setTextSize(13);
            chip.setOnClickListener(v -> {
                searchEditText.setText(tag);
                searchEditText.setSelection(tag.length());
                performSearch(tag);
            });
            hotSearchGroup.addView(chip);
        }
    }
    
    private void showSearchResultUI() {
        searchResultList.setVisibility(View.VISIBLE);
        searchSuggestions.setVisibility(View.GONE);
        emptySearchView.setVisibility(View.GONE);
    }

    private void showEmptySearchUI() {
        searchResultList.setVisibility(View.GONE);
        searchSuggestions.setVisibility(View.GONE);
        emptySearchView.setVisibility(View.VISIBLE);
    }
    
    private void showSuggestions() {
        searchResultList.setVisibility(View.GONE);
        searchSuggestions.setVisibility(View.VISIBLE);
        emptySearchView.setVisibility(View.GONE);
        updateRecentSearchUI();
    }
    
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.fade_out);
    }
}
