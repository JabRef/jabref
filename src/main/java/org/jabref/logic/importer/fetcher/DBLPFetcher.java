package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.cleanup.DoiCleanup;
import org.jabref.logic.formatter.bibtexfields.CleanupUrlFormatter;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches BibTeX data from DBLP (dblp.org)
 *
 * @see <a href="https://dblp.dagstuhl.de/faq/13501473">Basic API documentation</a>
 */
public class DBLPFetcher implements SearchBasedParserFetcher {

    private static final String BASIC_SEARCH_URL = "https://dblp.org/search/publ/api";

    private final ImportFormatPreferences importFormatPreferences;

    public DBLPFetcher(ImportFormatPreferences importFormatPreferences) {
        Objects.requireNonNull(importFormatPreferences);
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder(BASIC_SEARCH_URL);
        uriBuilder.addParameter("q", query);
        uriBuilder.addParameter("h", String.valueOf(100)); // number of hits
        uriBuilder.addParameter("c", String.valueOf(0)); // no need for auto-completion
        uriBuilder.addParameter("f", String.valueOf(0)); // "from", index of first hit to download
        uriBuilder.addParameter("format", "bib1");

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        DoiCleanup doiCleaner = new DoiCleanup();
        doiCleaner.cleanup(entry);

        FieldFormatterCleanup clearTimestampFormatter = new FieldFormatterCleanup(InternalField.TIMESTAMP, new ClearFormatter());
        clearTimestampFormatter.cleanup(entry);

        FieldFormatterCleanup cleanUpUrlFormatter = new FieldFormatterCleanup(StandardField.URL, new CleanupUrlFormatter());
        cleanUpUrlFormatter.cleanup(entry);
    }

    @Override
    public String getName() {
        return "DBLP";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_DBLP);
    }
}
