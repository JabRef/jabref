package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.cleanup.DoiCleanup;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetcher for OpenAlex Works API
 * Docs: <a href="https://docs.openalex.org/api-entities/works"> OpenAlex API Docs</a>
 */
public class OpenAlex implements SearchBasedParserFetcher, FulltextFetcher, EntryBasedFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAlex.class);

    private static final String URL_PATTERN = "https://api.openalex.org/works";
    private static final String NAME = "OpenAlex";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(URL_PATTERN);
        uriBuilder.addParameter("search", new DefaultQueryTransformer().transformSearchQuery(queryNode).orElse(""));
        URL result = uriBuilder.build().toURL();
        LOGGER.debug("URL for query: {}", result);
        return result;
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

            // Type
            entry.setType(EntryTypeFactory.parse(item.getString("type")));

            // Title
            entry.setField(StandardField.TITLE, item.optString("title"));

            // Year
            if (item.has("publication_year")) {
                entry.setField(StandardField.YEAR, String.valueOf(item.optInt("publication_year")));
            }

            // DOI
            if (item.has("doi")) {
                entry.setField(StandardField.DOI, item.optString("doi"));
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
                if (!first.isBlank() || !last.isBlank()) {
                    String pages = first + (first.isBlank() || last.isBlank() ? "" : "--") + last;
                    entry.setField(StandardField.PAGES, pages);
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
                                         .filter(s -> s != null && !s.isBlank())
                                         .collect(Collectors.joining(" and "));
    }

    @Override
    public Optional<URL> findFullText(@NonNull BibEntry entry) throws IOException, FetcherException {
        Objects.requireNonNull(entry);
        // Build an OpenAlex API URL from DOI or OpenAlex ID/URL
        Optional<URL> apiUrl = buildApiUrl(entry);
        if (apiUrl.isEmpty()) {
            return Optional.empty();
        }

        // Query the single work object and try to extract a PDF URL
        JSONObject work;
        try (var stream = getUrlDownload(apiUrl.get()).asInputStream()) {
            work = JsonReader.toJsonObject(stream);
        } catch (Exception e) {
            throw new FetcherException(apiUrl.get(), "Failed to query OpenAlex for fulltext", e);
        }

        // Get pdf_url from primary_location
        JSONObject primaryLocation = work.optJSONObject("primary_location");
        if (primaryLocation != null) {
            String pdfUrl = primaryLocation.optString("pdf_url", "");
            if (!pdfUrl.isBlank()) {
                try {
                    return Optional.of(URLUtil.create(pdfUrl));
                } catch (MalformedURLException e) {
                    throw new MalformedURLException(e.getMessage());
                }
            }
        }

        return Optional.empty();
    }

    private Optional<URL> buildApiUrl(BibEntry entry) throws MalformedURLException {
        Optional<String> doiOpt = entry.getField(StandardField.DOI);
        if (doiOpt.isPresent()) {
            String doi = doiOpt.get().trim();
            if (!doi.isEmpty()) {
                try {
                    return Optional.of(URLUtil.create("https://api.openalex.org/works/https://doi.org/" + doi));
                } catch (MalformedURLException exception) {
                    throw new MalformedURLException(exception.getMessage());
                }
            }
        }
        Optional<String> urlOpt = entry.getField(StandardField.URL);
        if (urlOpt.isPresent()) {
            String url = urlOpt.get();
            String lower = url.toLowerCase();
            try {
                int idx = lower.indexOf("openalex.org/");
                if (idx >= 0) {
                    String tail = url.substring(idx + "openalex.org/".length());
                    LOGGER.debug("URL for query: {}", tail);
                    int queryIdx = tail.indexOf('?');
                    if (queryIdx >= 0) {
                        tail = tail.substring(0, queryIdx);
                    }
                    if (!tail.isBlank()) {
                        return Optional.of(URLUtil.create("https://api.openalex.org/works/" + tail));
                    }
                }
            } catch (MalformedURLException ignored) {
                throw new IllegalArgumentException("Invalid OpenAlex URL: " + url);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> title = entry.getTitle();
        if (title.isEmpty()) {
            return new ArrayList<>();
        }
        return performSearch(title.get());
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }
}
