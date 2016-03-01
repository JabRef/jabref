/*  Copyright (C) 2003-2015 JabRef Contributors
    Copyright (C) 2003-2011 Aaron Chen
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

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.FetcherPreviewDialog;
import net.sf.jabref.logic.formatter.bibtexfields.UnitFormatter;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeper;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ACMPortalFetcher implements PreviewEntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(ACMPortalFetcher.class);

    private final HTMLConverter htmlConverter = new HTMLConverter();
    private final CaseKeeper caseKeeper = new CaseKeeper();
    private final UnitFormatter unitFormatter = new UnitFormatter();
    private String terms;

    private static final String START_URL = "http://portal.acm.org/";
    private static final String SEARCH_URL_PART = "results.cfm?query=";
    private static final String SEARCH_URL_PART_II = "&dl=";
    private static final String END_URL = "&coll=Portal&short=0";//&start=";

    private static final String BIBTEX_URL = "exportformats.cfm?id=";
    private static final String BIBTEX_URL_END = "&expformat=bibtex";
    private static final String ABSTRACT_URL = "tab_abstract.cfm?id=";

    private final JRadioButton acmButton = new JRadioButton(Localization.lang("The ACM Digital Library"));
    private final JRadioButton guideButton = new JRadioButton(Localization.lang("The Guide to Computing Literature"));
    private final JCheckBox absCheckBox = new JCheckBox(Localization.lang("Include abstracts"), false);

    private static final int PER_PAGE = 20; // Fetch only one page. Otherwise, the user will get blocked by ACM. 100 has been the old setting. See Bug 3532752 - https://sourceforge.net/tracker/index.php?func=detail&aid=3532752&group_id=92314&atid=600306
    private static final int WAIT_TIME = 200;
    private boolean shouldContinue;

    // user settings
    private boolean fetchAbstract;
    private boolean acmOrGuide;

    private int piv;

    private static final Pattern HITS_PATTERN = Pattern.compile("<strong>(\\d+,*\\d*)</strong> results found");
    private static final Pattern MAX_HITS_PATTERN = Pattern
            .compile("Result \\d+,*\\d* &ndash; \\d+,*\\d* of (\\d+,*\\d*)");

    private static final Pattern FULL_CITATION_PATTERN = Pattern.compile("<a href=\"(citation.cfm.*)\" target.*");

    private static final Pattern ID_PATTERN = Pattern.compile("citation.cfm\\?id=(\\d+)&.*");

    // Patterns used to extract information for the preview:
    private static final Pattern TITLE_PATTERN = Pattern.compile("<a href=.*?\">([^<]*)</a>");
    private static final Pattern ABSTRACT_PATTERN = Pattern.compile("<div .*?>(.*?)</div>");
    private static final Pattern SOURCE_PATTERN = Pattern.compile("<span style=\"padding-left:10px\">([^<]*)</span>");


    @Override
    public JPanel getOptionsPanel() {
        JPanel pan = new JPanel();
        pan.setLayout(new GridLayout(0, 1));

        guideButton.setSelected(true);

        ButtonGroup group = new ButtonGroup();
        group.add(acmButton);
        group.add(guideButton);

        pan.add(absCheckBox);
        pan.add(acmButton);
        pan.add(guideButton);

        return pan;
    }

    @Override
    public boolean processQueryGetPreview(String query, FetcherPreviewDialog preview, OutputPrinter status) {
        this.terms = query;
        piv = 0;
        shouldContinue = true;
        acmOrGuide = acmButton.isSelected();
        fetchAbstract = absCheckBox.isSelected();
        String address = makeUrl();
        LinkedHashMap<String, JLabel> previews = new LinkedHashMap<>();

        try {
            URL url = new URL(address);

            String page = Util.getResults(url);

            String resultsFound = "<div id=\"resfound\">";
            int hits = getNumberOfHits(page, resultsFound, ACMPortalFetcher.HITS_PATTERN);

            int index = page.indexOf(resultsFound);
            if (index >= 0) {
                page = page.substring(index + resultsFound.length());
            }

            if (hits == 0) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'",
                        terms),
                        Localization.lang("Search ACM Portal"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            } else if (hits > 20) {
                status.showMessage(
                        Localization.lang("%0 entries found. To reduce server load, only %1 will be downloaded.",
                                String.valueOf(hits), String.valueOf(PER_PAGE)),
                        Localization.lang("Search ACM Portal"), JOptionPane.INFORMATION_MESSAGE);
            }

            hits = getNumberOfHits(page, "<div class=\"pagerange\">", ACMPortalFetcher.MAX_HITS_PATTERN);
            parse(page, Math.min(hits, PER_PAGE), previews);
            for (Map.Entry<String, JLabel> entry : previews.entrySet()) {
                preview.addEntry(entry.getKey(), entry.getValue());
            }

            return true;

        } catch (MalformedURLException e) {
            LOGGER.warn("Problem with ACM fetcher URL", e);
        } catch (ConnectException e) {
            status.showMessage(Localization.lang("Connection to ACM Portal failed"),
                    Localization.lang("Search ACM Portal"), JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            status.showMessage(e.getMessage(),
                    Localization.lang("Search ACM Portal"), JOptionPane.ERROR_MESSAGE);
            LOGGER.warn("Problem with ACM Portal", e);
        }
        return false;

    }

    @Override
    public void getEntries(Map<String, Boolean> selection, ImportInspector inspector) {
        for (Map.Entry<String, Boolean> selentry : selection.entrySet()) {
            if (!shouldContinue) {
                break;
            }
            boolean sel = selentry.getValue();
            if (sel) {
                BibEntry entry = downloadEntryBibTeX(selentry.getKey(), fetchAbstract);
                if (entry != null) {
                    // Convert from HTML and optionally add curly brackets around key words to keep the case
                    String title = entry.getField("title");

                    if (title != null) {
                        title = title.replaceAll("\\\\&", "&").replaceAll("\\\\#", "#");
                        title = convertHTMLChars(title);

                        // Unit formatting
                        if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                            title = unitFormatter.format(title);
                        }

                        // Case keeping
                        if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                            title = caseKeeper.format(title);
                        }
                        entry.setField("title", title);
                    }

                    String abstr = entry.getField("abstract");
                    if (abstr != null) {
                        abstr = convertHTMLChars(abstr);
                        entry.setField("abstract", abstr);
                    }
                    inspector.addEntry(entry);
                }
            }
        }
    }

    @Override
    public int getWarningLimit() {
        return 10;
    }

    @Override
    public int getPreferredPreviewHeight() {
        return 75;
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        return false;
    }

    private String makeUrl() {
        StringBuilder sb = new StringBuilder(ACMPortalFetcher.START_URL).append(ACMPortalFetcher.SEARCH_URL_PART)
                .append(terms.replaceAll(" ", "%20")).append(ACMPortalFetcher.SEARCH_URL_PART_II);

        if (acmOrGuide) {
            sb.append("ACM");
        } else {
            sb.append("GUIDE");
        }
        sb.append(ACMPortalFetcher.END_URL);
        return sb.toString();
    }



    private void parse(String text, int hits, Map<String, JLabel> entries) {
        int entryNumber = 1;
        while (getNextEntryURL(text, entryNumber, entries) && (entryNumber <= hits)) {
            entryNumber++;
        }
    }

    private static String getEntryBibTeXURL(String fullCitation) {
        // Get ID
        Matcher idMatcher = ACMPortalFetcher.ID_PATTERN.matcher(fullCitation);
        if (idMatcher.find()) {
            return idMatcher.group(1);
        }
        LOGGER.info("Did not find ID in: " + fullCitation);
        return null;
    }

    private boolean getNextEntryURL(String allText, int entryNumber,
            Map<String, JLabel> entries) {
        String toFind = "<div class=\"numbering\">";
        int index = allText.indexOf(toFind, piv);
        int endIndex = allText.indexOf("<br clear=\"all\" />", index);
        piv = endIndex;

        if (index >= 0) {
            String text = allText.substring(index, endIndex);
            // Always try RIS import first
            Matcher fullCitation =
                    ACMPortalFetcher.FULL_CITATION_PATTERN.matcher(text);
            String item;
            if (fullCitation.find()) {
                String link = getEntryBibTeXURL(fullCitation.group(1));
                if (endIndex > 0) {
                    StringBuilder sb = new StringBuilder();

                    // Find authors:
                    String authMarker = "<div class=\"authors\">";
                    int authStart = text.indexOf(authMarker);
                    if (authStart >= 0) {
                        int authEnd = text.indexOf("</div>", authStart + authMarker.length());
                        if (authEnd >= 0) {
                            sb.append("<p>").append(text.substring(authStart, authEnd)).append("</p>");
                        }

                    }
                    // Find title:
                    Matcher titM = ACMPortalFetcher.TITLE_PATTERN.matcher(text);
                    if (titM.find()) {
                        sb.append("<p>").append(titM.group(1)).append("</p>");
                    }

                    String sourceMarker = "<div class=\"source\">";
                    int sourceStart = text.indexOf(sourceMarker);
                    if (sourceStart >= 0) {
                        int sourceEnd = text.indexOf("</div>", sourceStart + sourceMarker.length());
                        if (sourceEnd >= 0) {
                            String sourceText = text.substring(sourceStart, sourceEnd);
                            // Find source:
                            Matcher source = ACMPortalFetcher.SOURCE_PATTERN.matcher(sourceText);
                            if (source.find()) {
                                sb.append("<p>").append(source.group(1)).append("</p>");
                            }
                        }
                    }

                    item = sb.toString();
                } else {
                    item = link;
                }

                JLabel preview = new JLabel("<html>" + item + "</html>");
                preview.setPreferredSize(new Dimension(750, 100));
                entries.put(link, preview);
                return true;
            }
            LOGGER.warn("Citation unmatched " + Integer.toString(entryNumber));
            return false;
        }
        return false;
    }

    private static BibEntry downloadEntryBibTeX(String id, boolean downloadAbstract) {
        try {
            URL url = new URL(ACMPortalFetcher.START_URL + ACMPortalFetcher.BIBTEX_URL + id + ACMPortalFetcher.BIBTEX_URL_END);
            URLConnection connection = url.openConnection();

            // set user-agent to avoid being blocked as a crawler
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");
            Collection<BibEntry> items = null;
            try(BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                items = BibtexParser.parse(in).getDatabase().getEntries();
            } catch (IOException e) {
                LOGGER.info("Download of BibTeX information from ACM Portal failed.", e);
            }
            if ((items == null) || items.isEmpty()) {
                return null;
            }
            BibEntry entry = items.iterator().next();
            Thread.sleep(ACMPortalFetcher.WAIT_TIME);//wait between requests or you will be blocked by ACM

            // get abstract
            if (downloadAbstract) {
                url = new URL(ACMPortalFetcher.START_URL + ACMPortalFetcher.ABSTRACT_URL + id);
                String page = Util.getResults(url);
                Matcher absM = ACMPortalFetcher.ABSTRACT_PATTERN.matcher(page);
                if (absM.find()) {
                    entry.setField("abstract", absM.group(1).trim());
                }
                Thread.sleep(ACMPortalFetcher.WAIT_TIME);//wait between requests or you will be blocked by ACM
            }

            return entry;
        } catch (NoSuchElementException e) {
            LOGGER.info("Bad Bibtex record read at: " + ACMPortalFetcher.BIBTEX_URL + id + ACMPortalFetcher.BIBTEX_URL_END,
                    e);
            return null;
        } catch (MalformedURLException e) {
            LOGGER.info("Malformed URL.", e);
            return null;
        } catch (IOException e) {
            LOGGER.info("Cannot connect.", e);
            return null;
        } catch (InterruptedException ignored) {
            return null;
        }
    }

    /**
     * This method must convert HTML style char sequences to normal characters.
     * @param text The text to handle.
     * @return The converted text.
     */
    private String convertHTMLChars(String text) {

        return htmlConverter.format(text);
    }

    /**
     * Find out how many hits were found.
     * @param page
     */
    private static int getNumberOfHits(String page, String marker, Pattern pattern) throws IOException {
        int ind = page.indexOf(marker);
        if (ind >= 0) {
            String substring = page.substring(ind, Math.min(ind + 100, page.length()));
            Matcher m = pattern.matcher(substring);
            if (m.find()) {
                try {
                    String number = m.group(1);
                    number = number.replaceAll(",", ""); // Remove , as in 1,234
                    return Integer.parseInt(number);
                } catch (IllegalStateException | NumberFormatException ex) {
                    throw new IOException("Cannot parse number of hits");
                }
            } else {
                LOGGER.info("Unmatched! " + substring);
            }
        }
        throw new IOException("Cannot parse number of hits");
    }

    @Override
    public String getTitle() {
        return "ACM Portal";
    }

    @Override
    public String getHelpPage() {
        return "ACMPortalHelp.html";
    }


    // This method is called by the dialog when the user has cancelled or
    //signaled a stop. It is expected that any long-running fetch operations
    //will stop after this method is called.
    @Override
    public void stopFetching() {
        shouldContinue = false;
    }
}
