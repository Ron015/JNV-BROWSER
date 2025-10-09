package jnv.ron.browser;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.*;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.ActivityNotFoundException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrowserActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private List<String> allowedDomains = new ArrayList<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler();
    private static final int PERMISSION_REQUEST_CODE = 100;
    
    // Tab management
    private static List<BrowserTab> activeTabs = new ArrayList<>();
    private String currentTabId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        webView = findViewById(R.id.webView);

        setupUI();
        loadAllowedDomains();
        setupWebView();

        String url = getIntent().getStringExtra("url");
        if (url != null && !url.isEmpty()) {
            loadUrl(url);
            // Create new tab
            currentTabId = "tab_" + System.currentTimeMillis();
            activeTabs.add(new BrowserTab(currentTabId, url, "Loading..."));
        } else {
            loadUrl("file:///android_asset/main.html");
            currentTabId = "tab_" + System.currentTimeMillis();
            activeTabs.add(new BrowserTab(currentTabId, "file:///android_asset/main.html", "HOME"));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.browser_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.menu_tabs) {
            openTabManager();
            return true;
        } else if (id == R.id.menu_new_tab) {
            openNewTab();
            return true;
        } else if (id == R.id.menu_download) {
            openDownloadsFolder();
            return true;
        }
        
        
        return super.onOptionsItemSelected(item);
    }

    private void openTabManager() {
        Intent intent = new Intent(this, TabManagerActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
    private void openDownloadsFolder() {
        try {
            Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Fallback: open generic file explorer if no download app found
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType("resource/folder");
            intent.setData(Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toURI().toString()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, "No file manager found 📁", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openNewTab() {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra("url", "file:///android_asset/main.html");
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    // Update current tab info
    private void updateCurrentTab(String url, String title) {
        for (BrowserTab tab : activeTabs) {
            if (tab.id.equals(currentTabId)) {
                tab.url = url;
                tab.title = title != null ? title : "New Tab";
                break;
            }
        }
    }

    // Getters for TabManagerActivity
    public static List<BrowserTab> getActiveTabs() {
        return new ArrayList<>(activeTabs);
    }

    public static void removeTab(String tabId) {
        for (int i = 0; i < activeTabs.size(); i++) {
            if (activeTabs.get(i).id.equals(tabId)) {
                activeTabs.remove(i);
                break;
            }
        }
    }

    public static void clearAllTabs() {
        activeTabs.clear();
    }

    private void setupUI() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Loading...");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        // Setup progress bar
        progressBar.setMax(100);
        progressBar.setProgress(0);
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
                    Toast.makeText(this, "Using fallback domains", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
    
        // ✅ Core Web Features
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
    
        // ✅ Advanced Display & Interaction
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
    
        // ✅ Permissions & Access
        webSettings.setGeolocationEnabled(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
    
        // ✅ Cache & Performance
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setSaveFormData(true);
        webSettings.setSavePassword(false); // 🔒 deprecated
    
        // ✅ Cookie Support
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
    
        // ✅ Enable Web Debugging (for devs)
        WebView.setWebContentsDebuggingEnabled(true);
    
        // ✅ Custom Clients
        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());
    
        // ✅ File uploads, camera, mic, etc.
        webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
    
        // ✅ Downloads
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            handleDownload(url, userAgent, contentDisposition, mimeType);
        });
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

    private void loadUrl(String url) {
        webView.loadUrl(url);
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (isUrlAllowed(url)) {
                return false;
            } else {
                showBlockedPage(url);
                return true;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // Show progress bar when page starts loading
            progressBar.setVisibility(android.view.View.VISIBLE);
            progressBar.setProgress(10); // Start with 10% progress
            
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Loading...");
            }
            updateCurrentTab(url, "Loading...");
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            // Hide progress bar when page finishes loading
            progressBar.setVisibility(android.view.View.GONE);
            
            String title = view.getTitle();
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title != null && !title.isEmpty() ? title : "Browser");
            }
            updateCurrentTab(url, title);
        }

        private boolean isUrlAllowed(String url) {
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

        private void showBlockedPage(String blockedUrl) {
            String html = "<!DOCTYPE html>" +
                    "<html>" +
                    "<head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
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
                    "<p>This website is not in the allowed list for security reasons.<br>If you want to unblock this website then contact to Satendra Sir</p>" +
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
            if (ContextCompat.checkSelfPermission(BrowserActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                callback.invoke(origin, true, false);
            } else {
                ActivityCompat.requestPermissions(BrowserActivity.this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        101);
            }
        }

        @Override
        public void onPermissionRequest(PermissionRequest request) {
            // Grant all permissions for camera, microphone, etc.
            request.grant(request.getResources());
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title != null && !title.isEmpty() ? title : "Browser");
            }
            updateCurrentTab(view.getUrl(), title);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            
            // Update horizontal progress bar
            if (newProgress < 100) {
                progressBar.setVisibility(android.view.View.VISIBLE);
                progressBar.setProgress(newProgress);
                
                // Smooth animation for progress
                if (newProgress > 80) {
                    progressBar.setProgress(95); // Slow down near completion
                }
            } else {
                // When loading completes, hide progress bar with a slight delay
                handler.postDelayed(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    progressBar.setProgress(0);
                }, 200);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (webView != null) {
            webView.destroy();
        }
    }
}

// Tab data class
class BrowserTab {
    String id;
    String url;
    String title;

    BrowserTab(String id, String url, String title) {
        this.id = id;
        this.url = url;
        this.title = title;
    }
}