package net.sf.jabref.importer.fetcher;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;

import java.util.Objects;
import java.util.Optional;

/**
 * A class for fetching DOIs from CrossRef
 *
 * @see https://github.com/CrossRef/rest-api-doc
 */
public class CrossRef {
    private static final Log LOGGER = LogFactory.getLog(CrossRef.class);

    private static final String API_URL = "http://api.crossref.org";

    public static Optional<DOI> findDOI(BibEntry entry) {
        Objects.requireNonNull(entry);
        Optional<DOI> doi = Optional.empty();

        // Only title lookup by now
        // FIXME: this is way too less, e.g. service interaction patterns
        // need author last names at least
        String title = entry.getField("title");
        if ((title == null) || title.isEmpty()) {
            return doi;
        }

        try {
            HttpResponse<JsonNode> response = Unirest.get(API_URL + "/works")
                    .queryString("query", title)
                    .queryString("rows", "1")
                    .asJson();

            JSONArray items = response.getBody().getObject().getJSONObject("message").getJSONArray("items");
            String dataTitle = items.getJSONObject(0).getJSONArray("title").getString(0);
            String dataDOI = items.getJSONObject(0).getString("DOI");
            // Only return if entry.title == result.title
            if (dataTitle.equals(title)) {
                LOGGER.info("DOI " + dataDOI + "for " + title + "found.");
                return DOI.build(dataDOI);
            }
        } catch (UnirestException e) {
            LOGGER.warn("Unable to query CrossRef API: " + e.getMessage(), e);
        }
        return doi;
    }
}