package org.jabref.model.sciteTallies;

import kong.unirest.core.json.JSONObject;

/// Simple model object to hold the scite.ai tallies data for a given DOI
public record TalliesResponse(
        String doi,
        int total,
        int supporting,
        int contradicting,
        int mentioning,
        int unclassified,
        int citingPublications) {

    /// Creates a [TalliesResponse] from a JSONObject (dictionary/map)
    ///
    /// @param jsonObject The JSON object holding the tally values
    /// @return a new [TalliesResponse]
    public static TalliesResponse fromJSONObject(JSONObject jsonObject) {
        return new TalliesResponse(
                jsonObject.getString("doi"),
                jsonObject.getInt("total"),
                jsonObject.getInt("supporting"),
                jsonObject.getInt("contradicting"),
                jsonObject.getInt("mentioning"),
                jsonObject.getInt("unclassified"),
                jsonObject.getInt("citingPublications")
        );
    }
}
