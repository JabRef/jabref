package net.sf.jabref.gui.importer.fetcher;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.SpringerFetcher;
import net.sf.jabref.importer.fileformat.JSONEntryParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

public class SpringerFetcherGUI extends SpringerFetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_SPRINGER;
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        setShouldContinue(true);
        try {
            status.setStatus(Localization.lang("Searching..."));
            HttpResponse<JsonNode> jsonResponse;
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            jsonResponse = Unirest.get(getApiUrl() + encodedQuery + "&api_key=" + getApiKey() + "&p=1")
                    .header("accept", "application/json").asJson();

            JSONObject jsonObject = jsonResponse.getBody().getObject();
            int hits = jsonObject.getJSONArray("result").getJSONObject(0).getInt("total");
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
                            status.setStatus(Localization.lang("%0 import canceled", getTitle()));
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
                for (int startItem = 1; startItem <= numberToFetch; startItem += getMaxPerPage()) {
                    if (!shouldContinue()) {
                        break;
                    }

                    int noToFetch = Math.min(getMaxPerPage(), numberToFetch - startItem);
                    jsonResponse = Unirest.get(getApiUrl() + encodedQuery + "&api_key=" + getApiKey() + "&p="
                            + noToFetch + "&s=" + startItem).header("accept", "application/json").asJson();
                    jsonObject = jsonResponse.getBody().getObject();
                    if (jsonObject.has("records")) {
                        JSONArray results = jsonObject.getJSONArray("records");
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
        } catch (UnirestException e) {
            getLogger().warn("Problem searching Springer", e);
        } catch (UnsupportedEncodingException e) {
            getLogger().warn("Cannot encode query", e);
        }
        return false;

    }

}
