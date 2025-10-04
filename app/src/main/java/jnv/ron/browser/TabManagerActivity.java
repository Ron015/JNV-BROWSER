package jnv.ron.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class TabManagerActivity extends AppCompatActivity {
    private RecyclerView tabsRecyclerView;
    private FloatingActionButton fabAddTab;
    private Toolbar toolbar;
    private ImageButton btnBack;
    
    private TabAdapter tabAdapter;
    private List<BrowserTab> tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab_manager);

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        tabsRecyclerView = findViewById(R.id.tabsRecyclerView);
        fabAddTab = findViewById(R.id.fabAddTab);
        btnBack = findViewById(R.id.btnBack);

        // Get tabs from BrowserActivity
        tabs = BrowserActivity.getActiveTabs();
        
        setupRecyclerView();
        setupClickListeners();

        // Add initial tab if empty
        if (tabs.isEmpty()) {
            addNewTab("file:///android_asset/main.html");
        }
    }

    private void setupRecyclerView() {
        tabAdapter = new TabAdapter();
        tabsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tabsRecyclerView.setAdapter(tabAdapter);
    }

    private void setupClickListeners() {
        fabAddTab.setOnClickListener(v -> addNewTab("file:///android_asset/main.html"));
        
        btnBack.setOnClickListener(v -> finish());
    }

    private void addNewTab(String url) {
        BrowserTab tab = new BrowserTab("tab_" + System.currentTimeMillis(), url, "New Tab");
        tabs.add(tab);
        tabAdapter.notifyItemInserted(tabs.size() - 1);
        openTab(tab);
    }

    private void openTab(BrowserTab tab) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra("url", tab.url);
        startActivity(intent);
        finish();
    }

    private void closeTab(int position) {
        if (position >= 0 && position < tabs.size()) {
            BrowserTab tab = tabs.get(position);
            BrowserActivity.removeTab(tab.id);
            tabs.remove(position);
            tabAdapter.notifyItemRemoved(position);
            
            if (tabs.isEmpty()) {
                addNewTab("https://www.google.com");
            }
        }
    }

    private void closeAllTabs() {
        BrowserActivity.clearAllTabs();
        tabs.clear();
        tabAdapter.notifyDataSetChanged();
        addNewTab("file:///android_asset/main.html");
    }

    private class TabAdapter extends RecyclerView.Adapter<TabAdapter.TabViewHolder> {
        @NonNull
        @Override
        public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab, parent, false);
            return new TabViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
            BrowserTab tab = tabs.get(position);
            holder.bind(tab);
        }

        @Override
        public int getItemCount() {
            return tabs.size();
        }

        class TabViewHolder extends RecyclerView.ViewHolder {
            private TextView tvTitle, tvUrl;
            private ImageButton btnClose;

            public TabViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvUrl = itemView.findViewById(R.id.tvUrl);
                btnClose = itemView.findViewById(R.id.btnClose);
            }

            public void bind(BrowserTab tab) {
                tvTitle.setText(tab.title);
                tvUrl.setText(tab.url);

                itemView.setOnClickListener(v -> openTab(tab));
                
                btnClose.setOnClickListener(v -> closeTab(getAdapterPosition()));
            }
        }
    }
}