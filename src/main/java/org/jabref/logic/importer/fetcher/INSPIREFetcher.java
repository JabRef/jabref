package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.util.OS;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches data from the INSPIRE database.
 *
 * @implNote We just use the normal search interface since it provides direct BibTeX export while the API (http://inspirehep.net/info/hep/api) currently only supports JSON and XML
 */
public class INSPIREFetcher implements SearchBasedParserFetcher {

    private static final String INSPIRE_HOST = "https://inspirehep.net/search";

    private final ImportFormatPreferences preferences;

    public INSPIREFetcher(ImportFormatPreferences preferences) {
        this.preferences = preferences;
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
        uriBuilder.addParameter("p", query); // Query
        //uriBuilder.addParameter("jrec", "1"); // Start index (not needed at the moment)
        uriBuilder.addParameter("rg", "100"); // Should return up to 100 items (instead of default 25)
        uriBuilder.addParameter("of", "hx"); // BibTeX format
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        // Inspire returns the BibTeX result embedded in HTML
        // So we extract the BibTeX string from the <pre>bibtex</pre> tags and pass the content to the BibTeX parser
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));

            List<BibEntry> entries = new ArrayList<>();
            BibtexParser bibtexParser = new BibtexParser(preferences, new DummyFileUpdateMonitor());
            Pattern pattern = Pattern.compile("<pre>(?s)(.*)</pre>");
            Matcher matcher = pattern.matcher(response);
            while (matcher.find()) {
                String bibtexEntryString = matcher.group(1);
                entries.addAll(bibtexParser.parseEntries(bibtexEntryString));
            }
            return entries;
        };
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // Remove strange "SLACcitation" field
        new FieldFormatterCleanup("SLACcitation", new ClearFormatter()).cleanup(entry);

        // Remove braces around content of "title" field
        new FieldFormatterCleanup(FieldName.TITLE, new RemoveBracesFormatter()).cleanup(entry);
    }
}
