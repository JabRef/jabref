package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LatexParserResult {
    private final Multimap<Path, Path> bibFiles;
    private final Multimap<Path, Citation> citations;

    public LatexParserResult() {
        this.bibFiles = HashMultimap.create();
        this.citations = HashMultimap.create();
    }

    public Multimap<Path, Path> getBibFiles() {
        return bibFiles;
    }

    public Multimap<Path, Citation> getCitations() {
        return citations;
    }

    /**
     * Return a collection of citations using a string as key reference.
     */
    public Collection<Citation> getCitationsByKey(String key) {
        return citations.values()
                        .stream()
                        .filter(citation -> citation.key().equals(key))
                        .toList();
    }

    /**
     * Add a bibliography file to the BIB files set.
     */
    public void addBibFile(Path file, Path bibFile) {
        bibFiles.put(file, bibFile);
    }

    /**
     * Add a citation to the citations multimap.
     */
    public void addKey(String key, Path file, int lineNumber, int start, int end, String line) {
        Citation citation = new Citation(key, file, lineNumber, start, end, line);
        citations.put(file, citation);
    }

    @Override
    public String toString() {
        return String.format("TexParserResult{bibFiles=%s, citations=%s}",
                this.bibFiles,
                this.citations);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        LatexParserResult that = (LatexParserResult) obj;

        return Objects.equals(bibFiles, that.bibFiles)
                && Objects.equals(citations, that.citations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bibFiles, citations);
    }
}
