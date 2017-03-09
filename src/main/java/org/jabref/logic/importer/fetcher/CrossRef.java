package org.jabref.logic.importer.fetcher;

import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import org.jabref.logic.identifier.DOI;
import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A class for fetching DOIs from CrossRef
 *
 * See https://github.com/CrossRef/rest-api-doc
 */
public class CrossRef {
    private static final Log LOGGER = LogFactory.getLog(CrossRef.class);

    private static final String API_URL = "http://api.crossref.org";
    // number of results to lookup from crossref API
    private static final int API_RESULTS = 5;

    private static final RemoveBracesFormatter REMOVE_BRACES_FORMATTER = new RemoveBracesFormatter();

    public static Optional<DOI> findDOI(BibEntry entry) {
        Objects.requireNonNull(entry);
        Optional<DOI> doi = Optional.empty();

        // title is minimum requirement
        Optional<String> title = entry.getLatexFreeField(FieldName.TITLE);

        if (!title.isPresent() || title.get().isEmpty()) {
            return doi;
        }

        String query = enhanceQuery(title.get(), entry);

        try {
            HttpResponse<JsonNode> response = Unirest.get(API_URL + "/works")
                    .queryString("query", query)
                    .queryString("rows", API_RESULTS)
                    .asJson();

            JSONArray items = response.getBody().getObject().getJSONObject("message").getJSONArray("items");
            // quality check
            Optional<String> dataDoi = findMatchingEntry(entry, items);

            if (dataDoi.isPresent()) {
                LOGGER.debug("DOI " + dataDoi.get() + " for " + title.get() + " found.");
                return DOI.build(dataDoi.get());
            }
        } catch (UnirestException e) {
            LOGGER.warn("Unable to query CrossRef API: " + e.getMessage(), e);
        }
        return doi;
    }

    private static String enhanceQuery(String query, BibEntry entry) {
        StringBuilder enhancedQuery = new StringBuilder(query);
        // author
        entry.getField(FieldName.AUTHOR).ifPresent(author -> {
            if (!author.isEmpty()) {
                enhancedQuery.append('+').append(author);
            }
        });

        // year
        entry.getField(FieldName.YEAR).ifPresent(year -> {
            if (!year.isEmpty()) {
                enhancedQuery.append('+').append(year);
            }
        });

        return enhancedQuery.toString();
    }

    private static Optional<String> findMatchingEntry(BibEntry entry, JSONArray results) {
        final String entryTitle = REMOVE_BRACES_FORMATTER.format(entry.getLatexFreeField(FieldName.TITLE).orElse(""));
        final StringSimilarity stringSimilarity = new StringSimilarity();

        for (int i = 0; i < results.length(); i++) {
            // currently only title-based
            // title: [ "How the Mind Hurts and Heals the Body." ]
            // subtitle: [ "" ]
            try {
                // title
                JSONObject data = results.getJSONObject(i);
                String dataTitle = data.getJSONArray("title").getString(0);

                if (stringSimilarity.isSimilar(entryTitle, dataTitle)) {
                    return Optional.of(data.getString("DOI"));
                }

                // subtitle
                // additional check, as sometimes subtitle is needed but sometimes only duplicates the title
                if (data.getJSONArray("subtitle").length() > 0) {
                    String dataWithSubTitle = dataTitle + " " + data.getJSONArray("subtitle").getString(0);

                    if (stringSimilarity.isSimilar(entryTitle, dataWithSubTitle)) {
                        return Optional.of(data.getString("DOI"));
                    }
                }
            } catch(JSONException ex) {
                LOGGER.warn("CrossRef API JSON format has changed: " + ex.getMessage());
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
