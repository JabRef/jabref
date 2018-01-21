package org.jabref.gui.importer.fetcher;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jabref.Globals;
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

public class DOAJFetcher implements EntryFetcher {

    private static final String SEARCH_URL = "https://doaj.org/api/v1/search/articles/";
    private static final Logger LOGGER = LoggerFactory.getLogger(DOAJFetcher.class);
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
            int numberToFetch = jo.getInt("total");
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
                            BibEntry entry = jsonConverter.parseBibJSONtoBibtex(bibJsonEntry,
                                    Globals.prefs.getKeywordDelimiter());
                            inspector.addEntry(entry);
                            fetched++;
                            inspector.setProgress(fetched, numberToFetch);
                        }
                    }
                }
                return true;
            } else {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", query),
                        Localization.lang("Search %0", getTitle()), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        } catch (UnirestException e) {
            LOGGER.error("Error while fetching from " + getTitle(), e);
            ((ImportInspectionDialog)inspector).showErrorMessage(this.getTitle(), e.getLocalizedMessage());
            return false;
        }
    }

    @Override
    public String getTitle() {
        return "DOAJ";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DOAJ;
    }

    @Override
    public JPanel getOptionsPanel() {
        // No additional options available
        return null;
    }

}
