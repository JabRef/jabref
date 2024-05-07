package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LatexParserResults {
    private final Map<Path, LatexParserResult> parsedTexFiles;

    public LatexParserResults() {
        this.parsedTexFiles = new HashMap<>();
    }

    @VisibleForTesting
    public LatexParserResults(LatexParserResult... parsedFiles) {
        this();
        for (LatexParserResult parsedFile : parsedFiles) {
            add(parsedFile.getPath(), parsedFile);
        }
    }

    public void add(Path texFile, LatexParserResult parsedFile) {
        parsedTexFiles.put(texFile, parsedFile);
    }

    public LatexParserResult remove(Path texFile) {
        return parsedTexFiles.remove(texFile);
    }

    public Set<Path> getBibFiles() {
        Set<Path> bibFiles = new HashSet<>();
        parsedTexFiles.values().forEach(result -> bibFiles.addAll(result.getBibFiles()));
        return bibFiles;
    }

    public Multimap<String, Citation> getCitations() {
        Multimap<String, Citation> citations = HashMultimap.create();
        parsedTexFiles.forEach((path, result) -> citations.putAll(result.getCitations()));
        return citations;
    }

    public Collection<Citation> getCitationsByKey(String key) {
        Collection<Citation> citations = new ArrayList<>();
        parsedTexFiles.values().forEach(result -> citations.addAll(result.getCitationsByKey(key)));
        return citations;
    }

    public void clear() {
        parsedTexFiles.clear();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        LatexParserResults that = (LatexParserResults) obj;

        return Objects.equals(parsedTexFiles, that.parsedTexFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parsedTexFiles);
    }
}
