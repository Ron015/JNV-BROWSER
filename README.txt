JNV Browser - Minimal Android project (Java)
------------------------------------------
What this contains:
- Fetch allowed websites JSON from:
  https://raw.githubusercontent.com/Ron015/JNV-BROWSER/main/weballow.json
- Home: RecyclerView that shows website cards (title + icon placeholder)
- BrowserActivity: ViewPager2 tabs with WebView fragments
- Download support via DownloadManager
Notes:
- This is a skeleton project. Import into Android Studio (Gradle plugin 8.1+).
- You may want to add network error handling, icons loading (Glide/Picasso), and polish.
