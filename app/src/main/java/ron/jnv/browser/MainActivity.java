package ron.jnv.browser;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements WebsiteAdapter.WebsiteClickListener {
    
    private static final String PREF_NAME = "browser_prefs";
    private static final String KEY_WEBSITES = "websites";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    
    private CoordinatorLayout coordinatorLayout;
    private RecyclerView websitesRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout browserContainer, webViewContainer;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private AutoCompleteTextView searchAutoComplete;
    private MaterialToolbar toolbar;
    private BottomNavigationView bottomNavigation;
    private FloatingActionButton fabNewTab;
    private TextView tvError;
    
    private WebsiteAdapter websiteAdapter;
    private List<Website> websiteList = new ArrayList<>();
    private List<Website> filteredWebsiteList = new ArrayList<>();
    private List<String> searchHistory = new ArrayList<>();
    
    private ArrayAdapter<String> searchAdapter;
    private SharedPreferences preferences;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    private List<BrowserTab> tabs = new ArrayList<>();
    private int currentTabId = 0;
    private boolean isDarkTheme = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupPreferences();
        setupTheme();
        setupRecyclerView();
        setupSearch();
        setupWebView();
        setupBottomNavigation();
        setupToolbar();
        loadWebsites();
    }

    private void initViews() {
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        websitesRecyclerView = findViewById(R.id.websitesRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        browserContainer = findViewById(R.id.browserContainer);
        webViewContainer = findViewById(R.id.webViewContainer);
        tabLayout = findViewById(R.id.tabLayout);
        progressBar = findViewById(R.id.progressBar);
        searchAutoComplete = findViewById(R.id.searchAutoComplete);
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        fabNewTab = findViewById(R.id.fabNewTab);
        tvError = findViewById(R.id.tvError);
        
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        setSupportActionBar(toolbar);
    }

    private void setupPreferences() {
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        loadSearchHistory();
    }

    private void setupTheme() {
        int theme = preferences.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(theme);
        isDarkTheme = (theme == AppCompatDelegate.MODE_NIGHT_YES);
    }

    private void setupRecyclerView() {
        websiteAdapter = new WebsiteAdapter(filteredWebsiteList, this);
        websitesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        websitesRecyclerView.setAdapter(websiteAdapter);
        
        swipeRefreshLayout.setOnRefreshListener(this::refreshWebsites);
    }

    private void setupSearch() {
        searchAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_dropdown_item_1line, searchHistory);
        searchAutoComplete.setAdapter(searchAdapter);
        
        searchAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            String query = searchAdapter.getItem(position);
            performSearch(query);
        });
        
        searchAutoComplete.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(searchAutoComplete.getText().toString());
            return true;
        });
        
        findViewById(R.id.btnClearSearch).setOnClickListener(v -> {
            searchAutoComplete.setText("");
            filterWebsites("");
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        fabNewTab.setOnClickListener(v -> createNewTab("https://www.google.com"));
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switchTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Optional: Refresh tab or show tab overview
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                showHomeScreen();
                return true;
            } else if (itemId == R.id.nav_back) {
                goBack();
                return true;
            } else if (itemId == R.id.nav_forward) {
                goForward();
                return true;
            } else if (itemId == R.id.nav_refresh) {
                refreshCurrentTab();
                return true;
            } else if (itemId == R.id.nav_downloads) {
                showDownloads();
                return true;
            }
            return false;
        });
    }

    private void setupToolbar() {
        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_settings) {
                showSettings();
                return true;
            } else if (itemId == R.id.menu_downloads) {
                showDownloads();
                return true;
            } else if (itemId == R.id.menu_history) {
                showHistory();
                return true;
            } else if (itemId == R.id.menu_theme) {
                toggleTheme();
                return true;
            }
            return false;
        });
    }

    private void loadWebsites() {
        // Load from shared preferences first
        String websitesJson = preferences.getString(KEY_WEBSITES, null);
        if (websitesJson != null) {
            Type listType = new TypeToken<List<Website>>(){}.getType();
            List<Website> savedWebsites = new Gson().fromJson(websitesJson, listType);
            if (savedWebsites != null) {
                websiteList.addAll(savedWebsites);
                filteredWebsiteList.addAll(websiteList);
                websiteAdapter.notifyDataSetChanged();
            }
        }
        
        // Then try to fetch from API
        fetchWebsitesFromApi();
    }

    private void fetchWebsitesFromApi() {
        swipeRefreshLayout.setRefreshing(true);
        
        executorService.execute(() -> {
            try {
                // Simulate API call - replace with your actual API implementation
                Thread.sleep(1000);
                
                List<Website> apiWebsites = new ArrayList<>();
                apiWebsites.add(new Website("", "Google", "https://www.google.com", "Search"));
                apiWebsites.add(new Website("", "YouTube", "https://www.youtube.com", "Video"));
                apiWebsites.add(new Website("", "GitHub", "https://www.github.com", "Development"));
                apiWebsites.add(new Website("", "Stack Overflow", "https://stackoverflow.com", "Q&A"));
                apiWebsites.add(new Website("", "Medium", "https://medium.com", "Blogging"));
                apiWebsites.add(new Website("", "Twitter", "https://twitter.com", "Social"));
                
                mainHandler.post(() -> {
                    websiteList.clear();
                    websiteList.addAll(apiWebsites);
                    filteredWebsiteList.clear();
                    filteredWebsiteList.addAll(websiteList);
                    websiteAdapter.notifyDataSetChanged();
                    saveWebsites();
                    swipeRefreshLayout.setRefreshing(false);
                });
                
            } catch (Exception e) {
                mainHandler.post(() -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Snackbar.make(coordinatorLayout, "Failed to load websites", Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void saveWebsites() {
        String websitesJson = new Gson().toJson(websiteList);
        preferences.edit().putString(KEY_WEBSITES, websitesJson).apply();
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) return;
        
        addToSearchHistory(query);
        
        if (query.startsWith("http://") || query.startsWith("https://")) {
            loadUrlInCurrentTab(query);
        } else {
            // Search within websites
            filterWebsites(query);
            
            // Or search on Google
            loadUrlInCurrentTab("https://www.google.com/search?q=" + query);
        }
    }

    private void filterWebsites(String query) {
        filteredWebsiteList.clear();
        
        if (TextUtils.isEmpty(query)) {
            filteredWebsiteList.addAll(websiteList);
        } else {
            for (Website website : websiteList) {
                if (website.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    website.getUrl().toLowerCase().contains(query.toLowerCase()) ||
                    website.getCategory().toLowerCase().contains(query.toLowerCase())) {
                    filteredWebsiteList.add(website);
                }
            }
        }
        
        websiteAdapter.notifyDataSetChanged();
    }

    private void addToSearchHistory(String query) {
        if (!searchHistory.contains(query)) {
            searchHistory.add(0, query);
            // Keep only last 10 searches
            if (searchHistory.size() > 10) {
                searchHistory.remove(searchHistory.size() - 1);
            }
            saveSearchHistory();
            searchAdapter.notifyDataSetChanged();
        }
    }

    private void loadSearchHistory() {
        String historyJson = preferences.getString(KEY_SEARCH_HISTORY, "[]");
        Type listType = new TypeToken<List<String>>(){}.getType();
        List<String> history = new Gson().fromJson(historyJson, listType);
        if (history != null) {
            searchHistory.addAll(history);
        }
    }

    private void saveSearchHistory() {
        String historyJson = new Gson().toJson(searchHistory);
        preferences.edit().putString(KEY_SEARCH_HISTORY, historyJson).apply();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createNewTab(String url) {
        BrowserTab newTab = BrowserTab.newInstance(++currentTabId, url);
        tabs.add(newTab);
        
        TabLayout.Tab tab = tabLayout.newTab();
        tab.setText("New Tab");
        tabLayout.addTab(tab, true);
        
        getSupportFragmentManager().beginTransaction()
            .add(R.id.webViewContainer, newTab, "tab_" + currentTabId)
            .commit();
        
        showBrowser();
    }

    private void switchTab(int position) {
        if (position >= 0 && position < tabs.size()) {
            BrowserTab tab = tabs.get(position);
            // Show the selected tab and hide others
            for (int i = 0; i < tabs.size(); i++) {
                BrowserTab current = tabs.get(i);
                if (i == position) {
                    getSupportFragmentManager().beginTransaction().show(current).commit();
                } else {
                    getSupportFragmentManager().beginTransaction().hide(current).commit();
                }
            }
        }
    }

    private void loadUrlInCurrentTab(String url) {
        if (!tabs.isEmpty()) {
            BrowserTab currentTab = tabs.get(tabLayout.getSelectedTabPosition());
            currentTab.loadUrl(url);
        }
    }

    private void showHomeScreen() {
        browserContainer.setVisibility(View.GONE);
        websitesRecyclerView.setVisibility(View.VISIBLE);
        bottomNavigation.getMenu().findItem(R.id.nav_home).setChecked(true);
    }

    private void showBrowser() {
        websitesRecyclerView.setVisibility(View.GONE);
        browserContainer.setVisibility(View.VISIBLE);
    }

    private void goBack() {
        if (!tabs.isEmpty()) {
            BrowserTab currentTab = tabs.get(tabLayout.getSelectedTabPosition());
            if (currentTab.canGoBack()) {
                currentTab.goBack();
            }
        }
    }

    private void goForward() {
        if (!tabs.isEmpty()) {
            BrowserTab currentTab = tabs.get(tabLayout.getSelectedTabPosition());
            if (currentTab.canGoForward()) {
                currentTab.goForward();
            }
        }
    }

    private void refreshCurrentTab() {
        if (!tabs.isEmpty()) {
            BrowserTab currentTab = tabs.get(tabLayout.getSelectedTabPosition());
            currentTab.reload();
        }
    }

    private void showDownloads() {
        startActivity(new Intent(this, DownloadsActivity.class));
    }

    private void showSettings() {
        // Implement settings activity/dialog
        Toast.makeText(this, "Settings will be implemented", Toast.LENGTH_SHORT).show();
    }

    private void showHistory() {
        // Implement history view
        Toast.makeText(this, "History will be implemented", Toast.LENGTH_SHORT).show();
    }

    private void toggleTheme() {
        int currentTheme = AppCompatDelegate.getDefaultNightMode();
        int newTheme = currentTheme == AppCompatDelegate.MODE_NIGHT_YES ? 
            AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
        
        AppCompatDelegate.setDefaultNightMode(newTheme);
        preferences.edit().putInt(KEY_THEME, newTheme).apply();
        recreate();
    }

    private void refreshWebsites() {
        fetchWebsitesFromApi();
    }

    @Override
    public void onWebsiteClick(Website website) {
        website.incrementVisitCount();
        website.setLastVisited(System.currentTimeMillis());
        
        if (tabs.isEmpty()) {
            createNewTab(website.getUrl());
        } else {
            loadUrlInCurrentTab(website.getUrl());
        }
        
        showBrowser();
    }

    @Override
    public void onWebsiteLongClick(Website website) {
        // Show context menu for website (favorite, delete, etc.)
        showWebsiteContextMenu(website);
    }

    private void showWebsiteContextMenu(Website website) {
        // Implement context menu dialog
        Toast.makeText(this, "Long press: " + website.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (browserContainer.getVisibility() == View.VISIBLE) {
            if (!tabs.isEmpty()) {
                BrowserTab currentTab = tabs.get(tabLayout.getSelectedTabPosition());
                if (currentTab.canGoBack()) {
                    currentTab.goBack();
                    return;
                }
            }
            showHomeScreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    // WebsiteAdapter and other inner classes would be in separate files
    // Continuing with simplified version...
}