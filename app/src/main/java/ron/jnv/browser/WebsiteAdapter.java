package ron.jnv.browser;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
public class WebsiteAdapter extends RecyclerView.Adapter<WebsiteAdapter.VH> {
    public interface OnItemClick { void onClick(Website w); }
    private List<Website> list;
    private OnItemClick listener;
    public WebsiteAdapter(List<Website> list, OnItemClick listener){
        this.list = list;
        this.listener = listener;
    }
    @Override public VH onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_website, parent, false);
        return new VH(v);
    }
    @Override public void onBindViewHolder(VH holder, int position){
        Website w = list.get(position);
        holder.title.setText(w.title);
        holder.itemView.setOnClickListener(v -> listener.onClick(w));
    }
    @Override public int getItemCount(){ return list.size(); }
    static class VH extends RecyclerView.ViewHolder {
        TextView title;
        ImageView icon;
        VH(View v){
            super(v);
            title = v.findViewById(R.id.w_title);
            icon = v.findViewById(R.id.w_icon);
        }
    }
}
