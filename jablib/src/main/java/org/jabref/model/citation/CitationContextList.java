package org.jabref.model.citation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CitationContextList {

    private final List<CitationContext> contexts;

    public CitationContextList(String sourceCitationKey) {
        this.contexts = new ArrayList<>();
    }

    public void add(CitationContext context) {
        contexts.add(context);
    }

    public void addAll(List<CitationContext> newContexts) {
        contexts.addAll(newContexts);
    }

    public List<CitationContext> getContexts() {
        return Collections.unmodifiableList(contexts);
    }

    public int size() {
        return contexts.size();
    }

    public boolean isEmpty() {
        return contexts.isEmpty();
    }

    public List<CitationContext> findByMarker(String marker) {
        String normalizedSearch = marker.replaceAll("[\\[\\](){}]", "").trim().toLowerCase();
        return contexts.stream()
                .filter(ctx -> ctx.getNormalizedMarker().toLowerCase().contains(normalizedSearch))
                .toList();
    }

    public List<CitationContext> findByAuthor(String authorName) {
        String lowerAuthor = authorName.toLowerCase();
        return contexts.stream()
                .filter(ctx -> ctx.getNormalizedMarker().toLowerCase().contains(lowerAuthor))
                .toList();
    }

    public List<CitationContext> findByPage(int page) {
        return contexts.stream()
                .filter(ctx -> ctx.pageNumber().isPresent() && ctx.pageNumber().get() == page)
                .toList();
    }

    public List<String> getUniqueCitationMarkers() {
        return contexts.stream()
                .map(CitationContext::citationMarker)
                .distinct()
                .toList();
    }
}
