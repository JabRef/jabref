package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.texparser.DefaultLatexParser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class LatexParserResults {
    private static final DefaultLatexParser LATEX_PARSER = new DefaultLatexParser();
    private final Map<Path, LatexParserResult> parsedTex;

    public LatexParserResults() {
        this.parsedTex = new HashMap<>();
    }

    public LatexParserResults(List<Path> latexFiles) {
        this();
        latexFiles.forEach(this::parse);
    }

    /**
     * Constructor for testing purposes
     */
    public LatexParserResults(Path... latexFiles) {
        this();
        parse(List.of(latexFiles));
    }

    /**
     * Constructor for testing purposes
     */
    public LatexParserResults(LatexParserResult... results) {
        this();
        List.of(results).forEach(result -> parsedTex.put(result.getPath(), result));
    }

    public void parse(Path latexFile) {
        Optional.ofNullable(LATEX_PARSER.parse(latexFile))
                .ifPresent(result -> parsedTex.put(latexFile, result));
    }

    public void parse(List<Path> latexFiles) {
        latexFiles.forEach(this::parse);
    }

    public Set<Path> getBibFiles() {
        Set<Path> bibFiles = new HashSet<>();
        parsedTex.values().forEach(result -> bibFiles.addAll(result.getBibFiles()));
        return bibFiles;
    }

    public Multimap<String, Citation> getCitations() {
        Multimap<String, Citation> citations = HashMultimap.create();
        parsedTex.forEach((path, result) -> citations.putAll(result.getCitations()));
        return citations;
    }

    public Collection<Citation> getCitationsByKey(String key) {
        Collection<Citation> citations = new ArrayList<>();
        parsedTex.values().forEach(result -> citations.addAll(result.getCitationsByKey(key)));
        return citations;
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

        return Objects.equals(parsedTex, that.parsedTex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parsedTex, LATEX_PARSER);
    }
}
