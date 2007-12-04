package net.sf.jabref.imports;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.gui.ImportInspectionDialog;

/**
 * Fetch or search from Pubmed http://www.ncbi.nlm.nih.gov/sites/entrez/
 * 
 */
public class MedlineFetcher implements EntryFetcher {

    protected class SearchResult {

        public int count = 0;

        public int retmax = 0;

        public int retstart = 0;

        public String ids = "";

        public void addID(String id) {
            if (ids.equals(""))
                ids = id;
            else
                ids += "," + id;
        }
    }

    /**
     * How many entries to query in one request
     */
    public static final int PACING = 20;

    boolean shouldContinue;

    public String toSearchTerm(String in) {
        Pattern part1 = Pattern.compile(", ");
        Pattern part2 = Pattern.compile(",");
        Pattern part3 = Pattern.compile(" ");
        Matcher matcher;
        matcher = part1.matcher(in);
        in = matcher.replaceAll("\\+AND\\+");
        matcher = part2.matcher(in);
        in = matcher.replaceAll("\\+AND\\+");
        matcher = part3.matcher(in);
        in = matcher.replaceAll("+");

        return in;
    }

    /**
     * Gets the initial list of ids
     */
    public SearchResult getIds(String term, int start, int pacing) {

        String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
        String medlineUrl = baseUrl + "/esearch.fcgi?db=pubmed&retmax=" + Integer.toString(pacing) +
            "&retstart=" + Integer.toString(start) + "&term=";
        
        Pattern idPattern = Pattern.compile("<Id>(\\d+)</Id>");
        Pattern countPattern = Pattern.compile("<Count>(\\d+)<\\/Count>");
        Pattern retMaxPattern = Pattern.compile("<RetMax>(\\d+)<\\/RetMax>");
        Pattern retStartPattern = Pattern.compile("<RetStart>(\\d+)<\\/RetStart>");

        boolean doCount = true;
        SearchResult result = new SearchResult();
        try {
            URL ncbi = new URL(medlineUrl + term);
            // get the ids
            BufferedReader in = new BufferedReader(new InputStreamReader(ncbi.openStream()));
            String inLine;
            while ((inLine = in.readLine()) != null) {

                // get the count
                Matcher idMatcher = idPattern.matcher(inLine);
                if (idMatcher.find()) {
                    result.addID(idMatcher.group(1));
                }
                Matcher retMaxMatcher = retMaxPattern.matcher(inLine);
                if (idMatcher.find()) {
                    result.retmax = Integer.parseInt(retMaxMatcher.group(1));
                }
                Matcher retStartMatcher = retStartPattern.matcher(inLine);
                if (retStartMatcher.find()) {
                    result.retstart = Integer.parseInt(retStartMatcher.group(1));
                }
                Matcher countMatcher = countPattern.matcher(inLine);
                if (doCount && countMatcher.find()) {
                    result.count = Integer.parseInt(countMatcher.group(1));
                    doCount = false;
                }
            }
        } catch (MalformedURLException e) { // new URL() failed
            System.out.println("bad url");
            e.printStackTrace();
        } catch (IOException e) { // openConnection() failed
            System.out.println("connection failed");
            e.printStackTrace();

        }
        return result;
    }

    public void stopFetching() {
        shouldContinue = false;
    }

    public String getHelpPage() {
        return GUIGlobals.medlineHelp;
    }

    public URL getIcon() {
        return GUIGlobals.getIconUrl("www");
    }

    public String getKeyName() {
        return "Fetch Medline";
    }

    public JPanel getOptionsPanel() {
        // No Option Panel
        return null;
    }

    public String getTitle() {
        return Globals.menuTitle("Search Medline");
    }

    public void processQuery(String query, ImportInspectionDialog dialog, JabRefFrame frame) {

        shouldContinue = true;

        query = query.trim().replace(';', ',');

        if (query.matches("\\d+[,\\d+]*")) {
            frame.output(Globals.lang("Fetching Medline by id..."));
            dialog.setVisible(true);

            List<BibtexEntry> bibs = MedlineImporter.fetchMedline(query);

            if (bibs.size() == 0) {
                JOptionPane.showMessageDialog(frame, Globals.lang("No references found"));
            }
            dialog.addEntries(bibs);
            dialog.entryListComplete();
            return;
        }

        if (query.length() > 0) {
            frame.output(Globals.lang("Fetching Medline by term..."));
            dialog.setVisible(true);

            String searchTerm = toSearchTerm(query);

            // get the ids from entrez
            SearchResult result = getIds(searchTerm, 0, 1);

            if (result.count == 0) {
                JOptionPane.showMessageDialog(frame, Globals.lang("No references found"));
                return;
            }

            int numberToFetch = result.count;
            if (numberToFetch > PACING) {
                
                while (true) {
                    String strCount = JOptionPane.showInputDialog(Globals.lang("References found") +
                        ": " + numberToFetch + "  " +
                        Globals.lang("Number of references to fetch?"), Integer
                        .toString(numberToFetch));

                    if (strCount == null) {
                        dialog.dispose();
                        frame.output(Globals.lang("Medline import canceled"));
                        return;
                    }

                    try {
                        numberToFetch = Integer.parseInt(strCount.trim());
                        break;
                    } catch (RuntimeException ex) {
                        dialog.dispose();
                        frame.output("Please enter a valid number");
                    }
                }
            }

            for (int i = 0; i < numberToFetch; i += PACING) {
                if (!shouldContinue)
                    break;

                // get the ids from entrez
                result = getIds(searchTerm, i, Math.min(PACING, numberToFetch - i));

                List<BibtexEntry> bibs = MedlineImporter.fetchMedline(result.ids);
                dialog.addEntries(bibs);
                dialog.setProgress(i + PACING, numberToFetch);
            }
            dialog.entryListComplete();
            return;
        }

        JOptionPane.showMessageDialog(frame, Globals
            .lang("Please enter a comma separated list of Medline IDs (numbers) or search terms."),
            Globals.lang("Input error"), JOptionPane.ERROR_MESSAGE);
    }
}
