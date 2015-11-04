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

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryType;

public class SpringerJSONConverter {

    private static final Log LOGGER = LogFactory.getLog(SpringerJSONConverter.class);

    // Fields that are directly accessible at the top level Json object
    private static final String[] singleFieldStrings = new String[] {"issn", "volume", "abstract", "doi", "title",
            "number", "publisher"};


    public SpringerJSONConverter() {

    }

    public static BibtexEntry SpringerJSONtoBibtex(JSONObject springerJsonEntry) {
        BibtexEntry entry = new BibtexEntry();
        String nametype;

        // Guess publication type
        String isbn = springerJsonEntry.optString("isbn");
        if ((isbn == null) || (isbn.length() == 0)) {
            // Probably article
            entry.setType(BibtexEntryType.getType("article"));
            nametype = "journal";
        } else {
            // Probably book chapter or from proceeding, go for book chapter
            entry.setType(BibtexEntryType.getType("incollection"));
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
            entry.setField("url", springerJsonEntry.getJSONArray("url").getJSONObject(0).optString("value"));
        }

        // Date
        if (springerJsonEntry.has("publicationDate")) {
            String date = springerJsonEntry.getString("publicationDate");
            String dateparts[] = date.split("-");
            entry.setField("year", dateparts[0]);
            entry.setField("month", dateparts[1]);
        }

        // Clean up abstract (often starting with Abstract)
        String abstr = entry.getField("abstract");
        if ((abstr != null) && abstr.startsWith("Abstract")) {
            entry.setField("abstract", abstr.substring(8));
        }

        return entry;
    }
}
