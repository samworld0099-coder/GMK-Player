# GMK Media Player

**GMK Media Player** is a native Android application engineered to combine the simplicity and clean design of **Google Files**, the archive inspection, extraction, and compression power of **ZArchiver**, and the advanced video playback controls of **VLC Media Player**.

---

## 🌟 Major Capabilities

### 1. Google Files-Style File Manager
- **Storage Volumes & Quick Folders**: Real-time storage consumption meter for internal and external storage; 1-tap quick access to Downloads, Movies, Music, Pictures, Documents, and DCIM.
- **File Operations**: Copy, Cut, Paste, Rename, Delete (with confirmation), Create New Folder, and Create Text File.
- **Conflict Handling**: Replace, Skip, or Auto-rename duplicates (`filename (1).ext`).
- **Organization & View Modes**: Dynamic grid and list views; sorting by Name, Date Modified, Size, and File Type.
- **Properties Inspector**: Detailed metadata inspector including resolution, duration, MIME type, size, and modified timestamps.
- **Sharesheet & Intent Integration**: Direct integration with Android Sharesheet and intent filters for `ACTION_VIEW` and `ACTION_SEND`.

### 2. ZArchiver-Grade Archive Engine
- **Supported Formats**:
  - **ZIP**: Read, browse, extract, and create (Zip64, Unicode/UTF-8 extra field handling, Deflate & Stored methods).
  - **7z**: Read, navigate internal folders, and extract via Apache Commons Compress `SevenZFile`.
  - **TAR & TAR.GZ (TGZ)**: Full POSIX TAR archive creation, inspection, and extraction.
  - **GZIP, BZIP2, XZ**: Decompression and streaming processing.
- **Virtual Directory Tree**: Nested folder navigation inside archives with breadcrumbs (`Root > folder > subfolder`) without unpacking.
- **Selective Extraction**: Extract individual files, specific folders, or the complete archive.
- **Archive Security**:
  - **Path Traversal Protection**: Prevents "Zip Slip" directory escapes (`../../outside.txt` detection).
  - **Zip Bomb Guard**: Enforces entry size heuristics and decompression limits.
- **Extraction HUD**: Real-time progress bar, currently extracted entry name, bytes written, and instant safe cancellation.

### 3. VLC-Style Advanced Video Player
- **Engine**: Powered by AndroidX Media3 ExoPlayer.
- **Fullscreen Minimalist HUD**: Clean auto-hiding controls (4-second timeout), lockable screen overlay, timeline scrubber, and elapsed/total duration.
- **Gesture Controls**:
  - **Left Vertical Swipe**: Screen brightness adjustment with floating HUD badge.
  - **Right Vertical Swipe**: Media stream volume adjustment with floating HUD badge.
  - **Double-Tap**: Double-tap left to rewind 10s, double-tap right to forward 10s with visual indicator.
- **VLC Screen Fit & Display Modes**:
  - Fit Screen (Default aspect ratio)
  - Stretch to Fill
  - Zoom / Crop (center-cropped full bleed)
  - Fit Width / Fit Height
  - 100% Original Resolution
  - 90° Clockwise and Counter-Clockwise Rotation with Reset
- **Subtitles Engine**:
  - Embedded subtitle tracks extraction and switching.
  - External subtitle file picker (`.srt`, `.vtt`, `.ttml`).
  - Auto-discovery of candidate subtitles in identical directory (`Movie.srt`, `Movie.en.srt`).
  - Customizable font size and background opacity.
- **Audio Track Selection**: Multiple audio tracks selection (languages, channels, codecs) on the fly without playback reset.
- **Playback Persistence**: Automatically stores resume timestamp in Room database with "Resume from last saved position" prompt.
- **Picture-in-Picture (PiP)**: Multi-window and PiP mode support on Android 8.0+.

### 4. Full-Featured Music Player
- **Discovery**: MediaStore query with automatic fallback folder scanning.
- **Controls**: Play/pause, next, previous, scrubbing, shuffle, repeat (Off, One, All).
- **Persistent Mini-Player**: Bottom bar follows the user while navigating file browser and tabs.
- **Expanded Player Sheet**: Rotating vinyl album artwork animation, track timeline, favorites toggle, and live queue sheet.

### 5. Favorites, History & Global Search
- **Playback History**: Chronological log of watched videos and songs with resume positions.
- **Favorites**: Pinned items with instant 1-tap open.
- **Global Search**: Search by name or extension with category filter chips (All, Videos, Music, Archives, Documents).

---

## 🛠 Tech Stack & Architecture

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM + Clean Architecture with Coroutines and Flow
- **Media Engine**: AndroidX Media3 ExoPlayer (`media3-exoplayer`, `media3-ui`, `media3-session`, `media3-common`)
- **Archive Engine**: Apache Commons Compress (`org.apache.commons:commons-compress`) + `java.util.zip`
- **Local Persistence**: Jetpack Room Database (`RoomRuntime`, `RoomKtx`, KSP compiler)
- **Image & Thumbnail Loading**: Coil Compose
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`)

---

## 🚀 Build & Installation Instructions

### 1. Requirements
- JDK 17 or higher
- Android SDK 36 (minSdk 24, targetSdk 36)
- Gradle 8+

### 2. Run Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### 3. Build Debug APK
```bash
gradle :app:assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### 4. Install on Android Device
Connect your Android phone via USB with USB Debugging enabled:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Or export the APK directly from the settings menu in Google AI Studio.
