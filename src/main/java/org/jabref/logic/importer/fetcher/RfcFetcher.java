package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;

/*
 * https://datatracker.ietf.org
 * IETF (Internet Engineering Task Force) datatracker contains data about the documents,
 * working groups, meetings, agendas, minutes, presentations, and more, of the IETF.
 */
public class RfcFetcher implements IdBasedParserFetcher {

    private final static String DRAFT_PREFIX = "draft";
    private final ImportFormatPreferences importFormatPreferences;

    public RfcFetcher(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "RFC";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_RFC);
    }

    /**
     * Get the URL of the RFC resource according to the given identifier
     *
     * @param identifier the ID
     * @return the URL of the RFC resource
     */
    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {

        String prefixedIdentifier = identifier;
        // if not a "draft" version
        if (!identifier.toLowerCase().startsWith(DRAFT_PREFIX)) {
            // Add "rfc" prefix if user's search entry was numerical
            prefixedIdentifier = (!identifier.toLowerCase().startsWith("rfc")) ? "rfc" + prefixedIdentifier : prefixedIdentifier;
        }

        URIBuilder uriBuilder = new URIBuilder("https://datatracker.ietf.org/doc/" + prefixedIdentifier + "/bibtex/");

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
    }
}
