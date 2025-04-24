package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LatexParserResult {

    private final Path path;
    private final Multimap<String, Citation> citations;
    private final List<Path> nestedFiles;
    private final List<Path> bibFiles;

    public LatexParserResult(Path path) {
        this.path = path;
        this.citations = HashMultimap.create();
        this.nestedFiles = new ArrayList<>();
        this.bibFiles = new ArrayList<>();
    }

    public Path getPath() {
        return path;
    }

    public Multimap<String, Citation> getCitations() {
        return citations;
    }

    /**
     * Return a collection of citations using a string as key reference.
     */
    public Collection<Citation> getCitationsByKey(String key) {
        return citations.get(key);
    }

    /**
     * Add a citation to the citations multimap.
     */
    public void addKey(String key, Path path, int lineNumber, int start, int end, String line) {
        citations.put(key, new Citation(path, lineNumber, start, end, line));
    }

    public List<Path> getNestedFiles() {
        return nestedFiles;
    }

    public void addNestedFile(Path nestedFile) {
        nestedFiles.add(nestedFile);
    }

    public List<Path> getBibFiles() {
        return bibFiles;
    }

    public void addBibFile(Path bibFile) {
        bibFiles.add(bibFile);
    }

    @Override
    public String toString() {
        return "TexParserResult{path=%s, citations=%s, nestedFiles=%s, bibFiles=%s}".formatted(
                this.path,
                this.citations,
                this.nestedFiles,
                this.bibFiles);
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

        return Objects.equals(path, that.path)
                && Objects.equals(citations, that.citations)
                && Objects.equals(nestedFiles, that.nestedFiles)
                && Objects.equals(bibFiles, that.bibFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, citations, nestedFiles, bibFiles);
    }
}
