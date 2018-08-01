package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.cleanup.DoiCleanup;
import org.jabref.logic.cleanup.MoveFieldCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.importer.EntryBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
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
 * Fetches data from the MathSciNet (http://www.ams.org/mathscinet)
 */
public class MathSciNet implements SearchBasedParserFetcher, EntryBasedParserFetcher, IdBasedParserFetcher {

    private final ImportFormatPreferences preferences;

    public MathSciNet(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "MathSciNet";
    }

    /**
     * We use MR Lookup (http://www.ams.org/mrlookup) instead of the usual search since this tool is also available
     * without subscription and, moreover, is optimized for finding a publication based on partial information.
     */
    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://mathscinet.ams.org/mrlookup");
        uriBuilder.addParameter("format", "bibtex");

        entry.getFieldOrAlias(FieldName.TITLE).ifPresent(title -> uriBuilder.addParameter("ti", title));
        entry.getFieldOrAlias(FieldName.AUTHOR).ifPresent(author -> uriBuilder.addParameter("au", author));
        entry.getFieldOrAlias(FieldName.JOURNAL).ifPresent(journal -> uriBuilder.addParameter("jrnl", journal));
        entry.getFieldOrAlias(FieldName.YEAR).ifPresent(year -> uriBuilder.addParameter("year", year));

        return uriBuilder.build().toURL();
    }

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://mathscinet.ams.org/mathscinet/search/publications.html");
        uriBuilder.addParameter("pg7", "ALLF"); // search all fields
        uriBuilder.addParameter("s7", query); // query
        uriBuilder.addParameter("r", "1"); // start index
        uriBuilder.addParameter("extend", "1"); // should return up to 100 items (instead of default 10)
        uriBuilder.addParameter("fmt", "bibtex"); // BibTeX format
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://mathscinet.ams.org/mathscinet/search/publications.html");
        uriBuilder.addParameter("pg1", "MR"); // search MR number
        uriBuilder.addParameter("s1", identifier); // identifier
        uriBuilder.addParameter("fmt", "bibtex"); // BibTeX format
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        // MathSciNet returns the BibTeX result embedded in HTML
        // So we extract the BibTeX string from the <pre>bibtex</pre> tags and pass the content to the BibTeX parser
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));

            List<BibEntry> entries = new ArrayList<>();
            BibtexParser bibtexParser = new BibtexParser(preferences, new DummyFileUpdateMonitor());
            Pattern pattern = Pattern.compile("<pre>(?s)(.*)</pre>");
            Matcher matcher = pattern.matcher(response);
            while (matcher.find()) {
                String bibtexEntryString = matcher.group();
                entries.addAll(bibtexParser.parseEntries(bibtexEntryString));
            }
            return entries;
        };
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new MoveFieldCleanup("fjournal", FieldName.JOURNAL).cleanup(entry);
        new MoveFieldCleanup("mrclass", FieldName.KEYWORDS).cleanup(entry);
        new FieldFormatterCleanup("mrreviewer", new ClearFormatter()).cleanup(entry);
        new DoiCleanup().cleanup(entry);
        new FieldFormatterCleanup(FieldName.URL, new ClearFormatter()).cleanup(entry);

        // Remove comments: MathSciNet prepends a <pre> html tag
        entry.setCommentsBeforeEntry("");
    }
}
