package jnv.ron.browser;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.webkit.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrowserActivity extends AppCompatActivity {
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private Toolbar toolbar;
    private List<String> allowedDomains = new ArrayList<>();
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler();
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        webView = findViewById(R.id.webView);

        setupUI();
        loadAllowedDomains();
        setupWebView();

        String url = getIntent().getStringExtra("url");
        if (url != null && !url.isEmpty()) {
            loadUrl(url);
        } else {
            loadUrl("https://www.google.com");
        }
    }

    private void setupUI() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Loading...");
        }

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
        
        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
        });
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
                    Toast.makeText(this, "Loaded " + domains.size() + " allowed domains", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Using fallback domains", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void setupWebView() {
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
        
        // Cache settings (remove deprecated methods)
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        webView.setWebViewClient(new CustomWebViewClient());
        webView.setWebChromeClient(new CustomWebChromeClient());
        
        // Handle downloads
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            handleDownload(url, userAgent, contentDisposition, mimetype);
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
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
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
            swipeRefresh.setRefreshing(true);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Loading...");
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            swipeRefresh.setRefreshing(false);
            if (getSupportActionBar() != null) {
                String title = view.getTitle();
                getSupportActionBar().setTitle(title != null && !title.isEmpty() ? title : "Browser");
            }
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
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            if (newProgress < 100) {
                swipeRefresh.setRefreshing(true);
            } else {
                swipeRefresh.setRefreshing(false);
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