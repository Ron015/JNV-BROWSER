package ron.jnv.browser;

public class WebsiteItem {
    String title, url, icon, allowedDOM;

    public WebsiteItem(String title, String url, String icon, String allowedDOM) {
        this.title = title;
        this.url = url;
        this.icon = icon;
        this.allowedDOM = allowedDOM;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getIcon() { return icon; }
    public String getAllowedDOM() { return allowedDOM; }
}