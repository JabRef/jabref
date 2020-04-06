package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches data from the INSPIRE database.
 *
 */
public class INSPIREFetcher implements SearchBasedParserFetcher {

    private static final String INSPIRE_HOST = "https://inspirehep.net/api/literature/";

    private final ImportFormatPreferences importFormatPreferences;

    public INSPIREFetcher(ImportFormatPreferences preferences) {
        this.importFormatPreferences = preferences;
    }

    @Override
    public String getName() {
        return "INSPIRE";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_INSPIRE);
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(INSPIRE_HOST);
        uriBuilder.addParameter("q", query); // Query
        return uriBuilder.build().toURL();
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        if (StringUtil.isBlank(query)) {
            return Collections.emptyList();
        }

        try {
            URLDownload download = new URLDownload(getURLForQuery(query));
            download.addHeader("Accept", MediaTypes.APPLICATION_BIBTEX);

            try (InputStream stream = download.asInputStream()) {
                List<BibEntry> fetchedEntries = getParser().parseEntries(stream);

                // Post-cleanup
                fetchedEntries.forEach(this::doPostCleanup);

                return fetchedEntries;
            }

        } catch (URISyntaxException e) {
            throw new FetcherException("Search URI is malformed", e);
        } catch (IOException e) {
            // TODO: Catch HTTP Response 401/403 errors and report that user has no rights to access resource
            throw new FetcherException("A network error occurred", e);
        } catch (ParseException e) {
            throw new FetcherException("An internal parser error occurred", e);
        }

    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // Remove strange "SLACcitation" field
        new FieldFormatterCleanup(new UnknownField("SLACcitation"), new ClearFormatter()).cleanup(entry);

        // Remove braces around content of "title" field
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveBracesFormatter()).cleanup(entry);
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
    }
}
