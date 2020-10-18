package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

public class GrobidCitationFetcher implements SearchBasedFetcher {

    private static final String GROBID_URL = "http://grobid.jabref.org:8070";
    private ImportFormatPreferences importFormatPreferences;
    private GrobidService grobidService;

    public GrobidCitationFetcher(ImportFormatPreferences importFormatPreferences) {
        this(importFormatPreferences, new GrobidService(GROBID_URL));
    }

    GrobidCitationFetcher(ImportFormatPreferences importFormatPreferences, GrobidService grobidService) {
        this.importFormatPreferences = importFormatPreferences;
        this.grobidService = grobidService;
    }

    /**
     * Passes request to grobid server, using consolidateCitations option to improve result. Takes a while, since the
     * server has to look up the entry.
     *
     * @return A BibTeX string if extraction is successful
     */
    private Optional<String> parseUsingGrobid(String plainText) {
        try {
            return Optional.of(grobidService.processCitation(plainText, GrobidService.ConsolidateCitations.WITH_METADATA));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not process citation. " + e.getMessage(), e);
        }
    }

    private Optional<BibEntry> parseBibToBibEntry(String bibtexString) {
        try {
            return BibtexParser.singleFromString(bibtexString,
                    importFormatPreferences, new DummyFileUpdateMonitor());
        } catch (ParseException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<BibEntry> performSearch(String query) {
        return Arrays
                .stream(query.split("\\r\\r+|\\n\\n+|\\r\\n(\\r\\n)+"))
                .map(String::trim)
                .filter(str -> !str.isBlank())
                .map(reference -> parseUsingGrobid(reference))
                .flatMap(Optional::stream)
                .map(reference -> parseBibToBibEntry(reference))
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "GROBID";
    }

    public String getServerUrl() {
        return GROBID_URL;
    }
}
