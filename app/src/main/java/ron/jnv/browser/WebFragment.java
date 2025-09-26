package ron.jnv.browser;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
public class WebFragment extends Fragment {
    private static final String ARG_URL = "arg_url";
    WebView web;
    String url;
    public static WebFragment newInstance(String url){
        WebFragment f = new WebFragment();
        Bundle b = new Bundle();
        b.putString(ARG_URL, url);
        f.setArguments(b);
        return f;
    }
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        View v = inflater.inflate(R.layout.fragment_webview, container, false);
        web = v.findViewById(R.id.webview);
        if (getArguments()!=null) url = getArguments().getString(ARG_URL);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            if (getActivity() instanceof BrowserActivity){
                ((BrowserActivity)getActivity()).downloadFile(url, userAgent, contentDisposition, mimeType);
            }
        });
        web.loadUrl(url);
        return v;
    }
}
