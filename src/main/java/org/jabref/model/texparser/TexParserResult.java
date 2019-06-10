package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;

public class TexParserResult {

    private final List<Path> fileList;
    private final List<Path> nestedFiles;
    private final Map<String, Set<Citation>> citations;

    public TexParserResult() {
        this.fileList = new ArrayList<>();
        this.nestedFiles = new ArrayList<>();
        this.citations = new HashMap<>();
    }

    public List<Path> getFileList() {
        return fileList;
    }

    public List<Path> getNestedFiles() {
        return nestedFiles;
    }

    public Map<String, Set<Citation>> getCitations() {
        return citations;
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
     * Add a citation to the citations map. It puts a new key into the map if does not exist yet.
     */
    public void addKey(String key, Path file, int lineNumber, int start, int end, String line) {
        Citation citation = new Citation(file, lineNumber, start, end, line);
        citations.computeIfAbsent(key, value -> new HashSet<>())
                 .add(citation);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("fileList = " + fileList)
                .add("nestedFiles = " + nestedFiles)
                .add("citations = " + citations)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TexParserResult that = (TexParserResult) o;

        return Objects.equals(fileList, that.fileList)
                && Objects.equals(nestedFiles, that.nestedFiles)
                && Objects.equals(citations, that.citations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileList, nestedFiles, citations);
    }
}
