package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.BaseSearchQueryTransformer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseSearchFetcher implements PagedSearchBasedParserFetcher, CustomizableKeyFetcher {

    public static final String FETCHER_NAME = "BASE";

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseSearchFetcher.class);

    private static final String API_URL = "https://api.base-search.net/cgi-bin/BaseHttpSearchInterface.fcgi";

    private static final FetcherRateLimiter RATE_LIMITER =
            FetcherRateLimiter.ofRequestsPerSecond(FETCHER_NAME, 1.0);

    private final ImporterPreferences importerPreferences;

    public BaseSearchFetcher(@NonNull ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public String getName() {
        return FETCHER_NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.empty();
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode, int pageNumber) throws URISyntaxException, MalformedURLException {
        BaseSearchQueryTransformer transformer = new BaseSearchQueryTransformer();
        String query = transformer.transformSearchQuery(queryNode).orElse("");

        URIBuilder uriBuilder = new URIBuilder(API_URL);
        uriBuilder.addParameter("func", "PerformSearch");
        uriBuilder.addParameter("format", "json");
        uriBuilder.addParameter("query", query);
        uriBuilder.addParameter("hits", String.valueOf(getPageSize()));
        uriBuilder.addParameter("offset", String.valueOf(getPageSize() * pageNumber));
        importerPreferences.getApiKey(FETCHER_NAME).ifPresent(apiKey -> uriBuilder.addParameter("apikey", apiKey));

        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            String response;
            try {
                response = new String(inputStream.readAllBytes());
            } catch (java.io.IOException e) {
                throw new ParseException("Could not read response from BASE", e);
            }
            JSONObject jsonObject = new JSONObject(response);

            if (jsonObject.has("error")) {
                LOGGER.warn("BASE API returned error: {}", jsonObject.optString("error"));
                return List.of();
            }

            List<BibEntry> entries = new ArrayList<>();
            JSONObject result = jsonObject.optJSONObject("response").optJSONObject("result");
            if (result != null) {
                JSONArray docs = result.optJSONArray("docs");
                for (int i = 0; i < docs.length(); i++) {
                    entries.add(parseEntry(docs.getJSONObject(i)));
                }
            }
            return entries;
        };
    }

    private BibEntry parseEntry(JSONObject doc) {
        BibEntry entry = new BibEntry();

        entry.setType(mapEntryType(doc));

        entry.setField(StandardField.TITLE, doc.optString("dctitle"));
        entry.setField(StandardField.YEAR, doc.optString("dcyear"));
        entry.setField(StandardField.PUBLISHER, doc.optString("dcpublisher"));
        entry.setField(StandardField.DOI, doc.optString("dcdoi"));
        entry.setField(StandardField.URL, doc.optString("dclink"));

        JSONArray creators = doc.optJSONArray("dccreator");
        if (creators != null) {
            List<String> authorList = new ArrayList<>();
            for (int i = 0; i < creators.length(); i++) {
                authorList.add(creators.getString(i));
            }
            entry.setField(StandardField.AUTHOR, String.join(" and ", authorList));
        }

        JSONArray subjects = doc.optJSONArray("dcsubject");
        if (subjects != null) {
            for (int i = 0; i < subjects.length(); i++) {
                entry.addKeyword(subjects.getString(i), ',');
            }
        }

        return entry;
    }

    private StandardEntryType mapEntryType(JSONObject doc) {
        JSONArray typeNorm = doc.optJSONArray("dctypenorm");
        if (typeNorm == null || typeNorm.isEmpty()) {
            return StandardEntryType.Misc;
        }
        String code = typeNorm.getString(0);

        if (code.startsWith("18")) {
            return StandardEntryType.PhdThesis;
        } else if (code.startsWith("13")) {
            return StandardEntryType.InProceedings;
        } else if (code.startsWith("14")) {
            return StandardEntryType.TechReport;
        } else if (code.startsWith("11")) {
            return StandardEntryType.Book;
        } else if (code.startsWith("121")) {
            return StandardEntryType.Article;
        } else {
            return StandardEntryType.Misc;
        }
    }

    @Override
    public boolean isValidKey(String apiKey) {
        try {
            URIBuilder uriBuilder = new URIBuilder(API_URL);
            uriBuilder.addParameter("func", "PerformSearch");
            uriBuilder.addParameter("format", "json");
            uriBuilder.addParameter("query", "test");
            uriBuilder.addParameter("hits", "0");
            uriBuilder.addParameter("apikey", apiKey);
            URL testUrl = uriBuilder.build().toURL();

            org.jabref.logic.net.URLDownload urlDownload = new org.jabref.logic.net.URLDownload(testUrl);
            String response = urlDownload.asString();
            JSONObject jsonObject = new JSONObject(response);
            return !jsonObject.has("error");
        } catch (Exception e) {
            return false;
        }
    }
}
