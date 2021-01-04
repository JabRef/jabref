package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.MoveFieldCleanup;
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches data from the Zentralblatt Math (https://www.zbmath.org/)
 */
public class ZbMATH implements SearchBasedParserFetcher, IdBasedParserFetcher {

    private final ImportFormatPreferences preferences;

    public ZbMATH(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "zbMATH";
    }

    /**
     * TODO: Implement EntryBasedParserFetcher
     * We use the zbMATH Citation matcher (https://www.zbmath.org/citationmatching/)
     * instead of the usual search since this tool is optimized for finding a publication based on partial information.
     */
    /*
    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        // Example: https://zbmath.org/citationmatching/match?q=Ratiu
    }
    */
    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://zbmath.org/bibtexoutput/");
        uriBuilder.addParameter("q", query); // search all fields
        uriBuilder.addParameter("start", "0"); // start index
        uriBuilder.addParameter("count", "200"); // should return up to 200 items (instead of default 100)

        URLDownload.bypassSSLVerification();

        return uriBuilder.build().toURL();
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://zbmath.org/bibtexoutput/");
        String query = "an:".concat(identifier); // use an: to search for a zbMATH identifier
        uriBuilder.addParameter("q", query);
        uriBuilder.addParameter("start", "0"); // start index
        uriBuilder.addParameter("count", "1"); // return exactly one item

        URLDownload.bypassSSLVerification();

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(preferences, new DummyFileUpdateMonitor());
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new MoveFieldCleanup(new UnknownField("msc2010"), StandardField.KEYWORDS).cleanup(entry);
        new MoveFieldCleanup(new UnknownField("fjournal"), StandardField.JOURNAL).cleanup(entry);
        new FieldFormatterCleanup(StandardField.JOURNAL, new RemoveBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveBracesFormatter()).cleanup(entry);
    }
}
