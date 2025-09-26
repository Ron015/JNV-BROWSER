package ron.jnv.browser;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
public class WebPagerAdapter extends FragmentStateAdapter {
    ArrayList<String> urls = new ArrayList<>();
    ArrayList<String> titles = new ArrayList<>();
    public WebPagerAdapter(@NonNull FragmentActivity fa){ super(fa); }
    @NonNull @Override public Fragment createFragment(int position){
        return WebFragment.newInstance(urls.get(position));
    }
    @Override public int getItemCount(){ return urls.size(); }
    public void add(String url, String title){ urls.add(url); titles.add(title); }
    public String getTitle(int pos){ return titles.get(pos); }
}
