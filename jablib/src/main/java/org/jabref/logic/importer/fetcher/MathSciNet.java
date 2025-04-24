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
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.os.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.AMSField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;

import kong.unirest.core.JsonNode;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches data from the <a href="http://www.ams.org/mathscinet">MathSciNet</a> API.
 */
public class MathSciNet implements SearchBasedParserFetcher, EntryBasedParserFetcher, IdBasedParserFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(MathSciNet.class);

    private static final Map<StandardField, List<String>> FIELD_MAPPINGS = Map.of(
            StandardField.TITLE, List.of("titles", "title"),
            StandardField.YEAR, List.of("issue", "issue", "pubYear"),
            StandardField.JOURNAL, List.of("issue", "issue", "journal", "shortTitle"),
            StandardField.VOLUME, List.of("issue", "issue", "volume"),
            StandardField.NUMBER, List.of("issue", "issue", "number"),
            StandardField.PAGES, List.of("paging", "paging", "text"),
            StandardField.ISSN, List.of("issue", "issue", "journal", "issn")
    );

    private final ImportFormatPreferences preferences;

    public MathSciNet(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "MathSciNet";
    }

    /**
     * We use <a href="https://mathscinet.ams.org/mathscinet/freetools/mrlookup">MR Lookup</a> instead of the usual search since this tool is also available
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
    public URL getURLForQuery(QueryNode luceneQuery) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder("https://mathscinet.ams.org/mathscinet/api/publications/search");
        uriBuilder.addParameter("query", new DefaultQueryTransformer().transformLuceneQuery(luceneQuery).orElse("")); // query
        uriBuilder.addParameter("currentPage", "1"); // start index
        uriBuilder.addParameter("pageSize", "100"); // page size
        return uriBuilder.build().toURL();
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
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
            BibtexParser bibtexParser = new BibtexParser(preferences, new DummyFileUpdateMonitor());

            try {
                // Depending on the type of query we might get either a json object or directly a json array
                JsonNode node = new JsonNode(response);

                if (node.isArray()) {
                    JSONArray entriesArray = node.getArray();
                    for (int i = 0; i < entriesArray.length(); i++) {
                        JSONObject entryObject = entriesArray.getJSONObject(i);
                        if (entryObject.has("bib")) {
                            String bibTexFormat = entriesArray.getJSONObject(i).getString("bib");
                            entries.addAll(bibtexParser.parseEntries(bibTexFormat));
                        }
                    }
                } else {
                    var element = node.getObject();

                    if (element.has("all")) {
                        JSONArray entriesArray = element.getJSONObject("all").getJSONArray("results");
                        for (int i = 0; i < entriesArray.length(); i++) {
                            String bibTexFormat = entriesArray.getJSONObject(i).getString("bibTexFormat");
                            entries.addAll(bibtexParser.parseEntries(bibTexFormat));
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

            // Set the author and keywords field
            Optional<String> authors = toAuthors(item.optJSONArray("authors"));
            authors.ifPresent(value -> entry.setField(StandardField.AUTHOR, value));

            Optional<String> keywords = getKeywords(item.optJSONObject("primaryClass"));
            keywords.ifPresent(value -> entry.setField(StandardField.KEYWORDS, value));

            // Set the rest of the fields based on the mappings
            for (Map.Entry<StandardField, List<String>> mapEntry : FIELD_MAPPINGS.entrySet()) {
                StandardField field = mapEntry.getKey();
                List<String> path = mapEntry.getValue();
                Optional<String> value = getOthers(item, path);
                value.ifPresent(v -> entry.setField(field, v));
            }

            // Handle articleUrl and mrnumber fields separately, as they are non-nested properties in the JSON and can be retrieved as Strings directly
            String doi = item.optString("articleUrl");
            if (!doi.isEmpty()) {
                try {
                    DOI.parse(doi).ifPresent(validDoi -> entry.setField(StandardField.DOI, validDoi.asString()));
                } catch (IllegalArgumentException e) {
                    // If DOI parsing fails, use the original DOI string
                    entry.setField(StandardField.DOI, doi);
                }
            }

            String mrNumber = item.optString("mrnumber");
            if (!mrNumber.isEmpty()) {
                entry.setField(StandardField.MR_NUMBER, mrNumber);
            }

            return entry;
        } catch (JSONException exception) {
            throw new ParseException("MathSciNet API JSON format has changed", exception);
        }
    }

    private Optional<String> toAuthors(JSONArray authors) {
        if (authors == null) {
            return Optional.empty();
        }

        String authorsString = IntStream.range(0, authors.length())
                                        .mapToObj(authors::getJSONObject)
                                        .map(author -> {
                                            String name = author.optString("name", "");
                                            return fixStringEncoding(name);
                                        })
                                        .collect(Collectors.joining(" and "));

        return Optional.of(authorsString);
    }

    private Optional<String> getKeywords(JSONObject primaryClass) {
        if (primaryClass == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(primaryClass.optString("description", null));
    }

    private Optional<String> getOthers(JSONObject item, List<String> keys) {
        Object value = item;
        for (String key : keys) {
            if (value instanceof JSONObject obj) {
                value = obj.opt(key);
            } else if (value instanceof JSONArray arr) {
                value = arr.opt(Integer.parseInt(key));
            } else {
                break;
            }
        }

        if (value instanceof String stringValue) {
            return Optional.of(fixStringEncoding(stringValue));
        } else if (value instanceof Integer intValue) {
            return Optional.of(intValue.toString());
        }

        return Optional.empty();
    }

    /**
     * Method to change character set, to fix output string encoding
     * If we don't convert to the correct character set, the parser outputs anomalous characters.
     * This is observed in case of non-UTF-8 characters, such as accented characters.
     */

    private String fixStringEncoding(String value) {
        return new String(value.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
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
