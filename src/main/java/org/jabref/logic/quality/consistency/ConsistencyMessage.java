package org.jabref.logic.quality.consistency;

public class ConsistencyMessage {
    private String entrytype;
    private String citationkey;
    private String address;
    private String booktitle;
    private String crossref;
    private String doi;
    private String pages;
    private String ranking;
    private String series;
    private String url;
    private String urldate;
    private String volume;
    private String year;

    public ConsistencyMessage(String entrytype, String citationkey, String address, String booktitle, String crossref, String doi,
                              String pages, String ranking, String series, String url, String urldate, String volume, String year) {
        this.entrytype = entrytype;
        this.citationkey = citationkey;
        this.address = address;
        this.booktitle = booktitle;
        this.crossref = crossref;
        this.doi = doi;
        this.pages = pages;
        this.ranking = ranking;
        this.series = series;
        this.url = url;
        this.urldate = urldate;
        this.volume = volume;
        this.year = year;
    }

    public String getEntrytype() {
        return entrytype;
    }

    public void setEntrytype(String entryType) {
        this.entrytype = entryType;
    }

    public String getCitationkey() {
        return citationkey;
    }

    public void setCitationkey(String citationkey) {
        this.citationkey = citationkey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBooktitle() {
        return booktitle;
    }

    public void setBooktitle(String booktitle) {
        this.booktitle = booktitle;
    }

    public String getCrossref() {
        return crossref;
    }

    public void setCrossref(String crossref) {
        this.crossref = crossref;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getRanking() {
        return ranking;
    }

    public void setRanking(String ranking) {
        this.ranking = ranking;
    }

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrldate() {
        return urldate;
    }

    public void setUrldate(String urldate) {
        this.urldate = urldate;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
