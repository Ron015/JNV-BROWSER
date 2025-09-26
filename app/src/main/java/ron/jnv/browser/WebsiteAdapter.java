package ron.jnv.browser;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

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
        private MaterialCardView cardView;
        private ImageView iconImageView;
        private TextView titleTextView;
        private TextView urlTextView;
        
        WebsiteViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            iconImageView = itemView.findViewById(R.id.iconImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            urlTextView = itemView.findViewById(R.id.urlTextView);
            
            cardView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onWebsiteClick(websites.get(getAdapterPosition()));
                }
            });
        }
        
        void bind(Website website) {
            titleTextView.setText(website.getTitle());
            urlTextView.setText(website.getUrl());
            
            // Load icon using Glide or set default
            if (website.getIcon() != null && !website.getIcon().isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(website.getIcon())
                    .placeholder(android.R.drawable.ic_menu_web)
                    .error(android.R.drawable.ic_menu_web)
                    .into(iconImageView);
            } else {
                iconImageView.setImageResource(android.R.drawable.ic_menu_web);
            }
        }
    }
    
    public interface WebsiteClickListener {
        void onWebsiteClick(Website website);
    }
}