package org.jabref.logic.importer.fetcher.citation.opencitations;

import java.util.List;

public class CitationResponse {
    private List<CitationItem> citations;

    public CitationResponse() {
        this.citations = List.of();
    }

    public CitationResponse(List<CitationItem> citations) {
        this.citations = citations;
    }

    public List<CitationItem> getCitations() {
        return citations;
    }

    public void setCitations(List<CitationItem> citations) {
        this.citations = citations;
    }
}
