package ron.jnv.browser;

public class Website {
    private String icon;
    private String title;
    private String url;

    public Website() {}

    public Website(String icon, String title, String url) {
        this.icon = icon;
        this.title = title;
        this.url = url;
    }

    public String getIcon() { 
        return icon; 
    }
    
    public void setIcon(String icon) { 
        this.icon = icon; 
    }
    
    public String getTitle() { 
        return title; 
    }
    
    public void setTitle(String title) { 
        this.title = title; 
    }
    
    public String getUrl() { 
        return url; 
    }
    
    public void setUrl(String url) { 
        this.url = url; 
    }
}