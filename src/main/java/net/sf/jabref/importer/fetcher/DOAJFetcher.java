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
package net.sf.jabref.importer.fetcher;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.JSONEntryParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class DOAJFetcher implements EntryFetcher {

    private static final String SEARCH_URL = "https://doaj.org/api/v1/search/articles/";
    private static final Log LOGGER = LogFactory.getLog(DOAJFetcher.class);
    private static final int MAX_PER_PAGE = 100;
    private boolean shouldContinue;


    private final JSONEntryParser jsonConverter = new JSONEntryParser();

    @Override
    public void stopFetching() {
        shouldContinue = false;
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        shouldContinue = true;
        try {
            status.setStatus(Localization.lang("Searching..."));
            HttpResponse<JsonNode> jsonResponse;
            jsonResponse = Unirest.get(SEARCH_URL + query + "?pageSize=1").header("accept", "application/json").asJson();
            JSONObject jo = jsonResponse.getBody().getObject();
            int hits = jo.getInt("total");
            int numberToFetch = 0;
            if (hits > 0) {
                if (hits > MAX_PER_PAGE) {
                    while (true) {
                        String strCount = JOptionPane
                                .showInputDialog(
                                        Localization.lang("References found") + ": " + hits + "  "
                                                + Localization.lang("Number of references to fetch?"),
                                        Integer.toString(hits));

                        if (strCount == null) {
                            status.setStatus(Localization.lang("%0 import canceled", "DOAJ"));
                            return false;
                        }

                        try {
                            numberToFetch = Integer.parseInt(strCount.trim());
                            break;
                        } catch (NumberFormatException ex) {
                            status.showMessage(Localization.lang("Please enter a valid number"));
                        }
                    }
                } else {
                    numberToFetch = hits;
                }

                int fetched = 0; // Keep track of number of items fetched for the progress bar
                for (int page = 1; ((page - 1) * MAX_PER_PAGE) <= numberToFetch; page++) {
                    if (!shouldContinue) {
                        break;
                    }

                    int noToFetch = Math.min(MAX_PER_PAGE, numberToFetch - ((page - 1) * MAX_PER_PAGE));
                    jsonResponse = Unirest.get(SEARCH_URL + query + "?page=" + page + "&pageSize=" + noToFetch)
                            .header("accept", "application/json").asJson();
                    jo = jsonResponse.getBody().getObject();
                    if (jo.has("results")) {
                        JSONArray results = jo.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject bibJsonEntry = results.getJSONObject(i).getJSONObject("bibjson");
                            BibEntry entry = jsonConverter.parseBibJSONtoBibtex(bibJsonEntry);
                            inspector.addEntry(entry);
                            fetched++;
                            inspector.setProgress(fetched, numberToFetch);
                        }
                    }
                }
                return true;
            } else {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", query),
                        Localization.lang("Search %0", "DOAJ"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        } catch (UnirestException e) {
            LOGGER.warn("Problem searching DOAJ", e);
            status.setStatus(Localization.lang("%0 import canceled", "DOAJ"));
            return false;
        }

    }

    @Override
    public String getTitle() {
        return "DOAJ";
    }

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_DOAJ;
    }

    @Override
    public JPanel getOptionsPanel() {
        // No additional options available
        return null;
    }

}
