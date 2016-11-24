package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ParseException;
import net.sf.jabref.logic.importer.SearchBasedFetcher;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches BibTeX data from DBLP (dblp.org)
 *
 * @see <a href="http://dblp.dagstuhl.de/faq/13501473">Basic API documentation</a>
 */
public class DBLPFetcher implements SearchBasedFetcher {

    private static final String BASIC_SEARCH_URL = "http://www.dblp.org/search/api/";

    private final ImportFormatPreferences importFormatPreferences;

    public DBLPFetcher(ImportFormatPreferences importFormatPreferences) {
        Objects.requireNonNull(importFormatPreferences);

        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        try {
            URIBuilder uriBuilder = new URIBuilder(BASIC_SEARCH_URL);
            uriBuilder.addParameter("q", query);
            uriBuilder.addParameter("h", String.valueOf(1000)); // number of hits
            uriBuilder.addParameter("c", String.valueOf(0)); // no need for auto-completion
            uriBuilder.addParameter("f", String.valueOf(0)); // "from", index of first hit to download
            uriBuilder.addParameter("format", "bibtex"); // direct download of bibtex data; Caution: not officially documented!

            String content = URLDownload.createURLDownloadWithBrowserUserAgent(uriBuilder.build().toString())
                    .downloadToString(StandardCharsets.UTF_8);

            BibtexParser parser = new BibtexParser(importFormatPreferences);
            return parser.parseEntries(content);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new FetcherException("Error while generating fetch URL", Localization.lang("Error while generating fetch URL"), e);
        }  catch (ParseException e) {
            throw new FetcherException("Parsing error", Localization.lang("Parsing error"), e);
        } catch (IOException e) {
            throw new FetcherException("IOException while fetching from "+getName(), e);
        }
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
