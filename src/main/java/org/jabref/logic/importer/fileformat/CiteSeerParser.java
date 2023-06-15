package org.jabref.logic.importer.fileformat;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class CiteSeerParser {

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
        bibEntry.setField(StandardField.AUTHOR, parseAuthors(Optional.ofNullable(jsonObj.get("authors")).orElse(new Object())));
        bibEntry.setField(StandardField.JOURNAL, Objects.toString(jsonObj.get("journal"), ""));
        bibEntry.setField(StandardField.URL, Objects.toString(jsonObj.get("source"), ""));
        return bibEntry;
    }

    private String parseAuthors(Object authorsObj) {
        String authorsStr = StringUtils.remove(StringUtil.stripBrackets(Objects.toString(authorsObj, "")), '"');
        return Arrays.stream(authorsStr.split(","))
                      .reduce(null, (allAuthors, currAuthor) -> {
                          String[] fullName = StringUtils.remove(currAuthor, '"').split(" ");
                          ArrayUtils.shift(fullName, 1);
                          String lastNameFirstName = StringUtil.join(fullName, " ", 0, fullName.length);
                          if (allAuthors == null) {
                              return lastNameFirstName;
                          }
                          return StringUtils.join(List.of(allAuthors, lastNameFirstName), " and ");
                      });
    }
}
