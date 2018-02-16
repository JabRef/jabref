package org.jabref.logic.importer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.Month;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONEntryParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONEntryParser.class);

    /**
     * Convert a JSONObject containing a bibJSON entry to a BibEntry
     *
     * @param bibJsonEntry The JSONObject to convert
     * @return the converted BibEntry
     */
    public BibEntry parseBibJSONtoBibtex(JSONObject bibJsonEntry, Character keywordSeparator) {
        // Fields that are directly accessible at the top level BibJson object
        String[] singleFieldStrings = {FieldName.YEAR, FieldName.TITLE, FieldName.ABSTRACT, FieldName.MONTH};

        // Fields that are accessible in the journal part of the BibJson object
        String[] journalSingleFieldStrings = {FieldName.PUBLISHER, FieldName.NUMBER, FieldName.VOLUME};

        BibEntry entry = new BibEntry();
        entry.setType("article");

        // Authors
        if (bibJsonEntry.has("author")) {
            JSONArray authors = bibJsonEntry.getJSONArray("author");
            List<String> authorList = new ArrayList<>();
            for (int i = 0; i < authors.length(); i++) {
                if (authors.getJSONObject(i).has("name")) {
                    authorList.add(authors.getJSONObject(i).getString("name"));
                } else {
                    LOGGER.info("Empty author name.");
                }
            }
            entry.setField(FieldName.AUTHOR, String.join(" and ", authorList));
        } else {
            LOGGER.info("No author found.");
        }

        // Direct accessible fields
        for (String field : singleFieldStrings) {
            if (bibJsonEntry.has(field)) {
                entry.setField(field, bibJsonEntry.getString(field));
            }
        }

        // Page numbers
        if (bibJsonEntry.has("start_page")) {
            if (bibJsonEntry.has("end_page")) {
                entry.setField(FieldName.PAGES,
                        bibJsonEntry.getString("start_page") + "--" + bibJsonEntry.getString("end_page"));
            } else {
                entry.setField(FieldName.PAGES, bibJsonEntry.getString("start_page"));
            }
        }

        // Journal
        if (bibJsonEntry.has("journal")) {
            JSONObject journal = bibJsonEntry.getJSONObject("journal");
            // Journal title
            if (journal.has("title")) {
                entry.setField(FieldName.JOURNAL, journal.getString("title"));
            } else {
                LOGGER.info("No journal title found.");
            }
            // Other journal related fields
            for (String field : journalSingleFieldStrings) {
                if (journal.has(field)) {
                    entry.setField(field, journal.getString(field));
                }
            }
        } else {
            LOGGER.info("No journal information found.");
        }

        // Keywords
        if (bibJsonEntry.has("keywords")) {
            JSONArray keywords = bibJsonEntry.getJSONArray("keywords");
            for (int i = 0; i < keywords.length(); i++) {
                if (!keywords.isNull(i)) {
                    entry.addKeyword(keywords.getString(i), keywordSeparator);
                }
            }
        }

        // Identifiers
        if (bibJsonEntry.has("identifier")) {
            JSONArray identifiers = bibJsonEntry.getJSONArray("identifier");
            for (int i = 0; i < identifiers.length(); i++) {
                String type = identifiers.getJSONObject(i).getString("type");
                if ("doi".equals(type)) {
                    entry.setField(FieldName.DOI, identifiers.getJSONObject(i).getString("id"));
                } else if ("pissn".equals(type)) {
                    entry.setField(FieldName.ISSN, identifiers.getJSONObject(i).getString("id"));
                } else if ("eissn".equals(type)) {
                    entry.setField(FieldName.ISSN, identifiers.getJSONObject(i).getString("id"));
                }
            }
        }

        // Links
        if (bibJsonEntry.has("link")) {
            JSONArray links = bibJsonEntry.getJSONArray("link");
            for (int i = 0; i < links.length(); i++) {
                if (links.getJSONObject(i).has("type")) {
                    String type = links.getJSONObject(i).getString("type");
                    if ("fulltext".equals(type) && links.getJSONObject(i).has("url")) {
                        entry.setField(FieldName.URL, links.getJSONObject(i).getString("url"));
                    }
                }
            }
        }

        return entry;
    }

    /**
     * Convert a JSONObject obtained from http://api.springer.com/metadata/json to a BibEntry
     *
     * @param springerJsonEntry the JSONObject from search results
     * @return the converted BibEntry
     */
    public static BibEntry parseSpringerJSONtoBibtex(JSONObject springerJsonEntry) {
        // Fields that are directly accessible at the top level Json object
        String[] singleFieldStrings = {FieldName.ISSN, FieldName.VOLUME, FieldName.ABSTRACT, FieldName.DOI, FieldName.TITLE, FieldName.NUMBER,
                FieldName.PUBLISHER};

        BibEntry entry = new BibEntry();
        String nametype;

        // Guess publication type
        String isbn = springerJsonEntry.optString("isbn");
        if (com.google.common.base.Strings.isNullOrEmpty(isbn)) {
            // Probably article
            entry.setType("article");
            nametype = FieldName.JOURNAL;
        } else {
            // Probably book chapter or from proceeding, go for book chapter
            entry.setType("incollection");
            nametype = FieldName.BOOKTITLE;
            entry.setField(FieldName.ISBN, isbn);
        }

        // Authors
        if (springerJsonEntry.has("creators")) {
            JSONArray authors = springerJsonEntry.getJSONArray("creators");
            List<String> authorList = new ArrayList<>();
            for (int i = 0; i < authors.length(); i++) {
                if (authors.getJSONObject(i).has("creator")) {
                    authorList.add(authors.getJSONObject(i).getString("creator"));
                } else {
                    LOGGER.info("Empty author name.");
                }
            }
            entry.setField(FieldName.AUTHOR, String.join(" and ", authorList));
        } else {
            LOGGER.info("No author found.");
        }

        // Direct accessible fields
        for (String field : singleFieldStrings) {
            if (springerJsonEntry.has(field)) {
                String text = springerJsonEntry.getString(field);
                if (!text.isEmpty()) {
                    entry.setField(field, text);
                }
            }
        }

        // Page numbers
        if (springerJsonEntry.has("startingPage") && !(springerJsonEntry.getString("startingPage").isEmpty())) {
            if (springerJsonEntry.has("endPage") && !(springerJsonEntry.getString("endPage").isEmpty())) {
                entry.setField(FieldName.PAGES,
                        springerJsonEntry.getString("startingPage") + "--" + springerJsonEntry.getString("endPage"));
            } else {
                entry.setField(FieldName.PAGES, springerJsonEntry.getString("startingPage"));
            }
        }

        // Journal
        if (springerJsonEntry.has("publicationName")) {
            entry.setField(nametype, springerJsonEntry.getString("publicationName"));
        }

        // URL
        if (springerJsonEntry.has("url")) {
            JSONArray urlarray = springerJsonEntry.optJSONArray("url");
            if (urlarray == null) {
                entry.setField(FieldName.URL, springerJsonEntry.optString("url"));
            } else {
                entry.setField(FieldName.URL, urlarray.getJSONObject(0).optString("value"));
            }
        }

        // Date
        if (springerJsonEntry.has("publicationDate")) {
            String date = springerJsonEntry.getString("publicationDate");
            entry.setField(FieldName.DATE, date); // For biblatex
            String[] dateparts = date.split("-");
            entry.setField(FieldName.YEAR, dateparts[0]);
            Optional<Month> month = Month.getMonthByNumber(Integer.parseInt(dateparts[1]));
            month.ifPresent(entry::setMonth);
        }

        // Clean up abstract (often starting with Abstract)
        entry.getField(FieldName.ABSTRACT).ifPresent(abstractContents -> {
            if (abstractContents.startsWith("Abstract")) {
                entry.setField(FieldName.ABSTRACT, abstractContents.substring(8));
            }
        });

        return entry;
    }
}
