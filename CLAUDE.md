# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Kugou-concept-style Android music player app with a Node.js backend. The app streams music, displays synced lyrics, supports downloads, favorites, and user accounts.

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Output: app/release/app-release.apk
```

Backend (in `web/`):
```bash
cd web && npm install && npm start     # Runs on port 3000
```

## Architecture

### Android App (Java 11, minSdk 31, targetSdk 35)

**MVVM pattern** — `MainActivity` owns the UI, `MainViewModel` holds all observable state via `MutableLiveData`, and Room DAOs (`SongDao`, `RecentPlayDao`) handle persistence.

- **`MainActivity`** — Single-activity app. Manages fragment navigation (Discover, Player, Mine), the Media3 `MediaController` lifecycle, vinyl rotation animation, and lyric sync. All playback control flows through LiveData observations on `MainViewModel`.
- **`MainViewModel`** — Central state hub. Exposes LiveData for playback state, current song metadata, lyrics, user profile, and song lists. Runs DB writes and network calls on a shared `ExecutorService` (4-thread fixed pool). Fetches remote songs from `BASE_URL + "/api/songs"` and syncs them into Room.
- **`PlaybackService`** — Foreground `MediaSessionService` wrapping an ExoPlayer instance. Handles audio focus and noisy-audio-becoming (headphone unplug).
- **Fragments**: `DiscoveryFragment`, `PlayerFragment` (full-screen player with lyrics RecyclerView + SeekBar), `MineFragment` (user center), `LocalMusicFragment`, `FavoriteMusicFragment`, `RecentPlayFragment`, `ImportedMusicFragment`, `SearchActivity`, `SettingsFragment`, `EditProfileFragment`.
- **Data**: `Song` is the Room `@Entity` (table `songs`). Key fields: `id`, `title`, `artist`, `singer`, `path` (URL or local path), `coverUrl`, `isLocal`, `isFavorite`, `lrcId`, `lyrics`. `RecentPlay` tracks playback history with a 7-day window.
- **Networking**: OkHttp singleton (`HttpClient`) with 15s/30s/30s timeouts and a `RetryInterceptor` (3 retries). Gson for JSON parsing.
- **Lyrics**: `LrcLibService` fetches LRC lyrics from LRCLIB API. `LyricUtils.parseLrc()` parses LRC format into `List<LyricEntry>`. `LyricAdapter` renders lyrics in a RecyclerView. MainActivity syncs current playback position to lyrics via binary search on the sorted lyric list.
- **Images**: Coil for album art loading, Palette API for dynamic background color extraction + blur.
- **Download**: `DownloadManager` downloads songs to external files dir (`DIRECTORY_MUSIC`), updates the database to mark `isLocal = true`.

### Backend (Node.js/Express, `web/server.js`)

REST API server connected to a remote MySQL database. Key endpoints:
- `GET /api/songs` / `GET /api/songs/all` — paginated / full song list
- `GET /api/song/detail?id=` — single song detail
- `GET /api/song/search?q=` — search by title/singer/album
- `GET /api/song/play?id=` — audio streaming with HTTP Range support
- `GET /api/app/version` — update check (version table)
- `POST /api/user/register`, `POST /api/user/login` — JWT-based auth
- `GET/PUT /api/user/profile` — authenticated profile CRUD
- Static files served from `/musicplayer` directory, mapped to `/resource` URL path

### Database

Remote MySQL (`musicplayer` database) with tables: `songs`, `users`, `version`, `recent_play`. Room database on-device mirrors remote song data for offline access and local state (favorites, local files, lyrics cache).

## Key Files

| File | Role |
|------|------|
| `app/src/main/java/.../MainActivity.java` | Single activity, fragment host, playback controller |
| `app/src/main/java/.../MainViewModel.java` | All observable state + business logic |
| `app/src/main/java/.../PlaybackService.java` | Foreground media playback service |
| `app/src/main/java/.../PlayerFragment.java` | Full-screen player UI |
| `app/src/main/java/.../Song.java` | Room entity |
| `app/src/main/java/.../SongDao.java` | Room DAO (queries, inserts, updates) |
| `app/src/main/java/.../AppDatabase.java` | Room database singleton (version 9) |
| `app/src/main/java/.../LrcLibService.java` | LRC lyrics API client |
| `app/src/main/java/.../LyricUtils.java` | LRC format parser |
| `app/src/main/java/.../DownloadManager.java` | Song download with progress |
| `app/src/main/java/.../UpdateManager.java` | In-app update check/download |
| `app/src/main/java/.../UserManager.java` | Local user session (SharedPreferences) |
| `app/src/main/java/.../HttpClient.java` | OkHttp singleton |
| `web/server.js` | Express API + static file server |
| `database_indexes.sql` | MySQL index definitions |
