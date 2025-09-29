package ron.jnv.browser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    WebsiteAdapter adapter;
    ArrayList<WebsiteItem> list;
    SwipeRefreshLayout swipeRefresh;

    String DATA_URL = "https://raw.githubusercontent.com/Ron015/API-EXAMPLE/main/allowed.json";
    String DOMAINS_URL = "https://raw.githubusercontent.com/Ron015/API-EXAMPLE/main/dom.txt";
    
    // Global allowed domains set
    public static Set<String> globalAllowedDomains = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        recyclerView = findViewById(R.id.recyclerView);

        list = new ArrayList<>();
        adapter = new WebsiteAdapter(this, list);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadAllData);

        // Load domains first, then websites
        loadAllData();
    }

    private void loadAllData() {
        swipeRefresh.setRefreshing(true);
        
        // First load domains, then load websites
        loadDomains(() -> loadWebsites());
    }

    private void loadDomains(Runnable onComplete) {
        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.GET, DOMAINS_URL,
                response -> {
                    try {
                        // Parse domains from text file (one domain per line)
                        String[] domainsArray = response.split("\n");
                        globalAllowedDomains.clear();
                        
                        for (String domain : domainsArray) {
                            String trimmedDomain = domain.trim();
                            if (!trimmedDomain.isEmpty()) {
                                globalAllowedDomains.add(trimmedDomain.toLowerCase());
                            }
                        }
                        
                        Toast.makeText(MainActivity.this, "Loaded " + globalAllowedDomains.size() + " allowed domains", Toast.LENGTH_SHORT).show();
                        onComplete.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Error parsing domains", Toast.LENGTH_SHORT).show();
                        onComplete.run(); // Still continue to load websites
                    }
                },
                error -> {
                    Toast.makeText(MainActivity.this, "Error loading domains", Toast.LENGTH_SHORT).show();
                    onComplete.run(); // Still continue to load websites even if domains fail
                    error.printStackTrace();
                });

        queue.add(request);
    }

    private void loadWebsites() {
        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, DATA_URL, null,
                response -> {
                    list.clear();
                    try {
                        for(int i = 0; i < response.length(); i++){
                            JSONObject obj = response.optJSONObject(i);
                            if(obj != null){
                                String title = obj.optString("title", "No Name");
                                String url = obj.optString("url", "file:///android_asset/blocked.html");
                                String icon = obj.optString("icon", "");

                                // Check if the website URL is allowed by our global domains
                                if (isUrlAllowed(url)) {
                                    list.add(new WebsiteItem(title, url, icon));
                                }
                                // If not allowed, simply skip it (don't add to list)
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this,"JSON Parsing Error",Toast.LENGTH_SHORT).show();
                    }
                    swipeRefresh.setRefreshing(false);
                },
                error -> {
                    Toast.makeText(MainActivity.this,"Error loading websites",Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                    error.printStackTrace();
                });

        queue.add(request);
    }

    private boolean isUrlAllowed(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            if (host == null) return false;
            
            host = host.toLowerCase();
            
            // Check if host contains any allowed domain
            for (String allowedDomain : globalAllowedDomains) {
                if (host.contains(allowedDomain)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}