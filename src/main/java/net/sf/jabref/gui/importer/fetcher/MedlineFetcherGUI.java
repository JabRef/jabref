package net.sf.jabref.gui.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.MedlineFetcher;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

public class MedlineFetcherGUI extends MedlineFetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_MEDLINE;
    }

    private static String toSearchTerm(String in) {
        // This can probably be simplified using simple String.replace()...
        String result = in;
        Matcher matcher;
        matcher = getPart1Pattern().matcher(result);
        result = matcher.replaceAll("\\+AND\\+");
        matcher = getPart2Pattern().matcher(result);
        result = matcher.replaceAll("\\+AND\\+");
        result = result.replace(" ", "+");
        return result;
    }

    /**
     * Gets the initial list of ids
     */
    private SearchResult getIds(String term, int start, int pacing) {

        String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
        String medlineUrl = baseUrl + "/esearch.fcgi?db=pubmed&retmax=" + Integer.toString(pacing) + "&retstart="
                + Integer.toString(start) + "&term=";

        boolean doCount = true;
        SearchResult result = new SearchResult();
        try {
            URL ncbi = new URL(medlineUrl + term);
            // get the ids
            BufferedReader in = new BufferedReader(new InputStreamReader(ncbi.openStream()));
            String inLine;
            while ((inLine = in.readLine()) != null) {

                // get the count
                Matcher idMatcher = getIdPattern().matcher(inLine);
                if (idMatcher.find()) {
                    result.addID(idMatcher.group(1));
                }
                Matcher retMaxMatcher = getRetMaxPattern().matcher(inLine);
                if (retMaxMatcher.find()) {
                    result.retmax = Integer.parseInt(retMaxMatcher.group(1));
                }
                Matcher retStartMatcher = getRetStartPattern().matcher(inLine);
                if (retStartMatcher.find()) {
                    result.retstart = Integer.parseInt(retStartMatcher.group(1));
                }
                Matcher countMatcher = getCountPattern().matcher(inLine);
                if (doCount && countMatcher.find()) {
                    result.count = Integer.parseInt(countMatcher.group(1));
                    doCount = false;
                }
            }
        } catch (MalformedURLException e) { // new URL() failed
            getLogger().warn("Bad url", e);
        } catch (IOException e) { // openConnection() failed
            getLogger().warn("Connection failed", e);
        }
        return result;
    }

    @Override
    public boolean processQuery(String query, ImportInspector iIDialog, OutputPrinter frameOP) {

        setShouldContinue(true);

        String cleanQuery = query.trim().replace(';', ',');

        if (cleanQuery.matches("\\d+[,\\d+]*")) {
            frameOP.setStatus(Localization.lang("Fetching Medline by id..."));

            List<BibEntry> bibs = fetchMedline(cleanQuery, frameOP);

            if (bibs.isEmpty()) {
                frameOP.showMessage(Localization.lang("No references found"));
            }

            for (BibEntry entry : bibs) {
                iIDialog.addEntry(entry);
            }
            return true;
        }

        if (!query.isEmpty()) {
            frameOP.setStatus(Localization.lang("Fetching Medline by term..."));

            String searchTerm = toSearchTerm(query);

            // get the ids from entrez
            SearchResult result = getIds(searchTerm, 0, 1);

            if (result.count == 0) {
                frameOP.showMessage(Localization.lang("No references found"));
                return false;
            }

            int numberToFetch = result.count;
            if (numberToFetch > getPacing()) {

                while (true) {
                    String strCount = JOptionPane.showInputDialog(
                            Localization.lang("References found") + ": " + numberToFetch + "  "
                                    + Localization.lang("Number of references to fetch?"),
                            Integer.toString(numberToFetch));

                    if (strCount == null) {
                        frameOP.setStatus(Localization.lang("%0 import canceled", "Medline"));
                        return false;
                    }

                    try {
                        numberToFetch = Integer.parseInt(strCount.trim());
                        break;
                    } catch (NumberFormatException ex) {
                        frameOP.showMessage(Localization.lang("Please enter a valid number"));
                    }
                }
            }

            for (int i = 0; i < numberToFetch; i += getPacing()) {
                if (!isShouldContinue()) {
                    break;
                }

                int noToFetch = Math.min(getPacing(), numberToFetch - i);

                // get the ids from entrez
                result = getIds(searchTerm, i, noToFetch);

                List<BibEntry> bibs = fetchMedline(result.ids, frameOP);
                for (BibEntry entry : bibs) {
                    iIDialog.addEntry(entry);
                }
                iIDialog.setProgress(i + noToFetch, numberToFetch);
            }
            return true;
        }
        frameOP.showMessage(
                Localization.lang("Please enter a comma separated list of Medline IDs (numbers) or search terms."),
                Localization.lang("Input error"), JOptionPane.ERROR_MESSAGE);
        return false;
    }

}
