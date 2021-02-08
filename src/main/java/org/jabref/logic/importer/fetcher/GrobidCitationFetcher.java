package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidCitationFetcher implements SearchBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidCitationFetcher.class);

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
    private Optional<String> parseUsingGrobid(String plainText) throws RuntimeException {
        try {
            return Optional.of(grobidService.processCitation(plainText, GrobidService.ConsolidateCitations.WITH_METADATA));
        } catch (SocketTimeoutException e) {
            String msg = "Connection timed out.";
            LOGGER.debug(msg, e);
            throw new RuntimeException(msg, e);
        } catch (IOException e) {
            String msg = "Could not process citation. " + e.getMessage();
            LOGGER.debug(msg, e);
            return Optional.empty();
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
    public String getName() {
        return "GROBID";
    }

    @Override
    public List<BibEntry> performSearch(String searchQuery) throws FetcherException {
        List<BibEntry> collect;
        try {
            collect = Arrays.stream(searchQuery.split("\\r\\r+|\\n\\n+|\\r\\n(\\r\\n)+"))
                            .map(String::trim)
                            .filter(str -> !str.isBlank())
                            .map(this::parseUsingGrobid)
                            .flatMap(Optional::stream)
                            .map(this::parseBibToBibEntry)
                            .flatMap(Optional::stream)
                            .collect(Collectors.toList());
        } catch (RuntimeException e) {
            throw new FetcherException(e.getMessage(), e.getCause());
        }
        return collect;
    }

    /**
     * Not used
     */
    @Override
    public List<BibEntry> performSearch(QueryNode luceneQuery) throws FetcherException {
        return Collections.emptyList();
    }
}
