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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
import net.sf.jabref.model.entry.BibEntry;

public class SpringerFetcher implements EntryFetcher {

    private static final String API_URL = "http://api.springer.com/metadata/json?q=";
    private static final String API_KEY = "b0c7151179b3d9c1119cf325bca8460d";
    private static final Log LOGGER = LogFactory.getLog(SpringerFetcher.class);
    private static final int MAX_PER_PAGE = 100;
    private boolean shouldContinue;


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
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            jsonResponse = Unirest.get(API_URL + encodedQuery + "&api_key=" + API_KEY + "&p=1")
                    .header("accept", "application/json")
                    .asJson();
            JSONObject jo = jsonResponse.getBody().getObject();
            int hits = jo.getJSONArray("result").getJSONObject(0).getInt("total");
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
                            status.setStatus(Localization.lang("%0 import canceled", "Springer"));
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
                for (int startItem = 1; startItem <= numberToFetch; startItem += MAX_PER_PAGE) {
                    if (!shouldContinue) {
                        break;
                    }

                    int noToFetch = Math.min(MAX_PER_PAGE, numberToFetch - startItem);
                    jsonResponse = Unirest
                            .get(API_URL + encodedQuery + "&api_key=" + API_KEY + "&p=" + noToFetch + "&s=" + startItem)
                            .header("accept", "application/json").asJson();
                    jo = jsonResponse.getBody().getObject();
                    if (jo.has("records")) {
                        JSONArray results = jo.getJSONArray("records");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject springerJsonEntry = results.getJSONObject(i);
                            BibEntry entry = JSONEntryParser.parseSpringerJSONtoBibtex(springerJsonEntry);
                            inspector.addEntry(entry);
                            fetched++;
                            inspector.setProgress(fetched, numberToFetch);
                        }
                    }
                }
                return true;
            } else {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", encodedQuery),
                        Localization.lang("Search %0", "Springer"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        } catch (UnirestException e) {
            LOGGER.warn("Problem searching Springer", e);
        } catch (UnsupportedEncodingException e) {
            LOGGER.warn("Cannot encode query", e);
        }
        return false;

    }

    @Override
    public String getTitle() {
        return "Springer";
    }

    @Override
    public String getHelpPage() {
        return "SpringerHelp";
    }

    @Override
    public JPanel getOptionsPanel() {
        // No additional options available
        return null;
    }
}
