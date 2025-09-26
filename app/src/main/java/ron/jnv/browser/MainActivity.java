package ron.jnv.browser;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity {
    RecyclerView rv;
    ProgressBar progress;
    ArrayList<Website> websites = new ArrayList<>();
    static final String JSON_URL = "https://raw.githubusercontent.com/Ron015/JNV-BROWSER/main/weballow.json";
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        rv = findViewById(R.id.rv);
        progress = findViewById(R.id.progress);
        rv.setLayoutManager(new LinearLayoutManager(this));
        fetchWebsites();
    }
    void fetchWebsites(){
        progress.setVisibility(View.VISIBLE);
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            try {
                URL url = new URL(JSON_URL);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setConnectTimeout(7000);
                con.setReadTimeout(7000);
                InputStream is = con.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                JSONArray arr = new JSONArray(sb.toString());
                websites.clear();
                for (int i=0;i<arr.length();i++){
                    JSONObject o = arr.getJSONObject(i);
                    websites.add(new Website(o.optString("icon"), o.optString("title"), o.optString("url")));
                }
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    WebsiteAdapter ad = new WebsiteAdapter(websites, w -> {
                        Intent it = new Intent(MainActivity.this, BrowserActivity.class);
                        it.putExtra("url", w.url);
                        it.putExtra("title", w.title);
                        startActivity(it);
                    });
                    rv.setAdapter(ad);
                });
            } catch (Exception e){
                e.printStackTrace();
                runOnUiThread(() -> progress.setVisibility(View.GONE));
            }
        });
    }
}
