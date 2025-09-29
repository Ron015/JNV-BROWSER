package ron.jnv.browser;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class WebsiteAdapter extends RecyclerView.Adapter<WebsiteAdapter.MyViewHolder> {

    Context context;
    ArrayList<WebsiteItem> list;

    public WebsiteAdapter(Context context, ArrayList<WebsiteItem> list){
        this.context = context;
        this.list = list;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View view = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position){
        WebsiteItem item = list.get(position);
        holder.text.setText(item.getTitle());
        Glide.with(context).load(item.getIcon()).into(holder.icon);

        holder.itemView.setOnClickListener(v -> {
    Intent intent = new Intent(context, WebViewActivity.class);
    intent.putExtra("url", item.getUrl() != null ? item.getUrl() : "https://google.com");
    intent.putExtra("allowedDOM", item.getAllowedDOM() != null ? item.getAllowedDOM() : "google.com");
    context.startActivity(intent);
});
    }

    @Override
    public int getItemCount(){
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView text;
        public MyViewHolder(View itemView){
            super(itemView);
            icon = itemView.findViewById(R.id.itemImage);
            text = itemView.findViewById(R.id.itemText);
        }
    }
}