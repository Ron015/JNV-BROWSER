package ron.jnv.browser;

import android.os.Parcel;
import android.os.Parcelable;

public class Website implements Parcelable {
    private String icon;
    private String title;
    private String url;
    private String category;
    private boolean isFavorite;
    private long lastVisited;
    private int visitCount;

    public Website() {}

    public Website(String icon, String title, String url, String category) {
        this.icon = icon;
        this.title = title;
        this.url = url;
        this.category = category;
        this.isFavorite = false;
        this.lastVisited = System.currentTimeMillis();
        this.visitCount = 0;
    }

    protected Website(Parcel in) {
        icon = in.readString();
        title = in.readString();
        url = in.readString();
        category = in.readString();
        isFavorite = in.readByte() != 0;
        lastVisited = in.readLong();
        visitCount = in.readInt();
    }

    public static final Creator<Website> CREATOR = new Creator<Website>() {
        @Override
        public Website createFromParcel(Parcel in) {
            return new Website(in);
        }

        @Override
        public Website[] newArray(int size) {
            return new Website[size];
        }
    };

    // Getters and setters
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
    
    public long getLastVisited() { return lastVisited; }
    public void setLastVisited(long lastVisited) { this.lastVisited = lastVisited; }
    
    public int getVisitCount() { return visitCount; }
    public void setVisitCount(int visitCount) { this.visitCount = visitCount; }
    
    public void incrementVisitCount() { this.visitCount++; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(icon);
        dest.writeString(title);
        dest.writeString(url);
        dest.writeString(category);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeLong(lastVisited);
        dest.writeInt(visitCount);
    }
}