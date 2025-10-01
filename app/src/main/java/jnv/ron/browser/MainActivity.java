package jnv.ron.browser;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.webkit.*;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private LinearLayout tabsContainer;
    private ProgressBar progressBar;
    private WebView currentWebView;
    private ImageButton btnNewTab, btnMenu;
    private TextView txtNoTabs;
    
    private List<WebView> webViews = new ArrayList<>();
    private List<String> tabTitles = new ArrayList<>();
    private List<String> tabUrls = new ArrayList<>();
    private List<String> allowedDomains = new ArrayList<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler();
    private static final int PERMISSION_REQUEST_CODE = 100;
    private int currentTabIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupToolbar();
        setupNavigation();
        loadAllowedDomains();
        createHomeTab();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);
        tabsContainer = findViewById(R.id.tabsContainer);
        progressBar = findViewById(R.id.progressBar);
        btnNewTab = findViewById(R.id.btnNewTab);
        btnMenu = findViewById(R.id.btnMenu);
        txtNoTabs = findViewById(R.id.txtNoTabs);

        btnNewTab.setOnClickListener(v -> createNewTab("https://www.google.com"));
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ron Browser");
        }
    }

    private void setupNavigation() {
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void loadAllowedDomains() {
        executorService.execute(() -> {
            try {
                URL domainsUrl = new URL("https://raw.githubusercontent.com/Ron015/API-EXAMPLE/main/dom.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(domainsUrl.openStream()));
                String line;
                List<String> domains = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    String domain = line.trim().toLowerCase();
                    if (!domain.isEmpty() && !domain.startsWith("#")) {
                        domains.add(domain);
                    }
                }
                reader.close();
                
                handler.post(() -> {
                    allowedDomains.clear();
                    allowedDomains.addAll(domains);
                });
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback domains
                handler.post(() -> {
                    allowedDomains.clear();
                    allowedDomains.add("google.com");
                    allowedDomains.add("github.com");
                    allowedDomains.add("youtube.com");
                    allowedDomains.add("wikipedia.org");
                    allowedDomains.add("stackoverflow.com");
                });
            }
        });
    }

    private void createHomeTab() {
        createNewTab("file:///android_asset/main.html");
        tabTitles.set(0, "Home");
        updateTabUI(0);
    }

    private void createNewTab(String url) {
        WebView webView = new WebView(this);
        setupWebView(webView);
        
        webViews.add(webView);
        tabTitles.add("New Tab");
        tabUrls.add(url);
        
        int tabIndex = webViews.size() - 1;
        addTabToUI(tabIndex);
        switchToTab(tabIndex);
        
        if (!url.equals("file:///android_asset/main.html")) {
            webView.loadUrl(url);
        }
        
        updateNoTabsVisibility();
    }

    private void setupWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        
        // Enable JavaScript and DOM
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        // Enable advanced features
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        
        // Enable permissions
        webSettings.setGeolocationEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        
        // Cache settings
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());
        
        // Handle downloads
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            handleDownload(url, userAgent, contentDisposition, mimetype);
        });
        
        // Add JavaScript interface for home page
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
    }

    private void addTabToUI(int tabIndex) {
        View tabView = getLayoutInflater().inflate(R.layout.item_tab, tabsContainer, false);
        TextView tabTitle = tabView.findViewById(R.id.tabTitle);
        ImageButton btnClose = tabView.findViewById(R.id.btnCloseTab);
        
        tabTitle.setText(tabTitles.get(tabIndex));
        tabView.setTag(tabIndex);
        
        tabView.setOnClickListener(v -> {
            int index = (int) v.getTag();
            switchToTab(index);
        });
        
        btnClose.setOnClickListener(v -> {
            int index = (int) tabView.getTag();
            closeTab(index);
        });
        
        tabsContainer.addView(tabView);
        updateTabUI(tabIndex);
    }

    private void switchToTab(int tabIndex) {
        // Hide all webviews
        for (WebView webView : webViews) {
            webView.setVisibility(View.GONE);
        }
        
        // Remove active class from all tabs
        for (int i = 0; i < tabsContainer.getChildCount(); i++) {
            View tabView = tabsContainer.getChildAt(i);
            tabView.setBackgroundColor(Color.TRANSPARENT);
        }
        
        // Show selected webview
        currentWebView = webViews.get(tabIndex);
        currentWebView.setVisibility(View.VISIBLE);
        currentTabIndex = tabIndex;
        
        // Add active class to selected tab
        if (tabIndex < tabsContainer.getChildCount()) {
            View tabView = tabsContainer.getChildAt(tabIndex);
            tabView.setBackgroundColor(Color.parseColor("#E3F2FD"));
        }
        
        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(tabTitles.get(tabIndex));
        }
    }

    private void closeTab(int tabIndex) {
        if (webViews.size() <= 1) {
            Toast.makeText(this, "Cannot close the last tab", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Remove webview
        WebView webViewToRemove = webViews.get(tabIndex);
        webViewToRemove.destroy();
        webViews.remove(tabIndex);
        tabTitles.remove(tabIndex);
        tabUrls.remove(tabIndex);
        
        // Remove tab from UI
        tabsContainer.removeViewAt(tabIndex);
        
        // Update remaining tab tags
        for (int i = tabIndex; i < tabsContainer.getChildCount(); i++) {
            View tabView = tabsContainer.getChildAt(i);
            tabView.setTag(i);
        }
        
        // Switch to another tab
        int newTabIndex = Math.min(tabIndex, webViews.size() - 1);
        switchToTab(newTabIndex);
        
        updateNoTabsVisibility();
    }

    private void updateTabUI(int tabIndex) {
        if (tabIndex < tabsContainer.getChildCount()) {
            View tabView = tabsContainer.getChildAt(tabIndex);
            TextView tabTitle = tabView.findViewById(R.id.tabTitle);
            tabTitle.setText(tabTitles.get(tabIndex));
            
            if (tabIndex == currentTabIndex) {
                tabView.setBackgroundColor(Color.parseColor("#E3F2FD"));
            } else {
                tabView.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    private void updateNoTabsVisibility() {
        txtNoTabs.setVisibility(webViews.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void handleDownload(String url, String userAgent, String contentDisposition, String mimetype) {
        if (checkPermissions()) {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setMimeType(mimetype);
            request.addRequestHeader("User-Agent", userAgent);
            request.setDescription("Downloading file...");
            request.setTitle("Download");
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, getFileNameFromUrl(url));
            
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(this, "Download started...", Toast.LENGTH_LONG).show();
            }
        } else {
            requestPermissions();
        }
    }

    private String getFileNameFromUrl(String url) {
        try {
            return url.substring(url.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return "download_" + System.currentTimeMillis();
        }
    }

    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                },
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (isUrlAllowed(url)) {
                return false;
            } else {
                showBlockedPage(view, url);
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            
            // Update current tab URL
            int tabIndex = webViews.indexOf(view);
            if (tabIndex != -1) {
                tabUrls.set(tabIndex, url);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            
            // Update tab title with page title
            int tabIndex = webViews.indexOf(view);
            if (tabIndex != -1) {
                String title = view.getTitle();
                if (title != null && !title.isEmpty() && !url.equals("file:///android_asset/main.html")) {
                    tabTitles.set(tabIndex, title);
                    updateTabUI(tabIndex);
                    
                    if (tabIndex == currentTabIndex && getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(title);
                    }
                }
            }
        }

        private boolean isUrlAllowed(String url) {
            if (url.startsWith("file:///android_asset/")) {
                return true; // Allow local files
            }
            
            try {
                String host = Uri.parse(url).getHost().toLowerCase();
                for (String domain : allowedDomains) {
                    if (host.equals(domain) || host.endsWith("." + domain)) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // Invalid URL
            }
            return false;
        }

        private void showBlockedPage(WebView webView, String blockedUrl) {
            String html = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<style>" +
                    "body { font-family: 'Segoe UI', Arial, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); margin: 0; padding: 20px; display: flex; justify-content: center; align-items: center; min-height: 100vh; color: white; }" +
                    ".blocked-container { background: rgba(255,255,255,0.1); backdrop-filter: blur(10px); padding: 40px; border-radius: 20px; text-align: center; max-width: 400px; }" +
                    "h1 { font-size: 2.5em; margin-bottom: 20px; }" +
                    "p { font-size: 1.1em; margin-bottom: 30px; opacity: 0.9; }" +
                    ".url { background: rgba(255,255,255,0.2); padding: 10px; border-radius: 10px; margin: 20px 0; font-family: monospace; word-break: break-all; }" +
                    "button { background: #ff4757; color: white; border: none; padding: 12px 30px; border-radius: 25px; font-size: 1em; cursor: pointer; transition: all 0.3s; }" +
                    "button:hover { background: #ff3742; transform: translateY(-2px); }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<div class='blocked-container'>" +
                    "<h1>🚫 Blocked</h1>" +
                    "<p>This website is not in the allowed list for security reasons.</p>" +
                    "<div class='url'>" + blockedUrl + "</div>" +
                    "<button onclick='window.history.back()'>Go Back</button>" +
                    "</div>" +
                    "</body>" +
                    "</html>";
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
        }
    }

    private class CustomWebChromeClient extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                callback.invoke(origin, true, false);
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        101);
            }
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            request.grant(request.getResources());
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            progressBar.setProgress(newProgress);
            if (newProgress == 100) {
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            
            int tabIndex = webViews.indexOf(view);
            if (tabIndex != -1 && !view.getUrl().equals("file:///android_asset/main.html")) {
                tabTitles.set(tabIndex, title != null && !title.isEmpty() ? title : "New Tab");
                updateTabUI(tabIndex);
                
                if (tabIndex == currentTabIndex && getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(title);
                }
            }
        }
    }

    public class WebAppInterface {
        @android.webkit.JavascriptInterface
        public void openSite(String url) {
            handler.post(() -> {
                createNewTab(url);
            });
        }
        
        @android.webkit.JavascriptInterface
        public void showToast(String message) {
            handler.post(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.nav_home) {
            switchToTab(0); // Switch to home tab
        } else if (id == R.id.nav_new_tab) {
            createNewTab("https://www.google.com");
        } else if (id == R.id.nav_refresh) {
            if (currentWebView != null) {
                currentWebView.reload();
            }
        } else if (id == R.id.nav_downloads) {
            openDownloads();
        } else if (id == R.id.nav_settings) {
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        }
        
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void openDownloads() {
        Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack();
        } else if (currentTabIndex != 0) {
            switchToTab(0); // Switch to home tab
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        for (WebView webView : webViews) {
            webView.destroy();
        }
    }
}