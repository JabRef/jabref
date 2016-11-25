package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import net.sf.jabref.logic.cleanup.DoiCleanup;
import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.SearchBasedParserFetcher;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.model.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches BibTeX data from DBLP (dblp.org)
 *
 * @see <a href="http://dblp.dagstuhl.de/faq/13501473">Basic API documentation</a>
 */
public class DBLPFetcher implements SearchBasedParserFetcher {

    private static final String BASIC_SEARCH_URL = "http://www.dblp.org/search/api/";

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
        return new BibtexParser(importFormatPreferences);
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        DoiCleanup doiCleaner = new DoiCleanup();

        FieldFormatterCleanup clearTimestampFormatter = new FieldFormatterCleanup(FieldName.TIMESTAMP,
                new ClearFormatter());

        doiCleaner.cleanup(entry);
        clearTimestampFormatter.cleanup(entry);

    }

    @Override
    public String getName() {
        return "DBLP";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DBLP;
    }

}
