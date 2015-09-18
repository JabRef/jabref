/*  Copyright (C) 2003-2011 Aaron Chen
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
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
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
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.FetcherPreviewDialog;
import net.sf.jabref.logic.l10n.Localization;

public class ACMPortalFetcher implements PreviewEntryFetcher {

    private final ImportInspector dialog = null;
    private final HTMLConverter htmlConverter = new HTMLConverter();
    private final CaseKeeper caseKeeper = new CaseKeeper();
    private final UnitFormatter unitFormatter = new UnitFormatter();
    private String terms;

    private static final String startUrl = "http://portal.acm.org/";
    private static final String searchUrlPart = "results.cfm?query=";
    private static final String searchUrlPartII = "&dl=";
    private static final String endUrl = "&coll=Portal&short=0";//&start=";

    private static final String bibtexUrl = "exportformats.cfm?id=";
    private static final String bibtexUrlEnd = "&expformat=bibtex";
    private static final String abstractUrl = "tab_abstract.cfm?id=";

    private final JRadioButton acmButton = new JRadioButton(Localization.lang("The ACM Digital Library"));
    private final JRadioButton guideButton = new JRadioButton(Localization.lang("The Guide to Computing Literature"));
    private final JCheckBox absCheckBox = new JCheckBox(Localization.lang("Include abstracts"), false);

    private static final int perPage = 20;
    private static final int MAX_FETCH = ACMPortalFetcher.perPage; // only one page. Otherwise, the user will get blocked by ACM. 100 has been the old setting. See Bug 3532752 - https://sourceforge.net/tracker/index.php?func=detail&aid=3532752&group_id=92314&atid=600306
    private static final int WAIT_TIME = 200;
    private boolean shouldContinue;

    // user settings
    private boolean fetchAbstract;
    private boolean acmOrGuide;

    private static final Pattern hitsPattern = Pattern.compile(".*Found <b>(\\d+,*\\d*)</b>.*");
    private static final Pattern maxHitsPattern = Pattern.compile(".*Results \\d+ - \\d+ of (\\d+,*\\d*).*");
    //private static final Pattern bibPattern = Pattern.compile(".*'(exportformats.cfm\\?id=\\d+&expformat=bibtex)'.*");

    private static final Pattern fullCitationPattern =
            Pattern.compile("<A HREF=\"(citation.cfm.*)\" class.*");

    private static final Pattern idPattern =
            Pattern.compile("citation.cfm\\?id=\\d*\\.?(\\d+)&.*");

    // Patterns used to extract information for the preview:
    private static final Pattern titlePattern = Pattern.compile("<A HREF=.*?\">([^<]*)</A>");
    private static final Pattern monthYearPattern = Pattern.compile("([A-Za-z]+ [0-9]{4})");
    private static final Pattern absPattern = Pattern.compile("<div .*?>(.*?)</div>");


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
        int parsed = 0;
        int unparseable = 0;
        acmOrGuide = acmButton.isSelected();
        fetchAbstract = absCheckBox.isSelected();
        int firstEntry = 1;
        String address = makeUrl(firstEntry);
        LinkedHashMap<String, JLabel> previews = new LinkedHashMap<String, JLabel>();

        try {
            URL url = new URL(address);

            String page = getResults(url);

            int hits = getNumberOfHits(page, "Found", ACMPortalFetcher.hitsPattern);

            int index = page.indexOf("Found");
            if (index >= 0) {
                page = page.substring(index + 5);
                index = page.indexOf("Found");
                if (index >= 0) {
                    page = page.substring(index);
                }
            }

            if (hits == 0) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'",
                                terms),
                        Localization.lang("Search ACM Portal"), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            hits = getNumberOfHits(page, "Results", ACMPortalFetcher.maxHitsPattern);

            for (int i = 0; i < hits; i++) {
                parse(page, 0, firstEntry, previews);
                //address = makeUrl(firstEntry);
                firstEntry += ACMPortalFetcher.perPage;
            }
            for (String s : previews.keySet()) {
                preview.addEntry(s, previews.get(s));
            }

            return true;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            status.showMessage(Localization.lang("Connection to ACM Portal failed"),
                    Localization.lang("Search ACM Portal"), JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            status.showMessage(Localization.lang(e.getMessage()),
                    Localization.lang("Search ACM Portal"), JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return false;

    }

    @Override
    public void getEntries(Map<String, Boolean> selection, ImportInspector inspector) {
        for (String id : selection.keySet()) {
            if (!shouldContinue) {
                break;
            }
            boolean sel = selection.get(id);
            if (sel) {
                BibtexEntry entry = downloadEntryBibTeX(id, fetchAbstract);
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

    private String makeUrl(int startIndex) {
        StringBuilder sb = new StringBuilder(ACMPortalFetcher.startUrl).append(ACMPortalFetcher.searchUrlPart);
        sb.append(terms.replaceAll(" ", "%20"));
        sb.append("&start=").append(startIndex);
        sb.append(ACMPortalFetcher.searchUrlPartII);

        if (acmOrGuide) {
            sb.append("ACM");
        } else {
            sb.append("GUIDE");
        }
        sb.append(ACMPortalFetcher.endUrl);
        return sb.toString();
    }


    private int piv;


    private void parse(String text, int startIndex, int firstEntryNumber, Map<String, JLabel> entries) {
        piv = startIndex;
        int entryNumber = firstEntryNumber;
        while (getNextEntryURL(text, piv, entryNumber, entries)) {
            entryNumber++;
        }

    }

    private String getEntryBibTeXURL(String fullCitation, boolean abs) {
        // Get ID
        Matcher idMatcher = ACMPortalFetcher.idPattern.matcher(fullCitation);
        if (idMatcher.find()) {
            return idMatcher.group(1);
        }
        else {
            System.out.println("Did not find ID in: " + fullCitation);
            return null;
        }

    }

    private boolean getNextEntryURL(String allText, int startIndex, int entryNumber,
            Map<String, JLabel> entries) {
        String toFind = "<strong>" + entryNumber + "</strong><br>";
        int index = allText.indexOf(toFind, startIndex);
        int endIndex = allText.length();

        if (index >= 0) {
            piv = index + 1;
            String text = allText.substring(index, endIndex);
            // Always try RIS import first
            Matcher fullCitation =
                    ACMPortalFetcher.fullCitationPattern.matcher(text);
            if (fullCitation.find()) {
                String link = getEntryBibTeXURL(fullCitation.group(1), fetchAbstract);
                String part;
                int endOfRecord = text.indexOf("<div class=\"abstract2\">");
                if (endOfRecord > 0) {
                    StringBuilder sb = new StringBuilder();
                    part = text.substring(0, endOfRecord);

                    try {
                        save("part" + entryNumber + ".html", part);
                    } catch (IOException e) {
                        e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
                    }

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
                    Matcher titM = ACMPortalFetcher.titlePattern.matcher(part);
                    if (titM.find()) {
                        sb.append("<p>").append(titM.group(1)).append("</p>");
                    }
                    // Find month and year:
                    Matcher mY = ACMPortalFetcher.monthYearPattern.matcher(part);
                    if (mY.find()) {
                        sb.append("<p>").append(mY.group(1)).append("</p>");
                    }

                    part = sb.toString();
                    /*.replaceAll("</tr>", "<br>");
                    part = part.replaceAll("</td>", "");
                    part = part.replaceAll("<tr valign=\"[A-Za-z]*\">", "");
                    part = part.replaceAll("<table style=\"padding: 5px; 5px; 5px; 5px;\" border=\"0\">", "");*/
                } else {
                    part = link;
                }

                JLabel preview = new JLabel("<html>" + part + "</html>");
                preview.setPreferredSize(new Dimension(750, 100));
                entries.put(link, preview);
                return true;
            } else {
                System.out.printf("Citation Unmatched %d%n", entryNumber);
                System.out.printf(text);
                return false;
            }

        }

        return false;
    }

    private BibtexEntry downloadEntryBibTeX(String ID, boolean abs) {
        try {
            URL url = new URL(ACMPortalFetcher.startUrl + ACMPortalFetcher.bibtexUrl + ID + ACMPortalFetcher.bibtexUrlEnd);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            ParserResult result = BibtexParser.parse(in);
            in.close();
            Collection<BibtexEntry> item = result.getDatabase().getEntries();
            if (item.isEmpty()) {
                return null;
            }
            BibtexEntry entry = item.iterator().next();
            Thread.sleep(ACMPortalFetcher.WAIT_TIME);//wait between requests or you will be blocked by ACM

            // get abstract
            if (abs) {
                url = new URL(ACMPortalFetcher.startUrl + ACMPortalFetcher.abstractUrl + ID);
                String page = getResults(url);
                Matcher absM = ACMPortalFetcher.absPattern.matcher(page);
                if (absM.find()) {
                    entry.setField("abstract", absM.group(1).trim());
                }
                Thread.sleep(ACMPortalFetcher.WAIT_TIME);//wait between requests or you will be blocked by ACM
            }

            return entry;

        } catch (NoSuchElementException e) {
            System.out.println("Bad Bibtex record read at: " + ACMPortalFetcher.bibtexUrl + ID + ACMPortalFetcher.bibtexUrlEnd);
            e.printStackTrace();
            return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (ConnectException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
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
    private int getNumberOfHits(String page, String marker, Pattern pattern) throws IOException {
        int ind = page.indexOf(marker);
        if (ind < 0) {
            throw new IOException(Localization.lang("Could not parse number of hits"));
        }
        String substring = page.substring(ind, Math.min(ind + 42, page.length()));
        Matcher m = pattern.matcher(substring);
        if (!m.find()) {
            System.out.println("Unmatched!");
            System.out.println(substring);
        } else {
            try {
                // get rid of ,
                String number = m.group(1);
                //NumberFormat nf = NumberFormat.getInstance();
                //return nf.parse(number).intValue();
                number = number.replaceAll(",", "");
                //System.out.println(number);
                return Integer.parseInt(number);
            } catch (NumberFormatException ex) {
                throw new IOException(Localization.lang("Could not parse number of hits"));
            } catch (IllegalStateException e) {
                throw new IOException(Localization.lang("Could not parse number of hits"));
            }
        }
        throw new IOException(Localization.lang("Could not parse number of hits"));
    }

    /**
     * Download the URL and return contents as a String.
     * @param source
     * @return
     * @throws IOException
     */
    private String getResults(URL source) throws IOException {

        InputStream in = source.openStream();
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[256];
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                break;
            }
            for (int i = 0; i < bytesRead; i++) {
                sb.append((char) buffer[i]);
            }
        }
        return sb.toString();
    }

    /**
     * Read results from a file instead of an URL. Just for faster debugging.
     * @param f
     * @return
     * @throws IOException
     */
    public String getResultsFromFile(File f) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(f));
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[256];
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) {
                break;
            }
            for (int i = 0; i < bytesRead; i++) {
                sb.append((char) buffer[i]);
            }
        }
        return sb.toString();
    }

    @Override
    public String getTitle() {
        return "ACM Portal";
    }

    @Override
    public String getHelpPage() {
        return "ACMPortalHelp.html";
    }

    @Override
    public String getKeyName() {
        return "ACM Portal";
    }

    // This method is called by the dialog when the user has cancelled the import.
    public void cancelled() {
        shouldContinue = false;
    }

    // This method is called by the dialog when the user has selected the
    //wanted entries, and clicked Ok. The callback object can update status
    //line etc.
    public void done(int entriesImported) {

    }

    // This method is called by the dialog when the user has cancelled or
    //signalled a stop. It is expected that any long-running fetch operations
    //will stop after this method is called.
    @Override
    public void stopFetching() {
        shouldContinue = false;
    }

    private void save(String filename, String content) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(filename));
        out.write(content);
        out.close();
    }
}
