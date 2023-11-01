package org.jabref.logic.importer.fileformat;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.AuthorListParser;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class CiteSeerParser {

    public List<BibEntry> parseCiteSeerResponse(JSONArray jsonResponse) throws ParseException {
        List<BibEntry> response = new ArrayList<>();
        CookieHandler.setDefault(new CookieManager());
        for (int i = 0; i < jsonResponse.length(); ++i) {
            response.add(parseBibEntry(jsonResponse.getJSONObject(i)));
        }
        return response;
    }

    /***
     * WARNING: The DOI for each parsed BibEntry is not a valid DOI.
     * Cite Seer associates an id with each response as a unique hash.
     * However, it is not a valid variation of a DOI value.
     *
     * @param jsonObj Search response as a JSON Object
     * @return BibEntry
     */
    private BibEntry parseBibEntry(JSONObject jsonObj) {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.DOI, jsonObj.optString("id"));
        bibEntry.setField(StandardField.TITLE, jsonObj.optString("title"));
        bibEntry.setField(StandardField.VENUE, jsonObj.optString("venue"));
        bibEntry.setField(StandardField.YEAR, jsonObj.optString("year"));
        bibEntry.setField(StandardField.PUBLISHER, jsonObj.optString("publisher"));
        bibEntry.setField(StandardField.ABSTRACT, jsonObj.optString("abstract"));
        bibEntry.setField(StandardField.AUTHOR, parseAuthors(Optional.ofNullable(jsonObj.optJSONArray("authors"))));
        bibEntry.setField(StandardField.JOURNAL, jsonObj.optString("journal"));
        bibEntry.setField(StandardField.URL, jsonObj.optString("source"));
        return bibEntry;
    }

    private String parseAuthors(Optional<JSONArray> authorsOpt) {
        if (authorsOpt.isEmpty()) {
            return "";
        }
        String separator = " and ";
        JSONArray authorsArray = authorsOpt.get();
        StringBuilder authorsStringBuilder = new StringBuilder();
        for (int i = 0; i < authorsArray.length() - 1; i++) {
            authorsStringBuilder.append(StringUtil.shaveString(authorsArray.getString(i))).append(separator);
        }
        authorsStringBuilder.append(authorsArray.getString(authorsArray.length() - 1));
        return new AuthorListParser().parse(authorsStringBuilder.toString()).getAsLastFirstNamesWithAnd(false);
    }
}
