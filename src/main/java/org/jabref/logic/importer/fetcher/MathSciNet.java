package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.cleanup.DoiCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.MoveFieldCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.importer.EntryBasedParserFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.util.DummyFileUpdateMonitor;

import kong.unirest.JsonNode;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jbibtex.TokenMgrException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches data from the MathSciNet (http://www.ams.org/mathscinet)
 */
public class MathSciNet implements SearchBasedParserFetcher, EntryBasedParserFetcher, IdBasedParserFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MathSciNet.class);
    private final ImportFormatPreferences preferences;

    public MathSciNet(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "MathSciNet";
    }

    /**
     * We use MR Lookup (https://mathscinet.ams.org/mathscinet/freetools/mrlookup) instead of the usual search since this tool is also available
     * without subscription and, moreover, is optimized for finding a publication based on partial information.
     */
    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        Optional<String> mrNumberInEntry = entry.getField(StandardField.MR_NUMBER);
        if (mrNumberInEntry.isPresent()) {
            // We are lucky and already know the id, so use it instead
            return getUrlForIdentifier(mrNumberInEntry.get());
        }

        URIBuilder uriBuilder = new URIBuilder("https://mathscinet.ams.org/mathscinet/api/freetools/mrlookup");

        uriBuilder.addParameter("author", entry.getFieldOrAlias(StandardField.AUTHOR).orElse(""));
        uriBuilder.addParameter("title", entry.getFieldOrAlias(StandardField.TITLE).orElse(""));
        uriBuilder.addParameter("journal", entry.getFieldOrAlias(StandardField.JOURNAL).orElse(""));
        uriBuilder.addParameter("year", entry.getFieldOrAlias(StandardField.YEAR).orElse(""));
        uriBuilder.addParameter("firstPage", "");
        uriBuilder.addParameter("lastPage", "");

        return uriBuilder.build().toURL();
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://mathscinet.ams.org/mathscinet/api/publications/search");
        uriBuilder.addParameter("query", new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse("")); // query
        uriBuilder.addParameter("currentPage", "1"); // start index
        uriBuilder.addParameter("pageSize", "100"); // page size
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://mathscinet.ams.org/mathscinet/api/publications/format");
        uriBuilder.addParameter("formats", "bib");
        uriBuilder.addParameter("ids", identifier); // identifier

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
            BibtexParser bibtexParser = new BibtexParser(preferences, new DummyFileUpdateMonitor());

            List<BibEntry> entries = new ArrayList<>();
            try {
                // Depending on the type of query we might get either a json object or directly a json array
                JsonNode node = new JsonNode(response);
                if (node.isArray()) {
                    JSONArray entriesArray = node.getArray();
                    for (int i = 0; i < entriesArray.length(); i++) {
                        String bibTexFormat = entriesArray.getJSONObject(i).getString("bib");
                        entries.addAll(bibtexParser.parseEntries(bibTexFormat));
                    }
                } else {
                    var element = node.getObject();
                    JSONArray entriesArray = element.getJSONObject("all").getJSONArray("results");
                    for (int i = 0; i < entriesArray.length(); i++) {
                        String bibTexFormat = entriesArray.getJSONObject(i).getString("bibTexFormat");
                        entries.addAll(bibtexParser.parseEntries(bibTexFormat));
                    }
                }
            } catch (JSONException | TokenMgrException e) {
                LOGGER.error("An error occurred while parsing fetched data", e);
                throw new ParseException("Error when parsing entry", e);
            }
            return entries;
        };
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new MoveFieldCleanup(AMSField.FJOURNAL, StandardField.JOURNAL).cleanup(entry);
        new MoveFieldCleanup(new UnknownField("mrclass"), StandardField.KEYWORDS).cleanup(entry);
        new FieldFormatterCleanup(new UnknownField("mrreviewer"), new ClearFormatter()).cleanup(entry);
        new DoiCleanup().cleanup(entry);
        new FieldFormatterCleanup(StandardField.URL, new ClearFormatter()).cleanup(entry);

        // Remove comments: MathSciNet prepends a <pre> html tag
        entry.setCommentsBeforeEntry("");
    }
}

