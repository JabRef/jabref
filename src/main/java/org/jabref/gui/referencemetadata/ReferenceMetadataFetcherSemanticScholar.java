package org.jabref.gui.referencemetadata;

import java.util.Optional;

import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Minimalistic reference metadata fetcher for Semantic Scholar
 *
 * @see: https://api.semanticscholar.org/
 */
public class ReferenceMetadataFetcherSemanticScholar {
    // API format: https://api.semanticscholar.org/v1/paper/[Paper Identifier or URL]
    private static final String API_URL = "https://api.semanticscholar.org/v1/paper/";

    private static final int CITATION_COUNT_STRING_STRING_LENGTH = 7;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceMetadataFetcherSemanticScholar.class);

    public boolean fetchFor(BibDatabaseContext database, ObservableList<BibEntry> entries, DialogService dialogService) {

        for (int entryIndex = 0; entryIndex < entries.size(); entryIndex++) {
            BibEntry entry = entries.get(entryIndex);

            String citationKey = entry.getField(InternalField.KEY_FIELD).orElse("").trim();

            Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);

            if (doi.isPresent()) {
                String doiString = doi.get().getDOI();

                HttpResponse<JsonNode> jsonResponse = Unirest.get(API_URL + doiString)
                                                             .queryString("httpAccept", "application/json")
                                                             .asJson();

                if (!jsonResponse.isSuccess()) {
                    LOGGER.info("fetching metadata for reference with citation key \"" + citationKey + "\" was not successful");
                    continue;
                }

                JSONObject jsonData = jsonResponse.getBody().getObject();

                if (jsonData != null && jsonData.has("influentialCitationCount")) {
                    int citationCountNumber = jsonData.getInt("influentialCitationCount");

                    String citationCount = String.format("%0" + CITATION_COUNT_STRING_STRING_LENGTH + "d", citationCountNumber);

                    // set (updated) entry data (citation count)
                    entry.setField(SpecialField.CITATION_COUNT, citationCount);
                }
                else {
                    LOGGER.info("reference with citation key \"" + citationKey + "\" does not have the required metadata");
                }
            }
            else {
                LOGGER.info("skipping reference with citation key \"" + citationKey + "\", since it does not have a DOI");

                entry.setField(SpecialField.CITATION_COUNT, ""); // set empty field
            }
        }

        return true;
    }
}
