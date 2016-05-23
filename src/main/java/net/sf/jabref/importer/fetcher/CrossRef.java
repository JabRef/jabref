package net.sf.jabref.importer.fetcher;

import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import info.debatty.java.stringsimilarity.Levenshtein;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;

/**
 * A class for fetching DOIs from CrossRef
 *
 * See https://github.com/CrossRef/rest-api-doc
 */
public class CrossRef {
    private static final Log LOGGER = LogFactory.getLog(CrossRef.class);

    private static final String API_URL = "http://api.crossref.org";

    public static Optional<DOI> findDOI(BibEntry entry) {
        Objects.requireNonNull(entry);
        Optional<DOI> doi = Optional.empty();

        // title is minimum requirement
        String title = entry.getField("title");

        if ((title == null) || title.isEmpty()) {
            return doi;
        }

        String query = enhanceQuery(title, entry);

        try {
            HttpResponse<JsonNode> response = Unirest.get(API_URL + "/works")
                    .queryString("query", query)
                    .queryString("rows", "1")
                    .asJson();

            JSONArray items = response.getBody().getObject().getJSONObject("message").getJSONArray("items");
            // quality check
            if (checkValidity(entry, items)) {
                String dataDOI = items.getJSONObject(0).getString("DOI");
                LOGGER.info("DOI " + dataDOI + " for " + title + " found.");
                return DOI.build(dataDOI);
            }
        } catch (UnirestException e) {
            LOGGER.warn("Unable to query CrossRef API: " + e.getMessage(), e);
        }
        return doi;
    }

    private static String enhanceQuery(String query, BibEntry entry) {
        StringBuilder enhancedQuery = new StringBuilder(query);
        // author
        String author = entry.getField("author");
        if ((author != null) && !author.isEmpty()) {
            enhancedQuery.append('+').append(author);
        }

        // year
        String year = entry.getField("year");
        if ((year != null) && !year.isEmpty()) {
            enhancedQuery.append('+').append(year);
        }

        return enhancedQuery.toString();
    }

    private static boolean checkValidity(BibEntry entry, JSONArray result) {
        // currently only title
        // title: [ "How the Mind Hurts and Heals the Body." ]
        String dataTitle = result.getJSONObject(0).getJSONArray("title").getString(0);
        // author: [ { affiliation: [ ], family: "Ray", given: "Oakley" } ]
        // JSONArray dataAuthors = result.getJSONObject(0).getJSONArray("author");
        Levenshtein levenshtein = new Levenshtein();
        // TODO: formatter might not be goog enough!
        String entryTitle = new LatexCleanupFormatter().format(entry.getField("title"));
        double editDistance = levenshtein.distance(entryTitle.toLowerCase(), dataTitle.toLowerCase());
        return editDistance <= 5;
    }
}
