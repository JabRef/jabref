package org.jabref.model.openoffice;

import java.util.Objects;
import java.util.Optional;

public class CitationEntry implements Comparable<CitationEntry> {

    private final String refMarkName;
    private final Optional<String> pageInfo;
    private final String context;

    public CitationEntry(String refMarkName, String context) {
        this(refMarkName, context, Optional.empty());
    }

    public CitationEntry(String refMarkName, String context, String pageInfo) {
        this(refMarkName, context, Optional.ofNullable(pageInfo));
    }

    public CitationEntry(String refMarkName, String context, Optional<String> pageInfo) {
        this.refMarkName = refMarkName;
        this.context = context;
        this.pageInfo = pageInfo;
    }

    public Optional<String> getPageInfo() {
        return pageInfo;
    }

    public String getRefMarkName() {
        return refMarkName;
    }

    @Override
    public int compareTo(CitationEntry other) {
        return this.refMarkName.compareTo(other.refMarkName);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof CitationEntry) {
            CitationEntry other = (CitationEntry) object;
            return Objects.equals(this.refMarkName, other.refMarkName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(refMarkName);
    }

    public String getContext() {
        return context;
    }
}
