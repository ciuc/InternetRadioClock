package ro.antiprotv.radioclock;

/** A station found by the Stream Finder. */
public class Stream {

  private final String name;
  private final String url;
  private final String country;
  private final String tags;
  private final String language;

  /** radio-browser's id for the station; empty when it is not known. */
  private final String stationUuid;

  private final String codec;

  /** In kbps; 0 when radio-browser does not know it. */
  private final int bitrate;

  public Stream(String name, String url, String country, String tags, String language) {
    this(name, url, country, tags, language, "", "", 0);
  }

  public Stream(
      String name,
      String url,
      String country,
      String tags,
      String language,
      String stationUuid,
      String codec,
      int bitrate) {
    this.name = name;
    this.url = url;
    this.country = country;
    this.tags = tags;
    this.language = language;
    this.stationUuid = stationUuid;
    this.codec = codec;
    this.bitrate = bitrate;
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

  public String getStationUuid() {
    return stationUuid;
  }

  public String getCodec() {
    return codec;
  }

  public int getBitrate() {
    return bitrate;
  }

  @Override
  public String toString() {
    return "Stream{"
        + "name='"
        + name
        + '\''
        + ", url='"
        + url
        + '\''
        + ", country='"
        + country
        + '\''
        + ", tags='"
        + tags
        + '\''
        + ", uuid='"
        + stationUuid
        + '\''
        + '}';
  }
}
