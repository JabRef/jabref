package net.sf.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStream;
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

import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.cleanup.MoveFieldCleanup;
import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.EntryBasedParserFetcher;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.ParserException;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches data from the SAO/NASA Astrophysics Data System (http://www.adsabs.harvard.edu/)
 *
 * Search query-based: http://adsabs.harvard.edu/basic_search.html
 * Entry -based: http://adsabs.harvard.edu/abstract_service.html
 *
 * There is also a new API (https://github.com/adsabs/adsabs-dev-api) but it returns JSON
 * (or at least needs multiple calls to get BibTeX, status: September 2016)
 */
public class MathSciNetLookup implements EntryBasedParserFetcher {

    private final ImportFormatPreferences preferences;

    public MathSciNetLookup(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "MathSciNet";
    }

    @Override
    public HelpFile getHelpPage() {
        return null;
    }

    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("http://www.ams.org/mrlookup");
        uriBuilder.addParameter("format", "bibtex");

        entry.getFieldOrAlias(FieldName.TITLE).ifPresent(title -> uriBuilder.addParameter("ti", title));
        entry.getFieldOrAlias(FieldName.AUTHOR).ifPresent(author -> uriBuilder.addParameter("au", author));
        entry.getFieldOrAlias(FieldName.JOURNAL).ifPresent(journal -> uriBuilder.addParameter("jrnl", journal));
        entry.getFieldOrAlias(FieldName.YEAR).ifPresent(year -> uriBuilder.addParameter("year", year));

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new Parser() {

            Pattern pattern = Pattern.compile("<pre>(?s)(.*)</pre>");

            @Override
            public List<BibEntry> parseEntries(InputStream inputStream) throws ParserException {
                String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(
                        Collectors.joining(OS.NEWLINE));

                List<BibEntry> entries = new ArrayList<>();
                BibtexParser bibtexParser = new BibtexParser(preferences);
                Matcher matcher = pattern.matcher(response);
                while (matcher.find()) {
                    String bibtexEntryString = matcher.group();
                    entries.addAll(bibtexParser.parseEntries(bibtexEntryString));
                }
                return entries;
            }
        };
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new MoveFieldCleanup("fjournal", FieldName.JOURNAL).cleanup(entry);
        new MoveFieldCleanup("mrclass", FieldName.KEYWORDS).cleanup(entry);
        new FieldFormatterCleanup(FieldName.URL, new ClearFormatter()).cleanup(entry);
    }
}
