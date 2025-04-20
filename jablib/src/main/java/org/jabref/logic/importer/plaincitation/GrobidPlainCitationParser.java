package org.jabref.logic.importer.plaincitation;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Optional;

import org.jabref.http.dto.SimpleHttpResponse;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.model.entry.BibEntry;

import org.jsoup.HttpStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidPlainCitationParser implements PlainCitationParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidPlainCitationParser.class);

    private final ImportFormatPreferences importFormatPreferences;
    private final GrobidService grobidService;

    public GrobidPlainCitationParser(GrobidPreferences grobidPreferences, ImportFormatPreferences importFormatPreferences) {
        this(importFormatPreferences, new GrobidService(grobidPreferences));
    }

    GrobidPlainCitationParser(ImportFormatPreferences importFormatPreferences, GrobidService grobidService) {
        this.importFormatPreferences = importFormatPreferences;
        this.grobidService = grobidService;
    }

    /**
     * Passes request to grobid server, using consolidateCitations option to improve result. Takes a while, since the
     * server has to look up the entry.
     *
     * @return A BibTeX string if extraction is successful
     */
    @Override
    public Optional<BibEntry> parsePlainCitation(String text) throws FetcherException {
        try {
            return grobidService.processCitation(text, importFormatPreferences, GrobidService.ConsolidateCitations.WITH_METADATA);
        } catch (HttpStatusException e) {
            LOGGER.debug("Could not connect to Grobid", e);
            throw new FetcherException("{grobid}", new SimpleHttpResponse(e));
        } catch (SocketTimeoutException e) {
            String msg = "Connection timed out.";
            LOGGER.debug(msg, e);
            throw new FetcherException(msg, e.getCause());
        } catch (IOException | ParseException e) {
            LOGGER.debug("Could not process citation", e);
            throw new FetcherException("Could not process citation", e);
        }
    }
}
