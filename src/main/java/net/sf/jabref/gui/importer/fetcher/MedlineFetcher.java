package net.sf.jabref.gui.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.gui.importer.ImportInspectionDialog;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.MedlineImporter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Fetch or search from Pubmed http://www.ncbi.nlm.nih.gov/sites/entrez/
 *
 */
public class MedlineFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(MedlineFetcher.class);

    private static final Pattern PART1_PATTERN = Pattern.compile(", ");
    private static final Pattern PART2_PATTERN = Pattern.compile(",");

    private static final Pattern ID_PATTERN = Pattern.compile("<Id>(\\d+)</Id>");
    private static final Pattern COUNT_PATTERN = Pattern.compile("<Count>(\\d+)<\\/Count>");
    private static final Pattern RET_MAX_PATTERN = Pattern.compile("<RetMax>(\\d+)<\\/RetMax>");
    private static final Pattern RET_START_PATTERN = Pattern.compile("<RetStart>(\\d+)<\\/RetStart>");



    /**
     * How many entries to query in one request
     */
    private static final int PACING = 20;

    private boolean shouldContinue;
    private static String toSearchTerm(String in) {
        // This can probably be simplified using simple String.replace()...
        String result = in;
        Matcher matcher;
        matcher = PART1_PATTERN.matcher(result);
        result = matcher.replaceAll("\\+AND\\+");
        matcher = PART2_PATTERN.matcher(result);
        result = matcher.replaceAll("\\+AND\\+");
        result = result.replace(" ", "+");
        return result;
    }

    /**
     * Gets the initial list of ids
     */
    private SearchResult getIds(String term, int start, int pacing) throws IOException {
        String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
        String medlineUrl = baseUrl + "/esearch.fcgi?db=pubmed&retmax=" + Integer.toString(pacing) +
                "&retstart=" + Integer.toString(start) + "&term=";

        boolean doCount = true;
        SearchResult result = new SearchResult();

        URL ncbi = new URL(medlineUrl + term);
        // get the ids
        BufferedReader in = new BufferedReader(new InputStreamReader(ncbi.openStream()));
        String inLine;
        while ((inLine = in.readLine()) != null) {

            // get the count
            Matcher idMatcher = ID_PATTERN.matcher(inLine);
            if (idMatcher.find()) {
                result.addID(idMatcher.group(1));
            }
            Matcher retMaxMatcher = RET_MAX_PATTERN.matcher(inLine);
            if (retMaxMatcher.find()) {
                result.retmax = Integer.parseInt(retMaxMatcher.group(1));
            }
            Matcher retStartMatcher = RET_START_PATTERN.matcher(inLine);
            if (retStartMatcher.find()) {
                result.retstart = Integer.parseInt(retStartMatcher.group(1));
            }
            Matcher countMatcher = COUNT_PATTERN.matcher(inLine);
            if (doCount && countMatcher.find()) {
                result.count = Integer.parseInt(countMatcher.group(1));
                doCount = false;
            }
        }
        return result;
    }

    @Override
    public void stopFetching() {
        shouldContinue = false;
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_MEDLINE;
    }

    @Override
    public JPanel getOptionsPanel() {
        // No Option Panel
        return null;
    }

    @Override
    public String getTitle() {
        return "Medline";
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        try {
            shouldContinue = true;

            String cleanQuery = query.trim().replace(';', ',');
            if (cleanQuery.matches("\\d+[,\\d+]*")) {
                status.setStatus(Localization.lang("Fetching Medline by id..."));

                List<BibEntry> bibs = fetchMedline(cleanQuery, status);
                if (bibs.isEmpty()) {
                    status.showMessage(Localization.lang("No references found"),
                            Localization.lang("Search %0", getTitle()), JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                for (BibEntry entry : bibs) {
                    dialog.addEntry(entry);
                }
                return true;
            }

            if (!query.isEmpty()) {
                status.setStatus(Localization.lang("Fetching Medline by term..."));

                String searchTerm = toSearchTerm(query);

                // get the ids from entrez
                SearchResult result = getIds(searchTerm, 0, 1);

                if (result.count == 0) {
                    status.showMessage(Localization.lang("No references found"),
                            Localization.lang("Search %0", getTitle()), JOptionPane.INFORMATION_MESSAGE);
                    return false;
                }

                int numberToFetch = result.count;
                if (numberToFetch > MedlineFetcher.PACING) {
                    boolean numberEntered = false;
                    do {
                        String strCount = JOptionPane.showInputDialog(Localization.lang("References found") + ": " + numberToFetch + " " +
                                Localization.lang("Number of references to fetch?"), Integer.toString(numberToFetch));

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

                for (int i = 0; i < numberToFetch && shouldContinue; i += MedlineFetcher.PACING) {
                    int noToFetch = Math.min(MedlineFetcher.PACING, numberToFetch - i);

                    // get the ids from entrez
                    result = getIds(searchTerm, i, noToFetch);

                    List<BibEntry> bibs = fetchMedline(result.ids, status);
                    for (BibEntry entry : bibs) {
                        dialog.addEntry(entry);
                    }
                    dialog.setProgress(i + noToFetch, numberToFetch);
                }
                return true;
            }
            else {
                status.showMessage(
                        Localization.lang("Please enter a comma separated list of Medline IDs (numbers) or search terms."),
                        Localization.lang("Search %0", getTitle()) + ": " + Localization.lang("Input error"), JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (IOException e) {
            LOGGER.error("Error while fetching from " + getTitle(), e);
            status.showMessage(e.getLocalizedMessage());
            ((ImportInspectionDialog)dialog).showErrorMessage(this.getTitle(), e.getLocalizedMessage());
        }
        return false;
    }

    /**
     * Fetch and parse an medline item from eutils.ncbi.nlm.nih.gov.
     *
     * @param id One or several ids, separated by ","
     */
    private static List<BibEntry> fetchMedline(String id, OutputPrinter status) throws IOException {
        String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&rettype=citation&id=" +
                id;
        URL url = new URL(baseUrl);
        URLConnection data = url.openConnection();
        ParserResult result = new MedlineImporter().importDatabase(
                new BufferedReader(new InputStreamReader(data.getInputStream(), StandardCharsets.UTF_8)));
        if (result.hasWarnings()) {
            status.showMessage(result.getErrorMessage());
        }
        return result.getDatabase().getEntries();
    }

    static class SearchResult {

        public int count;

        public int retmax;

        public int retstart;

        public String ids = "";


        public void addID(String id) {
            if (ids.isEmpty()) {
                ids = id;
            } else {
                ids += "," + id;
            }
        }
    }

}
