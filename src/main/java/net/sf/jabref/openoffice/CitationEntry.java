package net.sf.jabref.openoffice;

import java.util.Optional;

public class CitationEntry implements Comparable<CitationEntry> {

    private final String refMarkName;
    private Optional<String> pageInfo;
    private final String context;
    private final Optional<String> origPageInfo;


    // Only used for testing...
    public CitationEntry(String refMarkName, String context) {
        this(refMarkName, context, Optional.empty());
    }

    // Only used for testing...
    public CitationEntry(String refMarkName, String context, String pageInfo) {
        this(refMarkName, context, Optional.ofNullable(pageInfo));
    }

    public CitationEntry(String refMarkName, String context, Optional<String> pageInfo) {
        this.refMarkName = refMarkName;
        this.context = context;
        this.pageInfo = pageInfo;
        this.origPageInfo = pageInfo;
    }

    public Optional<String> getPageInfo() {
        return pageInfo;
    }

    public String getRefMarkName() {
        return refMarkName;
    }

    public boolean pageInfoChanged() {
        if (pageInfo.isPresent() ^ origPageInfo.isPresent()) {
            return true;
        }
        if (!pageInfo.isPresent()) {
            // This means that origPageInfo.isPresent is also false
            return false;
        } else {
            // So origPageInfo.isPresent is true here
            return pageInfo.get().compareTo(origPageInfo.get()) != 0;
        }
    }

    @Override
    public int compareTo(CitationEntry other) {
        return this.refMarkName.compareTo(other.refMarkName);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CitationEntry) {
            CitationEntry other = (CitationEntry) o;
            return this.refMarkName.equals(other.refMarkName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.refMarkName.hashCode();
    }

    public String getContext() {
        return context;
    }

    public void setPageInfo(String trim) {
        pageInfo = Optional.ofNullable(trim);
    }
}
