/*  Copyright (C) 2003-2015 JabRef contributors.
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

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fileformat.MedlineImporter;
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
    private SearchResult getIds(String term, int start, int pacing) {

        String baseUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils";
        String medlineUrl = baseUrl + "/esearch.fcgi?db=pubmed&retmax=" + Integer.toString(pacing) +
                "&retstart=" + Integer.toString(start) + "&term=";


        boolean doCount = true;
        SearchResult result = new SearchResult();
        try {
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
        } catch (MalformedURLException e) { // new URL() failed
            LOGGER.warn("Bad url", e);
        } catch (IOException e) { // openConnection() failed
            LOGGER.warn("Connection failed", e);
        }
        return result;
    }

    @Override
    public void stopFetching() {
        shouldContinue = false;
    }

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_MEDLINE;
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
    public boolean processQuery(String query, ImportInspector iIDialog, OutputPrinter frameOP) {

        shouldContinue = true;

        String cleanQuery = query.trim().replace(';', ',');

        if (cleanQuery.matches("\\d+[,\\d+]*")) {
            frameOP.setStatus(Localization.lang("Fetching Medline by id..."));

            List<BibEntry> bibs = MedlineImporter.fetchMedline(cleanQuery, frameOP);

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
            if (numberToFetch > MedlineFetcher.PACING) {

                while (true) {
                    String strCount = JOptionPane.showInputDialog(Localization.lang("References found") +
                            ": " + numberToFetch + "  " +
                            Localization.lang("Number of references to fetch?"), Integer
                            .toString(numberToFetch));

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

            for (int i = 0; i < numberToFetch; i += MedlineFetcher.PACING) {
                if (!shouldContinue) {
                    break;
                }

                int noToFetch = Math.min(MedlineFetcher.PACING, numberToFetch - i);

                // get the ids from entrez
                result = getIds(searchTerm, i, noToFetch);

                List<BibEntry> bibs = MedlineImporter.fetchMedline(result.ids, frameOP);
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
