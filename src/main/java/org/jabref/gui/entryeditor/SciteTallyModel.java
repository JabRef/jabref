package org.jabref.gui.entryeditor;

import kong.unirest.json.JSONObject;

/**
 * Simple model object to hold the scite.ai tallies data for a given DOI
 */
public record SciteTallyModel(
        String doi,
        int total,
        int supporting,
        int contradicting,
        int mentioning,
        int unclassified,
        int citingPublications) {

    /**
     * Creates a {@link SciteTallyModel} from a JSONObject (dictionary/map)
     *
     * @param jsonObject The JSON object holding the tally values
     * @return a new {@link SciteTallyModel}
     */
    public static SciteTallyModel fromJSONObject(JSONObject jsonObject) {
        return new SciteTallyModel(
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
