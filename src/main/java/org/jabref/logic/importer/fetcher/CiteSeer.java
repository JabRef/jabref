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

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.Formatter;
import org.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import org.jabref.logic.formatter.casechanger.TitleCaseFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.CoinsParser;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;

public class CiteSeer implements SearchBasedParserFetcher {

    public CiteSeer() {
    }

    @Override
    public String getName() {
        return "CiteSeerX";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_CITESEERX);
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://citeseer.ist.psu.edu/search");
        uriBuilder.addParameter("sort", "rlv"); // Sort by relevance
        uriBuilder.addParameter("q", new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse("")); // Query
        uriBuilder.addParameter("t", "doc"); // Type: documents
        // uriBuilder.addParameter("start", "0"); // Start index (not supported at the moment)
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        // MathSciNet returns COinS result embedded in HTML
        // So we extract the data string from the <span class="Z3988" title="<data>"></span> tags and pass the content to the COinS parser
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            List<BibEntry> entries = new ArrayList<>();
            CoinsParser parser = new CoinsParser();
            Pattern pattern = Pattern.compile("<span class=\"Z3988\" title=\"(.*)\"></span>");
            Matcher matcher = pattern.matcher(response);
            while (matcher.find()) {
                String encodedDataString = matcher.group(1);
                entries.addAll(parser.parseEntries(encodedDataString));
            }
            return entries;
        };
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // CiteSeer escapes some characters in a way that is not recognized by the normal html to unicode formatter
        // We, of course, also want to convert these special characters
        Formatter extendedHtmlFormatter = new HtmlToUnicodeFormatter() {
            @Override
            public String format(String fieldText) {
                String formatted = super.format(fieldText);
                formatted = formatted.replaceAll("%3A", ":");
                formatted = formatted.replaceAll("%3Cem%3", "");
                formatted = formatted.replaceAll("%3C%2Fem%3E", "");
                formatted = formatted.replaceAll("%2C\\+", " ");
                formatted = formatted.replaceAll("\\+", " ");
                return formatted;
            }
        };
        new FieldFormatterCleanup(InternalField.INTERNAL_ALL_FIELD, extendedHtmlFormatter).cleanup(entry);

        // Many titles in the CiteSeer database have all-capital titles, for convenience we convert them to title case
        new FieldFormatterCleanup(StandardField.TITLE, new TitleCaseFormatter()).cleanup(entry);
    }
}
