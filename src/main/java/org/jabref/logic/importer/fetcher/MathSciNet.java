package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import kong.unirest.JsonNode;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches data from the MathSciNet (http://www.ams.org/mathscinet)
 */
public class MathSciNet implements SearchBasedParserFetcher, EntryBasedParserFetcher, IdBasedParserFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MathSciNet.class);
    private final ImportFormatPreferences preferences;
    // Define the field mappings
    private final Map<StandardField, List<String>> fieldMappings = Map.ofEntries(
            Map.entry(StandardField.TITLE, List.of("titles", "title")),
            Map.entry(StandardField.AUTHOR, List.of("authors")),
            Map.entry(StandardField.YEAR, List.of("issue", "issue", "pubYear")),
            Map.entry(StandardField.JOURNAL, List.of("issue", "issue", "journal", "shortTitle")),
            Map.entry(StandardField.VOLUME, List.of("issue", "issue", "volume")),
            Map.entry(StandardField.NUMBER, List.of("issue", "issue", "number")),
            Map.entry(StandardField.PAGES, List.of("paging", "paging", "text")),
            Map.entry(StandardField.KEYWORDS, List.of("primaryClass")),
            Map.entry(StandardField.ISSN, List.of("issue", "issue", "journal", "issn"))
    );
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
            List<BibEntry> entries = new ArrayList<>();

            try {
                // Depending on the type of query we might get either a json object or directly a json array
                JsonNode node = new JsonNode(response);

                if (node.isArray()) {
                    JSONArray entriesArray = node.getArray();
                    for (int i = 0; i < entriesArray.length(); i++) {
                        JSONObject entryObject = entriesArray.getJSONObject(i);
                        BibEntry bibEntry = jsonItemToBibEntry(entryObject);
                        entries.add(bibEntry);
                    }
                } else {
                    var element = node.getObject();

                    if (element.has("all")) {
                        JSONArray entriesArray = element.getJSONObject("all").getJSONArray("results");
                        for (int i = 0; i < entriesArray.length(); i++) {
                            JSONObject entryObject = entriesArray.getJSONObject(i);
                            BibEntry bibEntry = jsonItemToBibEntry(entryObject);
                            entries.add(bibEntry);
                        }
                    } else if (element.has("results")) {
                        JSONArray entriesArray = element.getJSONArray("results");
                        for (int i = 0; i < entriesArray.length(); i++) {
                            JSONObject entryObject = entriesArray.getJSONObject(i);
                            BibEntry bibEntry = jsonItemToBibEntry(entryObject);
                            entries.add(bibEntry);
                        }
                    }
                }
            } catch (JSONException | ParseException e) {
                LOGGER.error("An error occurred while parsing fetched data", e);
                throw new ParseException("Error when parsing entry", e);
            }
            return entries;
        };
    }

    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            BibEntry entry = new BibEntry(StandardEntryType.Article);
            // Set fields based on the mappings
            for (Map.Entry<StandardField, List<String>> mapEntry : fieldMappings.entrySet()) {
                StandardField field = mapEntry.getKey();
                List<String> path = mapEntry.getValue();

                String value;
                if (field == StandardField.AUTHOR) {
                    value = toAuthors(item.optJSONArray(path.getFirst()));
                } else if (field == StandardField.KEYWORDS) {
                    value = getKeywords(item.optJSONObject(path.getFirst()));
                } else {
                    value = getOrNull(item, path).orElse(null);
                }

                if (value != null) {
                    entry.setField(field, value);
                }
            }
            // Handle articleUrl and mrnumber fields separately
            String doi = item.optString("articleUrl", "");
            if (!doi.isEmpty()) {
                entry.setField(StandardField.DOI, doi);
            }

            String mrNumber = item.optString("mrnumber", "");
            if (!mrNumber.isEmpty()) {
                entry.setField(StandardField.MR_NUMBER, mrNumber);
            }
            return entry;
        } catch (JSONException exception) {
            throw new ParseException("MathSciNet API JSON format has changed", exception);
        }
    }

    private Optional<String> getOrNull(JSONObject item, List<String> keys) {
        Object value = item;
        for (String key : keys) {
            if (value instanceof JSONObject) {
                value = ((JSONObject) value).opt(key);
            } else if (value instanceof JSONArray) {
                value = ((JSONArray) value).opt(Integer.parseInt(key));
            } else {
                break;
            }
        }

        if (value instanceof String stringValue) {
            return Optional.of(new String(stringValue.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
        }

        return Optional.empty();
    }

    private String toAuthors(JSONArray authors) {
        if (authors == null) {
            return "";
        }

        return IntStream.range(0, authors.length())
                .mapToObj(authors::getJSONObject)
                .map(author -> {
                    String name = author.optString("name", "");
                    return new String(name.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                })
                .collect(Collectors.joining(" and "));
    }

    private String getKeywords(JSONObject primaryClass) {
        if (primaryClass == null) {
            return "";
        }
        return primaryClass.optString("description", "");
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
