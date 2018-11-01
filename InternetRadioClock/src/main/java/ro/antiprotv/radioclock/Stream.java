package ro.antiprotv.radioclock;

public class Stream {

    private String name;
    private String url;
    private String country;
    private String tags;

    public Stream(String name, String url, String country, String tags) {
        this.name = name;
        this.url = url;
        this.country = country;
        this.tags = tags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        return "Stream{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", country='" + country + '\'' +
                ", tags='" + tags + '\'' +
                '}';
    }
}
