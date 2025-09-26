package ron.jnv.browser;

import android.os.Bundle;
import android.webkit.WebView;
import androidx.fragment.app.Fragment;

public class BrowserTab extends Fragment {
    private WebView webView;
    private String currentUrl;
    private String tabTitle;
    private int tabId;

    public BrowserTab() {
        // Required empty public constructor
    }

    public static BrowserTab newInstance(int tabId, String initialUrl) {
        BrowserTab fragment = new BrowserTab();
        Bundle args = new Bundle();
        args.putInt("tabId", tabId);
        args.putString("initialUrl", initialUrl);
        fragment.setArguments(args);
        return fragment;
    }

    public WebView getWebView() {
        return webView;
    }

    public String getCurrentUrl() {
        return currentUrl;
    }

    public String getTabTitle() {
        return tabTitle != null ? tabTitle : "New Tab";
    }

    public int getTabId() {
        return tabId;
    }

    public void loadUrl(String url) {
        if (webView != null) {
            webView.loadUrl(url);
        }
    }

    public boolean canGoBack() {
        return webView != null && webView.canGoBack();
    }

    public void goBack() {
        if (webView != null) {
            webView.goBack();
        }
    }

    public boolean canGoForward() {
        return webView != null && webView.canGoForward();
    }

    public void goForward() {
        if (webView != null) {
            webView.goForward();
        }
    }

    public void reload() {
        if (webView != null) {
            webView.reload();
        }
    }
}