package ron.jnv.browser;
import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.activity.result.contract.ActivityResultContracts;
import android.webkit.URLUtil;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
public class BrowserActivity extends AppCompatActivity {
    ViewPager2 pager;
    TabLayout tabs;
    WebPagerAdapter adapter;
    ArrayList<String> urls = new ArrayList<>();
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_browser);
        pager = findViewById(R.id.pager);
        tabs = findViewById(R.id.tabs);
        adapter = new WebPagerAdapter(this);
        pager.setAdapter(adapter);
        // initial open
        String url = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        if (url != null) addTab(title==null?url:title, url);
        new TabLayoutMediator(tabs, pager, (tab, position) -> tab.setText(adapter.getTitle(position))).attach();
    }
    void addTab(String title, String url){
        adapter.add(url, title);
        adapter.notifyDataSetChanged();
        pager.setCurrentItem(adapter.getItemCount()-1, true);
    }
    // download helper
    public void downloadFile(String url, String userAgent, String contentDisposition, String mimeType){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
                Toast.makeText(this, "Permission asked. Try download again.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(url));
        req.allowScanningByMediaScanner();
        req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        req.setTitle(fileName);
        req.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName);
        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        dm.enqueue(req);
        Toast.makeText(this, "Download started: " + fileName, Toast.LENGTH_SHORT).show();
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // no-op
    }
}
