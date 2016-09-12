package net.sf.jabref.gui.importer.fetcher;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.importer.util.JSONEntryParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.JabRefPreferences;

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
                            BibEntry entry = jsonConverter.parseBibJSONtoBibtex(bibJsonEntry,
                                    Globals.prefs.get(JabRefPreferences.KEYWORD_SEPARATOR));
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
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DOAJ;
    }

    @Override
    public JPanel getOptionsPanel() {
        // No additional options available
        return null;
    }

}
