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

public class WebViewActivity extends AppCompatActivity {

    WebView webView;
    SwipeRefreshLayout swipeRefreshLayout;
    String url, allowedDOM;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        url = getIntent().getStringExtra("url");
        allowedDOM = getIntent().getStringExtra("allowedDOM");

        // Null safety
        if(url == null || url.isEmpty()) url = "file:///android_asset/blocked.html";
        if(allowedDOM == null || allowedDOM.isEmpty()) allowedDOM = "blocked.htm";

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
                String host;
                try {
                    host = new URI(request.getUrl().toString()).getHost();
                } catch (URISyntaxException e) {
                    host = null; // Invalid URL -> block
                }

                if(!isConnected()){
                    loadOfflinePage();
                    return true;
                }

                if(host != null && host.contains(allowedDOM)){
                    return false; // allow
                } else {
                    loadBlockedPage();
                    return true; // block
                }
            }
        });

        if(!isConnected()){
            loadOfflinePage();
        } else {
            webView.loadUrl(url);
        }
    }

    private boolean isConnected(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void loadBlockedPage(){
        webView.loadUrl("file:///android_asset/blocked.html");
    }

    private void loadOfflinePage(){
        webView.loadUrl("file:///android_asset/offline.html");
    }
}