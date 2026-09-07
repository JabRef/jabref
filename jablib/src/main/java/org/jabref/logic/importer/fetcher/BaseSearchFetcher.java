package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.BaseSearchQueryTransformer;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class BaseSearchFetcher implements PagedSearchBasedParserFetcher, CustomizableKeyFetcher {

    public static final String FETCHER_NAME = "Bielefeld Academic Search Engine";

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSearchFetcher.class);

    private static final String API_URL = "https://api.base-search.net/cgi-bin/BaseHttpSearchInterface.fcgi";

    private static final FetcherRateLimiter RATE_LIMITER =
            FetcherRateLimiter.ofRequestsPerSecond(FETCHER_NAME, 1.0);

    private static final Map<String, EntryType> ENTRY_TYPES_BY_CODE = Map.ofEntries(
            Map.entry("1", StandardEntryType.Misc),
            Map.entry("11", StandardEntryType.Book),
            Map.entry("111", StandardEntryType.InBook),
            Map.entry("12", IEEETranEntryType.Periodical),
            Map.entry("121", StandardEntryType.Article),
            Map.entry("122", StandardEntryType.SuppPeriodical),
            Map.entry("13", StandardEntryType.InProceedings),
            Map.entry("14", StandardEntryType.TechReport),
            Map.entry("15", BiblatexNonStandardEntryType.Review),
            Map.entry("16", StandardEntryType.Misc),
            Map.entry("17", StandardEntryType.Unpublished),
            Map.entry("18", StandardEntryType.Thesis),
            Map.entry("181", StandardEntryType.Thesis),
            Map.entry("182", StandardEntryType.MastersThesis),
            Map.entry("183", StandardEntryType.PhdThesis),
            Map.entry("19", StandardEntryType.Unpublished),
            Map.entry("1A", IEEETranEntryType.Patent),
            Map.entry("2", BiblatexNonStandardEntryType.Music),
            Map.entry("3", StandardEntryType.Misc),
            Map.entry("4", BiblatexNonStandardEntryType.Audio),
            Map.entry("5", BiblatexNonStandardEntryType.Image),
            Map.entry("51", BiblatexNonStandardEntryType.Image),
            Map.entry("52", BiblatexNonStandardEntryType.Video),
            Map.entry("6", StandardEntryType.Software),
            Map.entry("7", StandardEntryType.Dataset),
            Map.entry("F", StandardEntryType.Misc));

    private final ImporterPreferences importerPreferences;

    public BaseSearchFetcher(@NonNull ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode, int pageNumber) throws URISyntaxException, MalformedURLException {
        BaseSearchQueryTransformer transformer = new BaseSearchQueryTransformer();
        String query = transformer.transformSearchQuery(queryNode).orElse("");
        return buildSearchUrl(query, getPageSize(), getPageSize() * pageNumber);
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response;
            try {
                response = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                throw new ParseException("Could not read response from BASE", e);
            }
            JSONObject jsonObject = new JSONObject(response);

            if (jsonObject.has("error")) {
                LOGGER.warn("BASE API returned error: {}", jsonObject.optString("error"));
                return List.of();
            }

            return getDocs(jsonObject)
                    .stream()
                    .flatMap(docs -> IntStream.range(0, docs.length()).mapToObj(docs::getJSONObject))
                    .map(this::parseEntry)
                    .toList();
        };
    }

    private Optional<JSONArray> getDocs(JSONObject jsonObject) {
        return Optional.ofNullable(jsonObject.optJSONObject("response"))
                       .flatMap(responseObject -> Optional.ofNullable(responseObject.optJSONArray("docs"))
                                                          .or(() -> Optional.ofNullable(responseObject.optJSONObject("result"))
                                                                            .map(result -> result.optJSONArray("docs"))));
    }

    private BibEntry parseEntry(JSONObject doc) {
        BibEntry entry = new BibEntry(mapEntryType(doc));
        entry = withFieldIfPresent(entry, StandardField.TITLE, getFirstValue(doc, "dctitle"));
        entry = withFieldIfPresent(entry, StandardField.YEAR, getFirstValue(doc, "dcyear"));
        entry = withFieldIfPresent(entry, StandardField.PUBLISHER, getFirstValue(doc, "dcpublisher"));
        entry = withFieldIfPresent(entry, StandardField.DOI, getFirstValue(doc, "dcdoi"));
        entry = withFieldIfPresent(entry, StandardField.URL, getFirstValue(doc, "dclink"));
        entry = withFieldIfPresent(entry, StandardField.AUTHOR, getJoinedValues(doc, "dccreator", " and "));

        for (String subject : getValues(doc, "dcsubject")) {
            entry.addKeyword(subject, ',');
        }

        return entry;
    }

    private BibEntry withFieldIfPresent(BibEntry entry, Field field, Optional<String> value) {
        return value.map(fieldValue -> entry.withField(field, fieldValue))
                    .orElse(entry);
    }

    private Optional<String> getFirstValue(JSONObject doc, String key) {
        return getValues(doc, key).stream()
                                  .findFirst();
    }

    private Optional<String> getJoinedValues(JSONObject doc, String key, String delimiter) {
        List<String> values = getValues(doc, key);
        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join(delimiter, values));
    }

    private List<String> getValues(JSONObject doc, String key) {
        Object value = doc.opt(key);
        return switch (value) {
            case JSONArray array ->
                    IntStream.range(0, array.length())
                             .mapToObj(array::optString)
                             .filter(StringUtil::isNotBlank)
                             .toList();
            case Number number ->
                    List.of(number.toString());
            case String string when StringUtil.isNotBlank(string) ->
                    List.of(string);
            case null ->
                    List.of();
            default ->
                    List.of();
        };
    }

    private EntryType mapEntryType(JSONObject doc) {
        return getFirstValue(doc, "dctypenorm")
                .map(this::mapEntryType)
                .orElse(StandardEntryType.Misc);
    }

    private EntryType mapEntryType(String code) {
        return ENTRY_TYPES_BY_CODE.getOrDefault(code.toUpperCase(Locale.ROOT), StandardEntryType.Misc);
    }

    URL getValidationUrl(String apiKey) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(buildSearchUrl("test", 0, 0).toString());
        uriBuilder.setParameter("apikey", apiKey);
        return uriBuilder.build().toURL();
    }

    private URL buildSearchUrl(String query, int hits, int offset) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(API_URL);
        uriBuilder.addParameter("func", "PerformSearch");
        uriBuilder.addParameter("format", "json");
        uriBuilder.addParameter("query", query);
        uriBuilder.addParameter("hits", String.valueOf(hits));
        uriBuilder.addParameter("offset", String.valueOf(offset));
        importerPreferences.getApiKey(FETCHER_NAME).ifPresent(apiKey -> uriBuilder.addParameter("apikey", apiKey));
        return uriBuilder.build().toURL();
    }

    @Override
    public URLDownload getUrlDownload(URL url) {
        RATE_LIMITER.acquire(url.toString());
        return new URLDownload(url);
    }

    boolean isValidKeyResponse(String response) {
        JSONObject jsonObject = new JSONObject(response);
        return !jsonObject.has("error");
    }

    @Override
    public boolean isValidKey(String apiKey) {
        try {
            URLDownload urlDownload = getUrlDownload(getValidationUrl(apiKey));
            return isValidKeyResponse(urlDownload.asString());
        } catch (URISyntaxException | MalformedURLException | FetcherException | JSONException e) {
            LOGGER.debug("BASE API key validation failed", e);
            return false;
        }
    }
}
