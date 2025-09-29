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
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    WebsiteAdapter adapter;
    ArrayList<WebsiteItem> list;
    SwipeRefreshLayout swipeRefresh;

    String DATA_URL = "https://raw.githubusercontent.com/Ron015/API-EXAMPLE/main/allowed.json";

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

        swipeRefresh.setOnRefreshListener(this::loadData);

        loadData();
    }

    private void loadData(){
        swipeRefresh.setRefreshing(true);
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
                                String allowedDOM = obj.optString("allowedDOM", "blocked.html");

                                list.add(new WebsiteItem(title, url, icon, allowedDOM));
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
                    Toast.makeText(MainActivity.this,"Error loading data",Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false);
                    error.printStackTrace();
                });

        queue.add(request);
    }
}