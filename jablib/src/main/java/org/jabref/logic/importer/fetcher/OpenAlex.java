package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jabref.logic.cleanup.DoiCleanup;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.ProgressInputStream;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.search.query.BaseQueryNode;

import com.google.common.annotations.VisibleForTesting;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jooq.lambda.Unchecked;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fetcher for OpenAlex Works API
/// Docs: <a href="https://docs.openalex.org/api-entities/works"> OpenAlex API Docs</a>
@NullMarked
public class OpenAlex implements CustomizableKeyFetcher, SearchBasedParserFetcher, FulltextFetcher, EntryBasedFetcher, CitationFetcher {

    public static final String FETCHER_NAME = "OpenAlex";

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAlex.class);

    private static final String URL_PATTERN = "https://api.openalex.org/works";

    private final ImporterPreferences importerPreferences;

    public OpenAlex(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @VisibleForTesting
    Optional<String> extractOpenAlexId(String url) {
        try {
            URL u = new URL(url);

            if (!"openalex.org".equalsIgnoreCase(u.getHost())) {
                return Optional.empty();
            }

            String path = u.getPath();          // "/works/W4408614692" or "/W4408614692"
            if (path.isBlank() || "/".equals(path)) {
                return Optional.empty();
            }

            String[] segments = path.split("/");
            String id = segments[segments.length - 1];

            return id.isBlank() ? Optional.empty() : Optional.of(id);
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        importerPreferences.getApiKey(FETCHER_NAME).ifPresent(apiKey -> uriBuilder.addParameter("api_key", apiKey));
        uriBuilder.addParameter("search", new DefaultQueryTransformer().transformSearchQuery(queryNode).orElse(""));
        URL result = uriBuilder.build().toURL();
        LOGGER.debug("URL for query: {}", result);
        return result;
    }

    private Optional<URL> getUrl(BibEntry entry, List<String> fieldsToSelect) throws MalformedURLException {
        try {
            // Supported identifiers for lookup are listed at https://docs.openalex.org/api-entities/works/work-object#ids
            return entry.getField(StandardField.DOI)
                        .flatMap(DOI::findInText)
                        .map(Unchecked.function(doi -> getUrl("/" + doi.getURIAsASCIIString(), fieldsToSelect)))

                        .or(() -> entry.getField(StandardField.PMID)
                                       // URL: See https://docs.openalex.org/api-entities/works/get-a-single-work#external-ids
                                       .map(Unchecked.function(pmid -> getUrl("/pmid:" + pmid, fieldsToSelect))))

                        // Fallback: OpenAlex identifier from URL
                        .or(() -> entry.getField(StandardField.URL)
                                       .flatMap(this::extractOpenAlexId)
                                       .map(Unchecked.function(id -> getUrl("/" + id, fieldsToSelect))));
        } catch (RuntimeException ignored) {
            LOGGER.debug("Invalid OpenAlex URL");
            return Optional.empty();
        }
    }

    private URIBuilder getUriBuilder(String tail, List<String> fieldsToSelect) throws MalformedURLException {
        try {
            URIBuilder uriBuilder = new URIBuilder(URL_PATTERN + tail);
            importerPreferences.getApiKey(FETCHER_NAME).ifPresent(apiKey -> uriBuilder.addParameter("api_key", apiKey));
            String fieldList = fieldsToSelect.stream()
                                             .collect(Collectors.joining(","));
            if (!fieldList.isEmpty()) {
                uriBuilder.addParameter("select", fieldList);
            }
            return uriBuilder;
        } catch (URISyntaxException exception) {
            throw new MalformedURLException(exception.getMessage());
        }
    }

    private URL getUrl(String tail, List<String> fieldsToSelect) throws MalformedURLException {
        try {
            return getUriBuilder(tail, fieldsToSelect).build().toURL();
        } catch (URISyntaxException | MalformedURLException exception) {
            throw new MalformedURLException(exception.getMessage());
        }
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream);
            LOGGER.debug("Response for Parser: {}", response);
            if (response.isEmpty()) {
                return List.of();
            }

            // Response contains a list in "results"; single-item lookup (by id) returns the work object directly
            if (response.has("results")) {
                JSONArray items = response.getJSONArray("results");
                List<BibEntry> entries = new ArrayList<>(items.length());
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    entries.add(jsonItemToBibEntry(item));
                }
                return entries;
            } else {
                // Single work object
                return List.of(jsonItemToBibEntry(response));
            }
        };
    }

    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            DoiCleanup DoiCleanup = new DoiCleanup();
            BibEntry entry = new BibEntry();

            entry.setType(EntryTypeFactory.parse(item.getString("type")));

            entry.setField(StandardField.TITLE, item.optString("title"));

            if (item.has("publication_date")) {
                entry.setField(StandardField.DATE, item.optString("publication_date"));
            } else if (item.has("publication_year")) {
                entry.setField(StandardField.YEAR, item.optString("publication_year"));
            }

            JSONObject ids = item.optJSONObject("ids");
            if (ids != null) {
                if (ids.has("doi")) {
                    entry.setField(StandardField.DOI, ids.optString("doi"));
                }
                if (ids.has("pmid")) {
                    entry.setField(StandardField.PMID, ids.optString("pmid"));
                }
            }

            String url = item.optString("id");
            if (url != null && !url.isBlank()) {
                entry.setField(StandardField.URL, item.optString("id"));
            }

            // Authors
            JSONArray authorships = item.optJSONArray("authorships");
            if (authorships != null) {
                String authors = collectAuthorships(authorships);
                entry.setField(StandardField.AUTHOR, authors);
            }
            // Volume / Issue / Pages
            JSONObject biblio = item.optJSONObject("biblio");
            if (biblio != null) {
                entry.setField(StandardField.VOLUME, biblio.optString("volume"));
                entry.setField(StandardField.NUMBER, biblio.optString("issue"));
                String first = biblio.optString("first_page");
                String last = biblio.optString("last_page");
                boolean firstAvailable = StringUtil.isNotBlank(first);
                boolean lastAvailable = StringUtil.isNotBlank(last);
                if (firstAvailable && lastAvailable) {
                    entry.setField(StandardField.PAGES, first + "--" + last);
                } else if (firstAvailable) {
                    entry.setField(StandardField.PAGES, first);
                } else if (lastAvailable) {
                    entry.setField(StandardField.PAGES, last);
                }
            }

            // Keywords from concepts
            JSONArray concepts = item.optJSONArray("concepts");
            if (concepts != null) {
                List<String> kws = new ArrayList<>();
                for (int i = 0; i < concepts.length(); i++) {
                    JSONObject c = concepts.getJSONObject(i);
                    String name = c.optString("display_name");
                    if (name != null && !name.isBlank()) {
                        kws.add(name);
                    }
                }
                if (!kws.isEmpty()) {
                    entry.setField(StandardField.KEYWORDS, String.join(", ", kws));
                }
            }
            DoiCleanup.cleanup(entry);
            return entry;
        } catch (JSONException e) {
            throw new ParseException("OpenAlex API JSON format has changed", e);
        }
    }

    private String collectAuthorships(JSONArray authorships) {
        return java.util.stream.IntStream.range(0, authorships.length())
                                         .mapToObj(authorships::getJSONObject)
                                         .map(obj -> {
                                             JSONObject author = obj.optJSONObject("author");
                                             if (author != null) {
                                                 return author.optString("display_name");
                                             }
                                             return obj.optString("author_position");
                                         })
                                         .filter(StringUtil::isNotBlank)
                                         .collect(Collectors.joining(" and "));
    }

    private Optional<JSONObject> getWorkObject(BibEntry entry, List<String> fieldsToSelect) throws FetcherException {
        Optional<URL> apiUrl;
        try {
            apiUrl = getUrl(entry, fieldsToSelect);
        } catch (MalformedURLException e) {
            throw new FetcherException("Could not create URL for work resource for BibEntry {}", entry.getKeyAuthorTitleYear(10), e);
        }
        if (apiUrl.isEmpty()) {
            return Optional.empty();
        }

        try (ProgressInputStream stream = getUrlDownload(apiUrl.get()).asInputStream()) {
            return Optional.of(JsonReader.toJsonObject(stream));
        } catch (Exception e) {
            throw new FetcherException(apiUrl.get(), "Failed to query OpenAlex for fulltext", e);
        }
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        try {
            return getWorkObject(entry, List.of("primary_location"))
                    .map(work -> work.optJSONObject("primary_location"))
                    .filter(Objects::nonNull)
                    .map(primaryLocation -> primaryLocation.optString("pdf_url", ""))
                    .filter(StringUtil::isNotBlank)
                    .map(Unchecked.function(pdfUrl -> URLUtil.create(pdfUrl)));
        } catch (RuntimeException e) {
            LOGGER.warn("Malformed URL", e);
            throw (MalformedURLException) e.getCause();
        }
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> title = entry.getTitle();
        if (title.isEmpty()) {
            return List.of();
        }
        return performSearch(title.get());
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }

    // region CitationFetcher

    private List<BibEntry> workUrlsToBibEntryList(@Nullable JSONArray workUrlArray) {
        if (workUrlArray == null) {
            List.of();
        }
        // TODO: This could be batched - see https://github.com/JabRef/jabref/pull/15023#issuecomment-3846630255
        return IntStream.range(0, workUrlArray.length())
                        .mapToObj(workUrlArray::getString)
                        .map(Unchecked.function(workUrl -> getUrl("/" + workUrl, List.of())))
                        .map(Unchecked.function(url -> {
                            try (ProgressInputStream stream = getUrlDownload(url).asInputStream()) {
                                return jsonItemToBibEntry(JsonReader.toJsonObject(stream));
                            } catch (FetcherClientException e) {
                                String redactedUrl = FetcherException.getRedactedUrl(url.toString());
                                if (e.getHttpResponse().isPresent()) {
                                    int code = e.getHttpResponse().get().statusCode();
                                    if (code == 404) {
                                        LOGGER.trace("Work not found at URL: {}", redactedUrl);
                                    } else {
                                        LOGGER.debug("Could not fetch work at URL: {}", redactedUrl, e);
                                    }
                                } else {
                                    LOGGER.debug("Could not fetch work at URL: {}", redactedUrl, e);
                                }
                                return new BibEntry().withField(StandardField.URL, redactedUrl).withChanged(true);
                            } catch (RuntimeException e) {
                                String redactedUrl = FetcherException.getRedactedUrl(url.toString());
                                LOGGER.debug("Could not fetch work at URL: {}", redactedUrl, e);
                                return new BibEntry().withField(StandardField.URL, redactedUrl).withChanged(true);
                            }
                        }))
                        .toList();
    }

    private List<BibEntry> workArrayToBibEntryList(@Nullable JSONArray workUrlArray) {
        if (workUrlArray == null) {
            return List.of();
        }
        return IntStream.range(0, workUrlArray.length())
                        .mapToObj(workUrlArray::getJSONObject)
                        .map(Unchecked.function(jsonItem -> jsonItemToBibEntry(jsonItem)))
                        .toList();
    }

    private List<BibEntry> fetch(Optional<URI> apiUri, String arrayElementName, Function<JSONArray, List<BibEntry>> handler) throws FetcherException {
        if (apiUri.isEmpty()) {
            return List.of();
        }
        try (ProgressInputStream stream = getUrlDownload(apiUri.get().toURL()).asInputStream()) {
            JSONObject response = JsonReader.toJsonObject(stream);
            JSONArray results = response.getJSONArray(arrayElementName);
            return handler.apply(results);
        } catch (IOException | ParseException e) {
            throw new FetcherException("Could not fetch data from OpenAlex", e);
        }
    }

    /// @implNote This method is similar to  [#getCitations(BibEntry)]. Streamlining this into one is not leading to more maintainable code, because handling the exceptions properly
    @Override
    public List<BibEntry> getReferences(BibEntry entry) throws FetcherException {
        LOGGER.trace("Getting references for entry: {}", entry.getKeyAuthorTitleYear(10));
        return fetch(getReferencesApiUri(entry), "referenced_works", this::workUrlsToBibEntryList);
    }

    @Override
    public List<BibEntry> getCitations(BibEntry entry) throws FetcherException {
        LOGGER.trace("Getting citations for entry: {}", entry.getKeyAuthorTitleYear(10));
        return fetch(getCitationsApiUri(entry), "results", this::workArrayToBibEntryList);
    }

    @Override
    public Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException {
        return getWorkObject(entry, List.of("cited_by_count"))
                .map(work -> work.optInt("cited_by_count"))
                .filter(Objects::nonNull);
    }

    @Override
    public Optional<URI> getReferencesApiUri(BibEntry entry) {
        try {
            return getUrl(entry, List.of("referenced_works"))
                    .map(Unchecked.function(URL::toURI));
        } catch (MalformedURLException e) {
            LOGGER.debug("Could not create references API URI", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<URI> getCitationsApiUri(BibEntry entry) {
        /* Officially, `cited_by_api_url` is to be used to get citations.
         * However, this URL may be missing */
            /*
            return getWorkObject(entry, List.of("cited_by_api_url"))
                    .map(work -> work.optString("cited_by_api_url"))
                    .filter(url -> !StringUtil.isNullOrEmpty(url))
                    .map(Unchecked.function(apiUrl -> new URI(apiUrl).toURL()))
                    .map(Unchecked.function(url -> {
                        try (ProgressInputStream stream = getUrlDownload(url).asInputStream()) {
                            return JsonReader.toJsonArray(stream);
                        }
                    }))
                    .stream()
                    .flatMap(workUrlArray -> workUrlsToBibEntryStream(workUrlArray))
                    .toList();
             */

        // Instead, we perform a search for works that cite the given work's ID
        try {
            return getWorkObject(entry, List.of("id"))
                    .map(work -> work.optString("id"))
                    .filter(Objects::nonNull)
                    .map(Unchecked.function(id ->
                            getUriBuilder("", List.of())
                                    .addParameter("filter", "cites:" + id)
                                    .build()
                    ));
        } catch (FetcherException e) {
            LOGGER.debug("Could not create citations API URI", e);
            return Optional.empty();
        }
    }

    // endregion
}
