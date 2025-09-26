package ron.jnv.browser;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements WebsiteAdapter.WebsiteClickListener {
    
    private RecyclerView websitesRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout browserContainer, webViewContainer;
    private TabLayout tabLayout;
    private ProgressBar progressBar;
    private EditText searchEditText;
    private Toolbar toolbar;
    private FloatingActionButton fabNewTab;
    private TextView tvError;
    
    private WebsiteAdapter websiteAdapter;
    private List<Website> websiteList = new ArrayList<>();
    private List<Website> filteredWebsiteList = new ArrayList<>();
    
    private ExecutorService executorService;
    private Handler mainHandler;
    
    private List<WebView> webViews = new ArrayList<>();
    private int currentTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupRecyclerView();
        setupSearch();
        setupWebView();
        loadWebsitesFromApi();
    }

    private void initViews() {
        websitesRecyclerView = findViewById(R.id.websitesRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        browserContainer = findViewById(R.id.browserContainer);
        webViewContainer = findViewById(R.id.webViewContainer);
        tabLayout = findViewById(R.id.tabLayout);
        progressBar = findViewById(R.id.progressBar);
        searchEditText = findViewById(R.id.searchEditText);
        toolbar = findViewById(R.id.toolbar);
        fabNewTab = findViewById(R.id.fabNewTab);
        tvError = findViewById(R.id.tvError);
        
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("JNV Browser");
    }

    private void setupRecyclerView() {
        websiteAdapter = new WebsiteAdapter(filteredWebsiteList, this);
        websitesRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        websitesRecyclerView.setAdapter(websiteAdapter);
        
        swipeRefreshLayout.setOnRefreshListener(this::loadWebsitesFromApi);
    }

    private void setupSearch() {
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            performSearch(searchEditText.getText().toString());
            return true;
        });
        
        ImageButton btnClearSearch = findViewById(R.id.btnClearSearch);
        btnClearSearch.setOnClickListener(v -> {
            searchEditText.setText("");
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
                // Tab reselected - could show tab overview
            }
        });
        
        // Create initial tab
        createNewTab("https://www.google.com");
    }

    @SuppressLint("SetJavaScriptEnabled")
    private WebView createWebView() {
        WebView webView = new WebView(this);
        WebSettings webSettings = webView.getSettings();
        
        // Enable basic features
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        
        // Enable zoom controls
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        
        // Set WebView clients
        webView.setWebViewClient(new AdvancedWebViewClient());
        webView.setWebChromeClient(new AdvancedWebChromeClient());
        webView.setDownloadListener(new AdvancedDownloadListener(this));
        
        return webView;
    }

    private void loadWebsitesFromApi() {
        swipeRefreshLayout.setRefreshing(true);
        
        ApiService apiService = RetrofitClient.getApiService();
        Call<List<Website>> call = apiService.getAllowedWebsites();
        
        call.enqueue(new Callback<List<Website>>() {
            @Override
            public void onResponse(Call<List<Website>> call, Response<List<Website>> response) {
                swipeRefreshLayout.setRefreshing(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    websiteList.clear();
                    websiteList.addAll(response.body());
                    filteredWebsiteList.clear();
                    filteredWebsiteList.addAll(websiteList);
                    websiteAdapter.notifyDataSetChanged();
                    
                    Toast.makeText(MainActivity.this, 
                        "Loaded " + websiteList.size() + " websites", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, 
                        "Failed to load websites", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Website>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(MainActivity.this, 
                    "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                
                // Load default websites if API fails
                loadDefaultWebsites();
            }
        });
    }

    private void loadDefaultWebsites() {
        // Fallback websites if API fails
        websiteList.clear();
        websiteList.add(new Website("", "Google", "https://www.google.com"));
        websiteList.add(new Website("", "YouTube", "https://www.youtube.com"));
        websiteList.add(new Website("", "GitHub", "https://www.github.com"));
        websiteList.add(new Website("", "JNV Official", "https://navodaya.gov.in"));
        
        filteredWebsiteList.clear();
        filteredWebsiteList.addAll(websiteList);
        websiteAdapter.notifyDataSetChanged();
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) return;
        
        if (query.startsWith("http://") || query.startsWith("https://")) {
            loadUrlInCurrentTab(query);
        } else {
            filterWebsites(query);
            // Also search on Google if it's not a website filter
            if (filteredWebsiteList.isEmpty()) {
                loadUrlInCurrentTab("https://www.google.com/search?q=" + query);
            }
        }
    }

    private void filterWebsites(String query) {
        filteredWebsiteList.clear();
        
        if (TextUtils.isEmpty(query)) {
            filteredWebsiteList.addAll(websiteList);
        } else {
            for (Website website : websiteList) {
                if (website.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    website.getUrl().toLowerCase().contains(query.toLowerCase())) {
                    filteredWebsiteList.add(website);
                }
            }
        }
        
        websiteAdapter.notifyDataSetChanged();
    }

    private void createNewTab(String url) {
        WebView webView = createWebView();
        webViews.add(webView);
        
        TabLayout.Tab tab = tabLayout.newTab();
        tab.setText("New Tab");
        tabLayout.addTab(tab, true);
        
        currentTabIndex = webViews.size() - 1;
        updateWebViewContainer();
        
        if (url != null && !url.isEmpty()) {
            webView.loadUrl(url);
        }
        
        showBrowser();
    }

    private void switchTab(int position) {
        if (position >= 0 && position < webViews.size()) {
            currentTabIndex = position;
            updateWebViewContainer();
            tabLayout.getTabAt(position).select();
        }
    }

    private void updateWebViewContainer() {
        webViewContainer.removeAllViews();
        if (currentTabIndex >= 0 && currentTabIndex < webViews.size()) {
            WebView currentWebView = webViews.get(currentTabIndex);
            webViewContainer.addView(currentWebView);
        }
    }

    private void loadUrlInCurrentTab(String url) {
        if (currentTabIndex >= 0 && currentTabIndex < webViews.size()) {
            WebView currentWebView = webViews.get(currentTabIndex);
            currentWebView.loadUrl(url);
            showBrowser();
        }
    }

    private void showHomeScreen() {
        browserContainer.setVisibility(View.GONE);
        websitesRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showBrowser() {
        websitesRecyclerView.setVisibility(View.GONE);
        browserContainer.setVisibility(View.VISIBLE);
    }

    private void goBack() {
        if (currentTabIndex >= 0 && currentTabIndex < webViews.size()) {
            WebView currentWebView = webViews.get(currentTabIndex);
            if (currentWebView.canGoBack()) {
                currentWebView.goBack();
            } else {
                showHomeScreen();
            }
        }
    }

    private void refreshCurrentTab() {
        if (currentTabIndex >= 0 && currentTabIndex < webViews.size()) {
            WebView currentWebView = webViews.get(currentTabIndex);
            currentWebView.reload();
        }
    }

    @Override
    public void onWebsiteClick(Website website) {
        loadUrlInCurrentTab(website.getUrl());
    }

    @Override
    public void onBackPressed() {
        if (browserContainer.getVisibility() == View.VISIBLE) {
            if (currentTabIndex >= 0 && currentTabIndex < webViews.size()) {
                WebView currentWebView = webViews.get(currentTabIndex);
                if (currentWebView.canGoBack()) {
                    currentWebView.goBack();
                    return;
                }
            }
            showHomeScreen();
        } else {
            super.onBackPressed();
        }
    }

    // WebView Client Classes
    private class AdvancedWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return false;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
            tvError.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            updateTabTitle(view.getTitle());
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            progressBar.setVisibility(View.GONE);
            tvError.setVisibility(View.VISIBLE);
        }
    }

    private class AdvancedWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            updateTabTitle(title);
        }
    }

    private void updateTabTitle(String title) {
        if (currentTabIndex >= 0 && currentTabIndex < tabLayout.getTabCount()) {
            TabLayout.Tab tab = tabLayout.getTabAt(currentTabIndex);
            if (tab != null && title != null) {
                String tabTitle = title.length() > 20 ? title.substring(0, 20) + "..." : title;
                tab.setText(tabTitle);
            }
        }
    }

    // Download Listener
    private static class AdvancedDownloadListener implements DownloadListener {
        private Context context;
        
        public AdvancedDownloadListener(Context context) {
            this.context = context;
        }
        
        @Override
        public void onDownloadStart(String url, String userAgent, 
                                   String contentDisposition, String mimetype, 
                                   long contentLength) {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
            request.setTitle(fileName);
            request.setDescription("Downloading file...");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            manager.enqueue(request);
            
            Toast.makeText(context, "Download started: " + fileName, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}