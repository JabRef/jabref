package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.entry.BibEntry;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LatexParserResult {

    private final List<Path> fileList;
    private final List<Path> nestedFiles;
    private final Multimap<Path, Path> bibFiles;

    // BibTeXKey --> set of citations
    private final Multimap<String, Citation> citations;

    public LatexParserResult() {
        this.fileList = new ArrayList<>();
        this.nestedFiles = new ArrayList<>();
        this.bibFiles = HashMultimap.create();
        this.citations = HashMultimap.create();
    }

    public List<Path> getFileList() {
        return fileList;
    }

    public List<Path> getNestedFiles() {
        return nestedFiles;
    }

    public Multimap<Path, Path> getBibFiles() {
        return bibFiles;
    }

    public Multimap<String, Citation> getCitations() {
        return citations;
    }

    /**
     * Return a set of strings with the keys of the citations multimap.
     */
    public Set<String> getCitationsKeySet() {
        return citations.keySet();
    }

    /**
     * Return a collection of citations using a string as key reference.
     */
    public Collection<Citation> getCitationsByKey(String key) {
        return citations.get(key);
    }

    /**
     * Return a collection of citations using a BibEntry as reference.
     */
    public Collection<Citation> getCitationsByKey(BibEntry entry) {
        return entry.getCitationKey().map(this::getCitationsByKey).orElse(Collections.emptyList());
    }

    /**
     * Add a list of files to fileList or nestedFiles, depending on whether this is the first list.
     */
    public void addFiles(List<Path> texFiles) {
        if (fileList.isEmpty()) {
            fileList.addAll(texFiles);
        } else {
            nestedFiles.addAll(texFiles);
        }
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
        Citation citation = new Citation(file, lineNumber, start, end, line);
        citations.put(key, citation);
    }

    @Override
    public String toString() {
        return String.format("TexParserResult{fileList=%s, nestedFiles=%s, bibFiles=%s, citations=%s}",
                this.fileList,
                this.nestedFiles,
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

        return Objects.equals(fileList, that.fileList)
                && Objects.equals(nestedFiles, that.nestedFiles)
                && Objects.equals(bibFiles, that.bibFiles)
                && Objects.equals(citations, that.citations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileList, nestedFiles, bibFiles, citations);
    }
}
