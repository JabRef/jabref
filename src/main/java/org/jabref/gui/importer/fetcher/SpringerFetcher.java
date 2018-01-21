package org.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.net.URLEncoder;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.ImportInspector;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.importer.util.JSONEntryParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpringerFetcher implements EntryFetcher {

    private static final String API_URL = "http://api.springer.com/metadata/json?q=";
    private static final String API_KEY = "b0c7151179b3d9c1119cf325bca8460d";
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringerFetcher.class);
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
            int numberToFetch = jo.getJSONArray("result").getJSONObject(0).getInt("total");
            if (numberToFetch > 0) {
                if (numberToFetch > MAX_PER_PAGE) {
                    boolean numberEntered = false;
                    do {
                        String strCount = JOptionPane.showInputDialog(Localization.lang("%0 references found. Number of references to fetch?", String.valueOf(numberToFetch)));

                        if (strCount == null) {
                            status.setStatus(Localization.lang("%0 import canceled", getTitle()));
                            return false;
                        }

                        try {
                            numberToFetch = Integer.parseInt(strCount.trim());
                            numberEntered = true;
                        } catch (NumberFormatException ex) {
                            status.showMessage(Localization.lang("Please enter a valid number"));
                        }
                    } while (!numberEntered);
                }

                int fetched = 0; // Keep track of number of items fetched for the progress bar
                for (int startItem = 1; startItem <= numberToFetch; startItem += MAX_PER_PAGE) {
                    if (!shouldContinue) {
                        break;
                    }

                    int noToFetch = Math.min(MAX_PER_PAGE, (numberToFetch - startItem) + 1);
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
                        Localization.lang("Search %0", getTitle()), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        } catch (IOException | UnirestException e) {
            LOGGER.error("Error while fetching from " + getTitle(), e);
            ((ImportInspectionDialog)inspector).showErrorMessage(this.getTitle(), e.getLocalizedMessage());
        }
        return false;

    }

    @Override
    public String getTitle() {
        return "Springer";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_SPRINGER;
    }

    @Override
    public JPanel getOptionsPanel() {
        // No additional options available
        return null;
    }
}
