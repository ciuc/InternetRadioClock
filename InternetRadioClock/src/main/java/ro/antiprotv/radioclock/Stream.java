package ro.antiprotv.radioclock;

class Stream {

    private String name;
    private String url;
    private String country;
    private String tags;
    private String language;

    public Stream(String name, String url, String country, String tags, String language) {
        this.name = name;
        this.url = url;
        this.country = country;
        this.tags = tags;
        this.language = language;
    }

    public String getLanguage() {
        return language;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getCountry() {
        return country;
    }

    public String getTags() {
        return tags;
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
