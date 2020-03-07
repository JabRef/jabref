package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.entry.BibEntry;

import com.google.common.collect.Multimap;

public class LatexBibEntriesResolverResult {

    private final LatexParserResult latexParserResult;
    private final Set<BibEntry> newEntries;

    public LatexBibEntriesResolverResult(LatexParserResult latexParserResult) {
        this.latexParserResult = latexParserResult;
        this.newEntries = new HashSet<>();
    }

    public LatexParserResult getLatexParserResult() {
        return latexParserResult;
    }

    public Set<BibEntry> getNewEntries() {
        return newEntries;
    }

    public void addEntry(BibEntry entry) {
        newEntries.add(entry);
    }

    /**
     * @return the BIB files multimap from the LatexParserResult object.
     */
    public Multimap<Path, Path> getBibFiles() {
        return latexParserResult.getBibFiles();
    }

    /**
     * @return the citations multimap from the LatexParserResult object.
     */
    public Multimap<String, Citation> getCitations() {
        return latexParserResult.getCitations();
    }

    @Override
    public String toString() {
        return String.format("TexBibEntriesResolverResult{texParserResult=%s, newEntries=%s}",
                this.latexParserResult,
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

        return Objects.equals(latexParserResult, that.latexParserResult)
                && Objects.equals(newEntries, that.newEntries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latexParserResult, newEntries);
    }
}
