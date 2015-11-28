/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref;

import java.util.Collection;
import java.util.Vector;

import net.sf.jabref.logic.search.SearchQuery;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.importer.*;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

/**
 * @author Silberer, Zirn
 */
class SearchManagerNoGUI {

    private String searchTerm;
    private final BibtexDatabase database;
    private BibtexDatabase base;

    private static final Log LOGGER = LogFactory.getLog(SearchManagerNoGUI.class);

    public SearchManagerNoGUI(String term, BibtexDatabase dataBase) {
        searchTerm = term;
        database = dataBase;
    }

    public BibtexDatabase getDBfromMatches() {
        LOGGER.debug("Search term: " + searchTerm);
        if (specifiedYears()) {
            searchTerm = fieldYear();
        }

        SearchQuery searchQuery = new SearchQuery(searchTerm, Globals.prefs.getBoolean(JabRefPreferences.SEARCH_CASE_SENSITIVE),
                Globals.prefs.getBoolean(JabRefPreferences.SEARCH_REG_EXP));

        if (!searchQuery.isValidQuery()) {
            LOGGER.warn("Search failed: illegal search expression");
            return base;
        }

        Collection<BibtexEntry> entries = database.getEntries();
        Vector<BibtexEntry> matchEntries = new Vector<>();
        for (BibtexEntry entry : entries) {
            boolean hit = searchQuery.isMatch(entry);
            entry.setSearchHit(hit);
            if (hit) {
                matchEntries.add(entry);
            }
        }

        base = ImportFormatReader.createDatabase(matchEntries);

        return base;
    }

    private boolean specifiedYears() {
        return searchTerm.matches("year=[0-9]{4}-[0-9]{4}");
    }

    private String fieldYear() {
        String regPt1 = "";
        String regPt2 = "";
        String completeReg = null;
        boolean reg1Set = false; //if beginning of timeframe is BEFORE and end of timeframe is AFTER turn of the century
        boolean reg2Set = false;
        String[] searchTermsToPr = searchTerm.split("=");
        String field = searchTermsToPr[0];
        String[] years = searchTermsToPr[1].split("-");
        int year1 = Integer.parseInt(years[0]);
        int year2 = Integer.parseInt(years[1]);

        if ((year1 < 2000) && (year2 >= 2000)) { //for 199.
            regPt1 = "199+[" + years[0].substring(3, 4) + "-9]";
            reg1Set = true;
        } else {
            if (year1 < 2000) {
                // @formatter:off
                regPt1 = "199+[" + years[0].substring(3, 4) + "-"
                        + Math.min(Integer.parseInt(years[1].substring(3, 4)), 9) + "]";
                reg1Set = true;
                // @formatter:on
            }
        }
        if ((Integer.parseInt(years[1]) >= 2000) && (year1 < 2000)) { //for 200.
            regPt2 = "200+[0-" + years[1].substring(3, 4) + "]";
            reg2Set = true;
        } else {
            if (year2 >= 2000) {
                // @formatter:off
                regPt2 = "200+[" + years[0].substring(3, 4) + "-"
                        + Math.min(Integer.parseInt(years[1].substring(3, 4)), 9) + "]";
                // @formatter:on
                reg2Set = true;
            }
        }
        if (reg1Set && reg2Set) {
            completeReg = field + "=" + regPt1 + "|" + regPt2;
        } else {
            if (reg1Set) {
                completeReg = field + "=" + regPt1;
            }
            if (reg2Set) {
                completeReg = field + "=" + regPt2;
            }
        }

        return completeReg;
    }
}
