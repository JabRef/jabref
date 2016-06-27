package net.sf.jabref.gui.importer.fetcher;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.DOAJFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

public class DOAJFetcherGUI extends DOAJFetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_DOAJ;
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        setShouldContinue(true);
        try {
            status.setStatus(Localization.lang("Searching..."));
            HttpResponse<JsonNode> jsonResponse;
            jsonResponse = Unirest.get(getSearchUrl() + query + "?pageSize=1").header("accept", "application/json")
                    .asJson();
            JSONObject jo = jsonResponse.getBody().getObject();
            int hits = jo.getInt("total");
            int numberToFetch = 0;
            if (hits > 0) {
                if (hits > getMaxPerPage()) {
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
                for (int page = 1; ((page - 1) * getMaxPerPage()) <= numberToFetch; page++) {
                    if (!isShouldContinue()) {
                        break;
                    }

                    int noToFetch = Math.min(getMaxPerPage(), numberToFetch - ((page - 1) * getMaxPerPage()));
                    jsonResponse = Unirest.get(getSearchUrl() + query + "?page=" + page + "&pageSize=" + noToFetch)
                            .header("accept", "application/json").asJson();
                    jo = jsonResponse.getBody().getObject();
                    if (jo.has("results")) {
                        JSONArray results = jo.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject bibJsonEntry = results.getJSONObject(i).getJSONObject("bibjson");
                            BibEntry entry = getJsonConverter().parseBibJSONtoBibtex(bibJsonEntry);
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
            getLogger().warn("Problem searching DOAJ", e);
            status.setStatus(Localization.lang("%0 import canceled", "DOAJ"));
            return false;
        }

    }
}
