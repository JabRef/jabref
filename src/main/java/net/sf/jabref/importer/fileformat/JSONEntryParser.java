/*  Copyright (C) 2015 Oscar Gustafsson.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package net.sf.jabref.importer.fileformat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.MonthUtil;

public class JSONEntryParser {

    private static final Log LOGGER = LogFactory.getLog(JSONEntryParser.class);


    public JSONEntryParser() {

    }

    /**
     * Convert a JSONObject containing a bibJSON entry to a BibtexEntry
     *
     * @param bibJsonEntry The JSONObject to convert
     * @return the converted BibtexEntry
     */
    public BibtexEntry BibJSONtoBibtex(JSONObject bibJsonEntry) {
        // Fields that are directly accessible at the top level BibJson object
        String[] singleFieldStrings = {"year", "title", "abstract", "month"};

        // Fields that are accessible in the journal part of the BibJson object
        String[] journalSingleFieldStrings = {"publisher", "number", "volume"};



        BibtexEntry entry = new BibtexEntry();
        entry.setType(EntryTypes.getType("article"));

        // Authors
        if (bibJsonEntry.has("author")) {
            JSONArray authors = bibJsonEntry.getJSONArray("author");
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < authors.length(); i++) {
                if (authors.getJSONObject(i).has("name")) {
                    sb.append(authors.getJSONObject(i).getString("name"));
                    if (i < (authors.length() - 1)) {
                        sb.append(" and ");
                    }
                } else {
                    LOGGER.info("Empty author name.");
                }
            }
            entry.setField("author", sb.toString());
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
                entry.setField("pages",
                        bibJsonEntry.getString("start_page") + "--" + bibJsonEntry.getString("end_page"));
            } else {
                entry.setField("pages", bibJsonEntry.getString("start_page"));
            }
        }

        // Journal
        if (bibJsonEntry.has("journal")) {
            JSONObject journal = bibJsonEntry.getJSONObject("journal");
            // Journal title
            if (journal.has("title")) {
                entry.setField("journal", journal.getString("title"));
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
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < keywords.length(); i++) {
                if (!keywords.isNull(i)) {
                    sb.append(keywords.getString(i));
                    if (i < (keywords.length() - 1)) {
                        sb.append(", ");
                    }
                }
            }
            entry.setField("keywords", sb.toString());
        }

        // Identifiers
        if (bibJsonEntry.has("identifier")) {
            JSONArray identifiers = bibJsonEntry.getJSONArray("identifier");
            for (int i = 0; i < identifiers.length(); i++) {
                String type = identifiers.getJSONObject(i).getString("type");
                if ("doi".equals(type)) {
                    entry.setField("doi", identifiers.getJSONObject(i).getString("id"));
                } else if ("pissn".equals(type)) {
                    entry.setField("issn", identifiers.getJSONObject(i).getString("id"));
                } else if ("eissn".equals(type)) {
                    entry.setField("issn", identifiers.getJSONObject(i).getString("id"));
                }
            }
        }

        // Links
        if (bibJsonEntry.has("link")) {
            JSONArray links = bibJsonEntry.getJSONArray("link");
            for (int i = 0; i < links.length(); i++) {
                if (links.getJSONObject(i).has("type")) {
                    String type = links.getJSONObject(i).getString("type");
                    if ("fulltext".equals(type)) {
                        if (links.getJSONObject(i).has("url")) {
                            entry.setField("url", links.getJSONObject(i).getString("url"));
                        }
                    }
                }
            }
        }

        return entry;
    }

    /**
     * Convert a JSONObject obtained from http://api.springer.com/metadata/json to a BibtexEntry
     *
     * @param springerJsonEntry the JSONObject from search results
     * @return the converted BibtexEntry
     */
    public static BibtexEntry SpringerJSONtoBibtex(JSONObject springerJsonEntry) {
        // Fields that are directly accessible at the top level Json object
        String[] singleFieldStrings = {"issn", "volume", "abstract", "doi", "title", "number",
                "publisher"};

        BibtexEntry entry = new BibtexEntry();
        String nametype;

        // Guess publication type
        String isbn = springerJsonEntry.optString("isbn");
        if ((isbn == null) || (isbn.length() == 0)) {
            // Probably article
            entry.setType(EntryTypes.getType("article"));
            nametype = "journal";
        } else {
            // Probably book chapter or from proceeding, go for book chapter
            entry.setType(EntryTypes.getType("incollection"));
            nametype = "booktitle";
            entry.setField("isbn", isbn);
        }

        // Authors
        if (springerJsonEntry.has("creators")) {
            JSONArray authors = springerJsonEntry.getJSONArray("creators");
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < authors.length(); i++) {
                if (authors.getJSONObject(i).has("creator")) {
                    sb.append(authors.getJSONObject(i).getString("creator"));
                    if (i < (authors.length() - 1)) {
                        sb.append(" and ");
                    }
                } else {
                    LOGGER.info("Empty author name.");
                }
            }
            entry.setField("author", sb.toString());
        } else {
            LOGGER.info("No author found.");
        }

        // Direct accessible fields
        for (String field : singleFieldStrings) {
            if (springerJsonEntry.has(field)) {
                String text = springerJsonEntry.getString(field);
                if (text.length() > 0) {
                    entry.setField(field, text);
                }
            }
        }

        // Page numbers
        if (springerJsonEntry.has("startingPage") && (springerJsonEntry.getString("startingPage").length() > 0)) {
            if (springerJsonEntry.has("endPage") && (springerJsonEntry.getString("endPage").length() > 0)) {
                entry.setField("pages",
                        springerJsonEntry.getString("startingPage") + "--" + springerJsonEntry.getString("endPage"));
            } else {
                entry.setField("pages", springerJsonEntry.getString("startingPage"));
            }
        }

        // Journal
        if (springerJsonEntry.has("publicationName")) {
            entry.setField(nametype, springerJsonEntry.getString("publicationName"));
        }

        // URL
        if (springerJsonEntry.has("url")) {
            JSONArray urlarray = springerJsonEntry.optJSONArray("url");
            if (urlarray != null) {
                entry.setField("url", urlarray.getJSONObject(0).optString("value"));
            } else {
                entry.setField("url", springerJsonEntry.optString("url"));
            }
        }

        // Date
        if (springerJsonEntry.has("publicationDate")) {
            String date = springerJsonEntry.getString("publicationDate");
            entry.setField("date", date); // For BibLatex
            String dateparts[] = date.split("-");
            entry.setField("year", dateparts[0]);
            entry.setField("month", MonthUtil.getMonthByNumber(Integer.parseInt(dateparts[1])).bibtexFormat);
        }

        // Clean up abstract (often starting with Abstract)
        String abstr = entry.getField("abstract");
        if ((abstr != null) && abstr.startsWith("Abstract")) {
            entry.setField("abstract", abstr.substring(8));
        }

        return entry;
    }
}
