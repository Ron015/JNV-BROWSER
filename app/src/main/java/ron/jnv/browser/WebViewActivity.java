package ron.jnv.browser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class WebViewActivity extends AppCompatActivity {

    WebView webView;
    SwipeRefreshLayout swipeRefreshLayout;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        url = getIntent().getStringExtra("url");

        // Null safety
        if(url == null || url.isEmpty()) url = "file:///android_asset/blocked.html";

        swipeRefreshLayout = new SwipeRefreshLayout(this);
        webView = new WebView(this);
        swipeRefreshLayout.addView(webView);

        swipeRefreshLayout.setOnRefreshListener(() -> webView.reload());
        setContentView(swipeRefreshLayout);

        webView.getSettings().setJavaScriptEnabled(true);

        webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon){
                swipeRefreshLayout.setRefreshing(true);
            }

            @Override
            public void onPageFinished(WebView view, String url){
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
                String requestUrl = request.getUrl().toString();
                String host;
                try {
                    host = new URI(requestUrl).getHost();
                } catch (URISyntaxException e) {
                    host = null; // Invalid URL -> block
                }

                if(!isConnected()){
                    loadOfflinePage();
                    return true;
                }

                if(host != null && isDomainAllowed(host)){
                    return false; // allow navigation
                } else {
                    loadBlockedPage();
                    return true; // block navigation
                }
            }
        });

        // Check if the initial URL is allowed before loading
        if(!isConnected()){
            loadOfflinePage();
        } else if (isUrlAllowed(url)) {
            webView.loadUrl(url);
        } else {
            loadBlockedPage();
        }
    }

    private boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private boolean isUrlAllowed(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            return host != null && isDomainAllowed(host);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private boolean isDomainAllowed(String host) {
        // Get the global allowed domains from MainActivity
        Set<String> allowedDomains = MainActivity.globalAllowedDomains;
        
        if (host == null) return false;
        
        host = host.toLowerCase();
        
        // Check if the host contains any allowed domain
        for (String allowedDomain : allowedDomains) {
            if (host.contains(allowedDomain)) {
                return true;
            }
        }
        return false;
    }

    private void loadBlockedPage(){
        webView.loadUrl("file:///android_asset/blocked.html");
    }

    private void loadOfflinePage(){
        webView.loadUrl("file:///android_asset/offline.html");
    }
}