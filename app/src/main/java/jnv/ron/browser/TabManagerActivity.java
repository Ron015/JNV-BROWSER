package jnv.ron.browser;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import jnv.ron.browser.databinding.ActivityTabManagerBinding;

public class TabManagerActivity extends AppCompatActivity {
    private ActivityTabManagerBinding binding;
    private TabAdapter tabAdapter;
    private List<Tab> tabs = new ArrayList<>();
    private int tabCounter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTabManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupClickListeners();

        // Add initial tab if empty
        if (tabs.isEmpty()) {
            addNewTab("https://www.google.com");
        }
    }

    private void setupRecyclerView() {
        tabAdapter = new TabAdapter();
        binding.tabsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.tabsRecyclerView.setAdapter(tabAdapter);
    }

    private void setupClickListeners() {
        binding.fabAddTab.setOnClickListener(v -> addNewTab("https://www.google.com"));
        
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void addNewTab(String url) {
        Tab tab = new Tab("Tab " + tabCounter++, url, "New Tab");
        tabs.add(tab);
        tabAdapter.notifyItemInserted(tabs.size() - 1);
    }

    private void openTab(Tab tab) {
        Intent intent = new Intent(this, BrowserActivity.class);
        intent.putExtra("url", tab.url);
        startActivity(intent);
        finish();
    }

    private void closeTab(int position) {
        if (position >= 0 && position < tabs.size()) {
            tabs.remove(position);
            tabAdapter.notifyItemRemoved(position);
            
            if (tabs.isEmpty()) {
                addNewTab("https://www.google.com");
            }
        }
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
            Tab tab = tabs.get(position);
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

            public void bind(Tab tab) {
                tvTitle.setText(tab.title);
                tvUrl.setText(tab.url);

                itemView.setOnClickListener(v -> openTab(tab));
                
                btnClose.setOnClickListener(v -> closeTab(getAdapterPosition()));
            }
        }
    }

    private static class Tab {
        String id;
        String url;
        String title;

        Tab(String id, String url, String title) {
            this.id = id;
            this.url = url;
            this.title = title;
        }
    }
}