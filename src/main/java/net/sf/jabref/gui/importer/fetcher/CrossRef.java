package net.sf.jabref.gui.importer.fetcher;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import info.debatty.java.stringsimilarity.Levenshtein;
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
    private static final Levenshtein METRIC_DISTANCE = new Levenshtein();
    private static final int METRIC_THRESHOLD = 4;

    public static Optional<DOI> findDOI(BibEntry entry) {
        Objects.requireNonNull(entry);
        Optional<DOI> doi = Optional.empty();

        // title is minimum requirement
        Optional<String> title = entry.getFieldOptional(FieldName.TITLE);

        if (!title.isPresent() || title.get().isEmpty()) {
            return doi;
        }

        String query = enhanceQuery(title.get(), entry);

        try {
            HttpResponse<JsonNode> response = Unirest.get(API_URL + "/works")
                    .queryString("query", query)
                    .queryString("rows", "1")
                    .asJson();

            JSONArray items = response.getBody().getObject().getJSONObject("message").getJSONArray("items");
            // quality check
            if (checkValidity(entry, items)) {
                String dataDOI = items.getJSONObject(0).getString("DOI");
                LOGGER.debug("DOI " + dataDOI + " for " + title.get() + " found.");
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
        entry.getFieldOptional(FieldName.AUTHOR).ifPresent(author -> {
            if (!author.isEmpty()) {
                enhancedQuery.append('+').append(author);
            }
        });

        // year
        entry.getFieldOptional(FieldName.YEAR).ifPresent(year -> {
            if (!year.isEmpty()) {
                enhancedQuery.append('+').append(year);
            }
        });

        return enhancedQuery.toString();
    }

    private static boolean checkValidity(BibEntry entry, JSONArray result) {
        // TODO: use latex-free version instead in the future
        final String entryTitle = removeLaTeX(entry.getField(FieldName.TITLE));

        // currently only title-based
        // title: [ "How the Mind Hurts and Heals the Body." ]
        // subtitle: [ "" ]
        try {
            // title
            JSONObject data = result.getJSONObject(0);
            String dataTitle = data.getJSONArray("title").getString(0);

            if (editDistanceIgnoreCase(entryTitle, dataTitle) <= METRIC_THRESHOLD) {
                return true;
            }

            // subtitle
            // additional check, as sometimes subtitle is needed but sometimes only duplicates the title
            if (data.getJSONArray("subtitle").length() > 0) {
                String dataWithSubTitle = dataTitle + " " + data.getJSONArray("subtitle").getString(0);

                return editDistanceIgnoreCase(entryTitle, dataWithSubTitle) <= METRIC_THRESHOLD;
            }

            return false;
        } catch(JSONException ex) {
            return false;
        }
    }

    private static String removeLaTeX(String text) {
        String result;
        // remove braces
        result = new RemoveBracesFormatter().format(text);
        // convert to unicode
        result = new LatexToUnicodeFormatter().format(result);

        return result;
    }

    private static double editDistanceIgnoreCase(String a, String b) {
        // TODO: locale is dependent on the language of the strings?!
        return METRIC_DISTANCE.distance(a.toLowerCase(Locale.ENGLISH), b.toLowerCase(Locale.ENGLISH));
    }
}
