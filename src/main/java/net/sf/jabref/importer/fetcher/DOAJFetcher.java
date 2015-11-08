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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.JSONEntryParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;

public class DOAJFetcher implements EntryFetcher {

    private final String searchURL = "https://doaj.org/api/v1/search/articles/";
    private static final Log LOGGER = LogFactory.getLog(DOAJFetcher.class);
    private final int maxPerPage = 100;
    private boolean shouldContinue;


    private final JSONEntryParser jsonConverter = new JSONEntryParser();

    public DOAJFetcher() {
        super();
    }

    @Override
    public void stopFetching() {
        shouldContinue = false;
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        shouldContinue = true;
        try {
            status.setStatus(Localization.lang("Searching DOAJ..."));
            HttpResponse<JsonNode> jsonResponse;
            jsonResponse = Unirest.get(searchURL + query + "?pageSize=1").header("accept", "application/json").asJson();
            JSONObject jo = jsonResponse.getBody().getObject();
            int hits = jo.getInt("total");
            int numberToFetch = 0;
            if (hits > 0) {
                if (hits > maxPerPage) {
                    while (true) {
                        String strCount = JOptionPane
                                .showInputDialog(
                                        Localization.lang("References found") + ": " + hits + "  "
                                                + Localization.lang("Number of references to fetch?"),
                                        Integer.toString(hits));

                        if (strCount == null) {
                            status.setStatus(Localization.lang("DOAJ search canceled"));
                            return false;
                        }

                        try {
                            numberToFetch = Integer.parseInt(strCount.trim());
                            break;
                        } catch (RuntimeException ex) {
                            status.showMessage(Localization.lang("Please enter a valid number"));
                        }
                    }
                } else {
                    numberToFetch = hits;
                }

                int fetched = 0; // Keep track of number of items fetched for the progress bar
                for (int page = 1; ((page - 1) * maxPerPage) <= numberToFetch; page++) {
                    if (!shouldContinue) {
                        break;
                    }

                    int noToFetch = Math.min(maxPerPage, numberToFetch - ((page - 1) * maxPerPage));
                    jsonResponse = Unirest.get(searchURL + query + "?page=" + page + "&pageSize=" + noToFetch)
                            .header("accept", "application/json").asJson();
                    jo = jsonResponse.getBody().getObject();
                    if (jo.has("results")) {
                        JSONArray results = jo.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject bibJsonEntry = results.getJSONObject(i).getJSONObject("bibjson");
                            BibtexEntry entry = jsonConverter.BibJSONtoBibtex(bibJsonEntry);
                            inspector.addEntry(entry);
                            fetched++;
                            inspector.setProgress(fetched, numberToFetch);
                        }
                    }
                }
                return true;
            } else {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", query),
                        Localization.lang("Search DOAJ"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        } catch (UnirestException e) {
            LOGGER.warn("Problem searching DOAJ", e);
            return false;
        }

    }

    @Override
    public String getTitle() {
        return "DOAJ (Directory of Open Access Journals)";
    }

    @Override
    public String getKeyName() {
        return "DOAJ";
    }

    @Override
    public String getHelpPage() {
        return "DOAJHelp.html";
    }

    @Override
    public JPanel getOptionsPanel() {
        // No additional options available
        return null;
    }

}
