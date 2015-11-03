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

public class BibJSONConverter {

    private static final Log LOGGER = LogFactory.getLog(BibJSONConverter.class);

    private static final String[] singleFieldStrings = new String[] {"year", "title", "abstract", "month"};
    private static final String[] journalSingleFieldStrings = new String[] {"publisher", "number", "volume"};


    public BibJSONConverter() {

    }

    public static BibtexEntry BibJSONtoBibtex(JSONObject bibJsonEntry) {
        BibtexEntry entry = new BibtexEntry();
        entry.setType(BibtexEntryType.getType("article"));

        // Authors
        JSONArray authors = bibJsonEntry.getJSONArray("author");
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < authors.length(); i++) {
            sb.append(authors.getJSONObject(i).getString("name"));
            if (i < (authors.length() - 1)) {
                sb.append(" and ");
            }
        }
        entry.setField("author", sb.toString());

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
        JSONObject journal = bibJsonEntry.getJSONObject("journal");
        entry.setField("journal", journal.getString("title"));
        for (String field : journalSingleFieldStrings) {
            if (journal.has(field)) {
                entry.setField(field, journal.getString(field));
            }
        }

        // Keywords
        if (bibJsonEntry.has("keywords")) {
            JSONArray keywords = bibJsonEntry.getJSONArray("keywords");
            sb = new StringBuffer();
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
                if (type.equals("doi")) {
                    entry.setField("doi", identifiers.getJSONObject(i).getString("id"));
                }
            }
        }

        // Links
        if (bibJsonEntry.has("link")) {
            JSONArray links = bibJsonEntry.getJSONArray("link");
            for (int i = 0; i < links.length(); i++) {
                String type = links.getJSONObject(i).getString("type");
                if (type.equals("fulltext")) {
                    entry.setField("url", links.getJSONObject(i).getString("url"));
                }
            }
        }

        return entry;
    }
}
