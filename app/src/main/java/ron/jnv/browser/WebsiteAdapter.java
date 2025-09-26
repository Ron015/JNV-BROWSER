package ron.jnv.browser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import java.util.List;

public class WebsiteAdapter extends RecyclerView.Adapter<WebsiteAdapter.WebsiteViewHolder> {
    
    private List<Website> websites;
    private WebsiteClickListener listener;
    
    public WebsiteAdapter(List<Website> websites, WebsiteClickListener listener) {
        this.websites = websites;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public WebsiteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_website_card, parent, false);
        return new WebsiteViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull WebsiteViewHolder holder, int position) {
        Website website = websites.get(position);
        holder.bind(website);
    }
    
    @Override
    public int getItemCount() {
        return websites.size();
    }
    
    public void updateWebsites(List<Website> newWebsites) {
        this.websites = newWebsites;
        notifyDataSetChanged();
    }
    
    class WebsiteViewHolder extends RecyclerView.ViewHolder {
        private CardView cardView;
        private ImageView iconImageView;
        private TextView titleTextView;
        private TextView urlTextView;
        private TextView categoryTextView;
        
        WebsiteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            urlTextView = itemView.findViewById(R.id.urlTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onWebsiteClick(websites.get(getAdapterPosition()));
                }
            });
            
            cardView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onWebsiteLongClick(websites.get(getAdapterPosition()));
                    return true;
                }
                return false;
            });
        }
        
        void bind(Website website) {
            titleTextView.setText(website.getTitle());
            urlTextView.setText(website.getUrl());
            categoryTextView.setText(website.getCategory());
            
            // Load icon using Glide
            if (website.getIcon() != null && !website.getIcon().isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(website.getIcon())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(iconImageView);
            } else {
                // Set default icon based on category
                int iconRes = getIconForCategory(website.getCategory());
                iconImageView.setImageResource(iconRes);
            }
            
            // Visual feedback for frequently visited sites
            if (website.getVisitCount() > 10) {
                cardView.setCardBackgroundColor(
                    itemView.getContext().getColor(R.color.colorPrimaryLight)
                );
            }
        }
        
        private int getIconForCategory(String category) {
            switch (category.toLowerCase()) {
                case "search": return R.drawable.ic_search;
                case "video": return R.drawable.ic_video;
                case "development": return R.drawable.ic_code;
                case "social": return R.drawable.ic_social;
                case "blogging": return R.drawable.ic_blog;
                default: return R.drawable.ic_web;
            }
        }
    }
    
    public interface WebsiteClickListener {
        void onWebsiteClick(Website website);
        void onWebsiteLongClick(Website website);
    }
}