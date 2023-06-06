package org.jabref.logic.importer.fileformat;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public class CiteSeerParser {

//    private static final Pattern PAPERID = Pattern.compile("pid/[0-9a-zA-Z]+", Pattern.MULTILINE);

    private void appendData(String data, BibEntry entry, Pattern pattern, Field field) {
        Matcher matcher = pattern.matcher(data);
        if (matcher.find()) {
            entry.setField(field, matcher.group(1));
        }
    }

    public List<BibEntry> parseCiteSeerResponse(JSONArray jsonResponse) throws ParseException {
        List<BibEntry> response = new ArrayList<>();
        CookieHandler.setDefault(new CookieManager());
        for (int i = 0; i < jsonResponse.length(); ++i) {
            response.add(parseBibEntry(jsonResponse.getJSONObject(i)));
        }
        return response;
    }

    private BibEntry parseBibEntry(JSONObject jsonObj) throws ParseException {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.TITLE,
                Optional.of(jsonObj.get("title").toString())
                        .orElse(""));
        bibEntry.setField(StandardField.VENUE, Objects.toString(jsonObj.get("venue"), ""));
        bibEntry.setField(StandardField.YEAR, Objects.toString(jsonObj.get("year"), ""));
        bibEntry.setField(StandardField.PUBLISHER, Objects.toString(jsonObj.get("publisher"), ""));
        bibEntry.setField(StandardField.ABSTRACT, Objects.toString(jsonObj.get("abstract"), ""));
        bibEntry.setField(StandardField.AUTHOR, Objects.toString(Optional.ofNullable(jsonObj.get("authors"))
                                                                         .orElse(new JSONArray()), ""));
        bibEntry.setField(StandardField.JOURNAL, Objects.toString(jsonObj.get("journal"), ""));
        return bibEntry;
    }
}
