package jnv.ron.browser;

import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.webkit.*;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private DrawerLayout drawerLayout;
    private LinearLayout tabsContainer, webViewsContainer;
    private ProgressBar progressBar;
    private WebView currentWebView;
    private ImageButton btnMenu, btnTabs, btnNewTab;
    private TextView txtTabCount;
    
    private List<WebView> webViews = new ArrayList<>();
    private List<String> tabTitles = new ArrayList<>();
    private List<String> tabUrls = new ArrayList<>();
    private List<String> allowedDomains = new ArrayList<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler();
    private static final int PERMISSION_REQUEST_CODE = 100;
    private int currentTabIndex = 0;
    private boolean tabsViewVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        loadAllowedDomains();
        createHomeTab();
    }

    private void initializeViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        tabsContainer = findViewById(R.id.tabsContainer);
        webViewsContainer = findViewById(R.id.webViewsContainer);
        progressBar = findViewById(R.id.progressBar);
        btnMenu = findViewById(R.id.btnMenu);
        btnTabs = findViewById(R.id.btnTabs);
        btnNewTab = findViewById(R.id.btnNewTab);
        txtTabCount = findViewById(R.id.txtTabCount);

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        btnTabs.setOnClickListener(v -> toggleTabsView());
        btnNewTab.setOnClickListener(v -> createNewTab("file:///android_asset/main.html"));
    }

    private void toggleTabsView() {
        tabsViewVisible = !tabsViewVisible;
        if (tabsViewVisible) {
            showTabsView();
        } else {
            hideTabsView();
        }
    }

    private void showTabsView() {
        tabsContainer.setVisibility(View.VISIBLE);
        webViewsContainer.setVisibility(View.GONE);
        updateTabsView();
    }

    private void hideTabsView() {
        tabsContainer.setVisibility(View.GONE);
        webViewsContainer.setVisibility(View.VISIBLE);
    }

    private void updateTabsView() {
        tabsContainer.removeAllViews();
        
        for (int i = 0; i < webViews.size(); i++) {
            View tabView = getLayoutInflater().inflate(R.layout.item_tab, tabsContainer, false);
            TextView tabTitle = tabView.findViewById(R.id.tabTitle);
            TextView tabUrl = tabView.findViewById(R.id.tabUrl);
            ImageButton btnClose = tabView.findViewById(R.id.btnClose);
            
            tabTitle.setText(tabTitles.get(i));
            tabUrl.setText(tabUrls.get(i));
            
            if (i == currentTabIndex) {
                tabView.setBackgroundColor(Color.parseColor("#E8F0FE"));
            }
            
            final int tabIndex = i;
            tabView.setOnClickListener(v -> {
                switchToTab(tabIndex);
                hideTabsView();
            });
            
            btnClose.setOnClickListener(v -> closeTab(tabIndex));
            
            tabsContainer.addView(tabView);
        }
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
                
                handler.post(() -> allowedDomains.clear());
                handler.post(() -> allowedDomains.addAll(domains));
            } catch (Exception e) {
                e.printStackTrace();
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
        updateTabCount();
    }

    private void createNewTab(String url) {
        WebView webView = new WebView(this);
        setupWebView(webView);
        
        webViews.add(webView);
        tabTitles.add("New Tab");
        tabUrls.add(url);
        
        webViewsContainer.addView(webView);
        
        int tabIndex = webViews.size() - 1;
        switchToTab(tabIndex);
        
        if (url.equals("file:///android_asset/main.html")) {
            tabTitles.set(tabIndex, "Home");
        } else {
            webView.loadUrl(url);
        }
        
        updateTabCount();
    }

    private void setupWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());
        
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            handleDownload(url, userAgent, contentDisposition, mimetype);
        });
        
        webView.addJavascriptInterface(new WebAppInterface(), "Android");
    }

    private void switchToTab(int tabIndex) {
        for (int i = 0; i < webViews.size(); i++) {
            webViews.get(i).setVisibility(i == tabIndex ? View.VISIBLE : View.GONE);
        }
        
        currentWebView = webViews.get(tabIndex);
        currentTabIndex = tabIndex;
        updateTabCount();
        
        if (tabsViewVisible) {
            updateTabsView();
        }
    }

    private void closeTab(int tabIndex) {
        if (webViews.size() <= 1) {
            Toast.makeText(this, "Cannot close the last tab", Toast.LENGTH_SHORT).show();
            return;
        }
        
        WebView webViewToRemove = webViews.get(tabIndex);
        webViewToRemove.destroy();
        webViews.remove(tabIndex);
        tabTitles.remove(tabIndex);
        tabUrls.remove(tabIndex);
        webViewsContainer.removeView(webViewToRemove);
        
        int newTabIndex = Math.min(tabIndex, webViews.size() - 1);
        switchToTab(newTabIndex);
        
        if (tabsViewVisible) {
            updateTabsView();
        }
        
        updateTabCount();
    }

    private void updateTabCount() {
        txtTabCount.setText(String.valueOf(webViews.size()));
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
            
            int tabIndex = webViews.indexOf(view);
            if (tabIndex != -1) {
                tabUrls.set(tabIndex, url);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
            
            int tabIndex = webViews.indexOf(view);
            if (tabIndex != -1) {
                String title = view.getTitle();
                if (title != null && !title.isEmpty() && !url.equals("file:///android_asset/main.html")) {
                    tabTitles.set(tabIndex, title);
                    if (tabsViewVisible) {
                        updateTabsView();
                    }
                }
            }
        }

        private boolean isUrlAllowed(String url) {
            if (url.startsWith("file:///android_asset/")) {
                return true;
            }
            
            try {
                String host = Uri.parse(url).getHost().toLowerCase();
                for (String domain : allowedDomains) {
                    if (host.equals(domain) || host.endsWith("." + domain)) {
                        return true;
                    }
                }
            } catch (Exception e) {
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
                if (tabsViewVisible) {
                    updateTabsView();
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
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (tabsViewVisible) {
            hideTabsView();
        } else if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack();
        } else if (currentTabIndex != 0) {
            switchToTab(0);
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