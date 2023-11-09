package org.jabref.gui.entryeditor;

import kong.unirest.json.JSONObject;

/**
 * Simple DTO object to hold the scite.ai tallies data for a given DOI
 */
public record SciteTallyDTO(
        String doi,
        int total,
        int supporting,
        int contradicting,
        int mentioning,
        int unclassified,
        int citingPublications) {

    /**
     * Creates a {@link SciteTallyDTO} from a JSONObject (dictionary/map)
     *
     * @param jsonObject The JSON object holding the tally values
     * @return a new {@link SciteTallyDTO}
     */
    public static SciteTallyDTO fromJSONObject(JSONObject jsonObject) {
        return new SciteTallyDTO(
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
