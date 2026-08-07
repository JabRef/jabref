package org.jabref.model.texparser;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.entry.BibEntry;

import com.google.common.collect.Multimap;

public class LatexBibEntriesResolverResult {

    private final LatexParserResults latexParserResults;
    private final Set<BibEntry> newEntries;

    public LatexBibEntriesResolverResult(LatexParserResults latexParserResults) {
        this.latexParserResults = latexParserResults;
        this.newEntries = new HashSet<>();
    }

    public Set<BibEntry> getNewEntries() {
        return newEntries;
    }

    public void addEntry(BibEntry entry) {
        newEntries.add(entry);
    }

    public Multimap<String, Citation> getCitations() {
        return latexParserResults.getCitations();
    }

    @Override
    public String toString() {
        return "TexBibEntriesResolverResult{latexParserResults=%s, newEntries=%s}".formatted(
                this.latexParserResults,
                this.newEntries);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        LatexBibEntriesResolverResult that = (LatexBibEntriesResolverResult) obj;

        return Objects.equals(latexParserResults, that.latexParserResults)
                && Objects.equals(newEntries, that.newEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latexParserResults, newEntries);
    }
}
