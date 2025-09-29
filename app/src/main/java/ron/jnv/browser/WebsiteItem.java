package ron.jnv.browser;

public class WebsiteItem {
    String title, url, icon;

    public WebsiteItem(String title, String url, String icon) {
        this.title = title;
        this.url = url;
        this.icon = icon;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getIcon() { return icon; }
}