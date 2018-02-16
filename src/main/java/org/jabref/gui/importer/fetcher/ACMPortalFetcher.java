package org.jabref.gui.importer.fetcher;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.jabref.Globals;
import org.jabref.gui.importer.FetcherPreviewDialog;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.ImportInspector;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ACMPortalFetcher implements PreviewEntryFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ACMPortalFetcher.class);

    private static final String START_URL = "https://portal.acm.org/";
    private static final String SEARCH_URL_PART = "results.cfm?query=";
    private static final String SEARCH_URL_PART_II = "&dl=";
    private static final String END_URL = "&coll=Portal&short=0";//&start=";

    private static final String BIBTEX_URL = "exportformats.cfm?id=";
    private static final String BIBTEX_URL_END = "&expformat=bibtex";
    private static final String ABSTRACT_URL = "tab_abstract.cfm?id=";

    private static final String NEXT_ENTRY_PATTERN = "<div class=\"numbering\">";
    private static final String AUTHOR_MARKER = "<div class=\"authors\">";
    private static final String SOURCE_MARKER = "<div class=\"source\">";
    private static final String END_ENTRY_PATTERN = "<br clear=\"all\" />";

    private static final String RESULTS_FOUND_PATTERN = "<div id=\"resfound\">";
    private static final String PAGE_RANGE_PATTERN = "<div class=\"pagerange\">";

    private static final String START_BIBTEX_ENTRY = "@";
    private static final String END_BIBTEX_ENTRY_HTML = "</pre>";

    private static final int PER_PAGE = 20; // Fetch only one page. Otherwise, the user will get blocked by ACM. 100 has been the old setting. See Bug 3532752 - https://sourceforge.net/tracker/index.php?func=detail&aid=3532752&group_id=92314&atid=600306
    private static final int WAIT_TIME = 200;

    private static final Pattern HITS_PATTERN = Pattern.compile("<strong>(\\d+,*\\d*)</strong> results found");
    private static final Pattern MAX_HITS_PATTERN = Pattern
            .compile("Result \\d+,*\\d* &ndash; \\d+,*\\d* of (\\d+,*\\d*)");

    private static final Pattern FULL_CITATION_PATTERN = Pattern.compile("<a href=\"(citation.cfm.*)\" target.*");

    private static final Pattern ID_PATTERN = Pattern.compile("citation.cfm\\?id=(\\d+)&.*");

    // Patterns used to extract information for the preview:
    private static final Pattern TITLE_PATTERN = Pattern.compile("<a href=.*?\">([^<]*)</a>");
    private static final Pattern ABSTRACT_PATTERN = Pattern.compile("<div .*?>(.*?)</div>");
    private static final Pattern SOURCE_PATTERN = Pattern.compile("<span style=\"padding-left:10px\">([^<]*)</span>");

    private final HtmlToLatexFormatter htmlToLatexFormatter = new HtmlToLatexFormatter();

    private final Formatter protectTermsFormatter = new ProtectTermsFormatter(Globals.protectedTermsLoader);

    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();
    private String terms;
    private final JRadioButton acmButton = new JRadioButton(Localization.lang("The ACM Digital Library"));

    private final JRadioButton guideButton = new JRadioButton(Localization.lang("The Guide to Computing Literature"));
    private final JCheckBox absCheckBox = new JCheckBox(Localization.lang("Include abstracts"), false);

    private boolean shouldContinue;
    // user settings
    private boolean fetchAbstract;

    private boolean acmOrGuide;

    private int piv;

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
            URLDownload dl = new URLDownload(address);

            String page = dl.asString(Globals.prefs.getDefaultEncoding());

            int hits = getNumberOfHits(page, RESULTS_FOUND_PATTERN, ACMPortalFetcher.HITS_PATTERN);

            int index = page.indexOf(RESULTS_FOUND_PATTERN);
            if (index >= 0) {
                page = page.substring(index + RESULTS_FOUND_PATTERN.length());
            }

            if (hits == 0) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", terms),
                        Localization.lang("Search %0", getTitle()), JOptionPane.INFORMATION_MESSAGE);
                return false;
            } else if (hits > 20) {
                status.showMessage(
                        Localization.lang("%0 entries found. To reduce server load, only %1 will be downloaded.",
                                String.valueOf(hits), String.valueOf(PER_PAGE)),
                        Localization.lang("Search %0", getTitle()), JOptionPane.INFORMATION_MESSAGE);
            }

            hits = getNumberOfHits(page, PAGE_RANGE_PATTERN, ACMPortalFetcher.MAX_HITS_PATTERN);
            parse(page, Math.min(hits, PER_PAGE), previews);
            for (Map.Entry<String, JLabel> entry : previews.entrySet()) {
                preview.addEntry(entry.getKey(), entry.getValue());
            }

            return true;

        } catch (IOException e) {
            LOGGER.error("Error while fetching from " + getTitle(), e);
            preview.showErrorMessage(this.getTitle(), e.getLocalizedMessage());
            return false;
        }
    }

    @Override
    public void getEntries(Map<String, Boolean> selection, ImportInspector inspector) {
        for (Map.Entry<String, Boolean> selentry : selection.entrySet()) {
            if (!shouldContinue) {
                break;
            }
            if (selentry.getValue()) {
                downloadEntryBibTeX(selentry.getKey(), fetchAbstract).ifPresent(entry -> {
                    // Convert from HTML and optionally add curly brackets around key words to keep the case
                    entry.getField(FieldName.TITLE).ifPresent(title -> {
                        title = title.replace("\\&", "&").replace("\\#", "#");
                        title = convertHTMLChars(title);

                        // Unit formatting
                        if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                            title = unitsToLatexFormatter.format(title);
                        }

                        // Case keeping
                        if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                            title = protectTermsFormatter.format(title);
                        }
                        entry.setField(FieldName.TITLE, title);
                    });

                    entry.getField(FieldName.ABSTRACT)
                            .ifPresent(abstr -> entry.setField(FieldName.ABSTRACT, convertHTMLChars(abstr)));
                    inspector.addEntry(entry);
                });
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
                .append(terms.replace(" ", "%20")).append(ACMPortalFetcher.SEARCH_URL_PART_II);

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
        int index = allText.indexOf(NEXT_ENTRY_PATTERN, piv);
        int endIndex = allText.indexOf(END_ENTRY_PATTERN, index);
        piv = endIndex;

        if (index >= 0) {
            String text = allText.substring(index, endIndex);
            // Always try RIS import first
            Matcher fullCitation = ACMPortalFetcher.FULL_CITATION_PATTERN.matcher(text);
            String item;
            if (fullCitation.find()) {
                String link = getEntryBibTeXURL(fullCitation.group(1));
                if (endIndex > 0) {
                    StringBuilder sb = new StringBuilder();

                    // Find authors:
                    int authStart = text.indexOf(AUTHOR_MARKER);
                    if (authStart >= 0) {
                        int authEnd = text.indexOf("</div>", authStart + AUTHOR_MARKER.length());
                        if (authEnd >= 0) {
                            sb.append("<p>").append(text.substring(authStart, authEnd)).append("</p>");
                        }

                    }
                    // Find title:
                    Matcher titM = ACMPortalFetcher.TITLE_PATTERN.matcher(text);
                    if (titM.find()) {
                        sb.append("<p>").append(titM.group(1)).append("</p>");
                    }

                    int sourceStart = text.indexOf(SOURCE_MARKER);
                    if (sourceStart >= 0) {
                        int sourceEnd = text.indexOf("</div>", sourceStart + SOURCE_MARKER.length());
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

    private static Optional<BibEntry> downloadEntryBibTeX(String id, boolean downloadAbstract) {
        try {
            URL url = new URL(
                    ACMPortalFetcher.START_URL + ACMPortalFetcher.BIBTEX_URL + id + ACMPortalFetcher.BIBTEX_URL_END);
            URLConnection connection = url.openConnection();

            // set user-agent to avoid being blocked as a crawler
            connection.addRequestProperty("User-Agent", URLDownload.USER_AGENT);
            Collection<BibEntry> items = null;

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String htmlCode = in.lines().filter(s -> !s.isEmpty()).collect(Collectors.joining());
                String bibtexString = htmlCode.substring(htmlCode.indexOf(START_BIBTEX_ENTRY),
                        htmlCode.indexOf(END_BIBTEX_ENTRY_HTML));
                items = new BibtexParser(Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor()).parseEntries(bibtexString);

            } catch (IOException | ParseException e) {
                LOGGER.info("Download of BibTeX information from ACM Portal failed.", e);
            }
            if ((items == null) || items.isEmpty()) {
                return Optional.empty();
            }
            BibEntry entry = items.iterator().next();
            Thread.sleep(ACMPortalFetcher.WAIT_TIME);//wait between requests or you will be blocked by ACM

            // get abstract
            if (downloadAbstract) {
                URLDownload dl = new URLDownload(ACMPortalFetcher.START_URL + ACMPortalFetcher.ABSTRACT_URL + id);
                String page = dl.asString(Globals.prefs.getDefaultEncoding());

                Matcher absM = ACMPortalFetcher.ABSTRACT_PATTERN.matcher(page);
                if (absM.find()) {
                    entry.setField(FieldName.ABSTRACT, absM.group(1).trim());
                }
                Thread.sleep(ACMPortalFetcher.WAIT_TIME);//wait between requests or you will be blocked by ACM
            }

            return Optional.of(entry);
        } catch (NoSuchElementException e) {
            LOGGER.info(
                    "Bad BibTeX record read at: " + ACMPortalFetcher.BIBTEX_URL + id + ACMPortalFetcher.BIBTEX_URL_END,
                    e);
        } catch (MalformedURLException e) {
            LOGGER.info("Malformed URL.", e);
        } catch (IOException e) {
            LOGGER.info("Cannot connect.", e);
        } catch (InterruptedException ignored) {
            // Ignored
        }
        return Optional.empty();
    }

    /**
     * This method must convert HTML style char sequences to normal characters.
     * @param text The text to handle.
     * @return The converted text.
     */
    private String convertHTMLChars(String text) {

        return htmlToLatexFormatter.format(text);
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
                    number = number.replace(",", ""); // Remove , as in 1,234
                    return Integer.parseInt(number);
                } catch (NumberFormatException ex) {
                    throw new IOException("Cannot parse number of hits");
                }
            }
            LOGGER.info("Unmatched! " + substring);
        }
        throw new IOException("Cannot parse number of hits");
    }

    @Override
    public String getTitle() {
        return "ACM Portal";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ACM;
    }

    // This method is called by the dialog when the user has canceled or
    //signaled a stop. It is expected that any long-running fetch operations
    //will stop after this method is called.
    @Override
    public void stopFetching() {
        shouldContinue = false;
    }
}
