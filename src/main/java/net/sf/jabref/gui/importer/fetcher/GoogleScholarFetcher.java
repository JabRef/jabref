package net.sf.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.importer.FetcherPreviewDialog;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GoogleScholarFetcher implements PreviewEntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(GoogleScholarFetcher.class);

    private static final String QUERY_MARKER = "___QUERY___";
    private static final String URL_START = "https://scholar.google.com";
    private static final String SEARCH_URL = GoogleScholarFetcher.URL_START + "/scholar?q=" + GoogleScholarFetcher.QUERY_MARKER
            + "&hl=en&btnG=Search&oe=utf-8";
    private static final String CITATIONS_PAGE_URL_BASE = "https://scholar.google.de/scholar?q=info:";
    private static final String CITATIONS_PAGE_URL_SUFFIX = ":scholar.google.com/&output=cite&scirp=0&hl=en";

    private static final Pattern SCHOLAR_ID_PATTERN = Pattern.compile("gs_ocit\\(event,'(\\w*)'");
    private static final Pattern BIBTEX_LINK_PATTERN = Pattern.compile("href=\"(.*)\">BibTeX");
    private static final Pattern TITLE_START_PATTERN = Pattern.compile("<div class=\"gs_ri\">");
    private static final Pattern LINK_PATTERN = Pattern.compile("<h3 class=\"gs_rt\"><a href=\"([^\"]*)\">");
    private static final Pattern TITLE_END_PATTERN = Pattern.compile("<div class=\"gs_fl\">");

    private static final Pattern INPUT_PATTERN = Pattern.compile("<input type=([^ ]+) name=([^ ]+) value=([^> ]+)");

    private final Map<String, String> entryLinks = new HashMap<>();

    private boolean stopFetching;


    @Override
    public int getWarningLimit() {
        return 10;
    }

    @Override
    public int getPreferredPreviewHeight() {
        return 100;
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        return false;
    }

    @Override
    public boolean processQueryGetPreview(String query, FetcherPreviewDialog preview, OutputPrinter status) {
        entryLinks.clear();
        stopFetching = false;
        try {
            Map<String, JLabel> citations = getCitations(query);
            if (!citations.isEmpty()) {
                for (Map.Entry<String, JLabel> linkEntry : citations.entrySet()) {
                    preview.addEntry(linkEntry.getKey(), linkEntry.getValue());
                }
                return true;
            }

            status.showMessage(Localization.lang("No entries found for the search string '%0'", query),
                    Localization.lang("Search %0", getTitle()), JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            LOGGER.error("Error while fetching from " + getTitle(), e);
            preview.showErrorMessage(this.getTitle(), e.getLocalizedMessage());
        }
        return false;
    }

    @Override
    public void getEntries(Map<String, Boolean> selection, ImportInspector inspector) {
        int toDownload = 0;
        for (Map.Entry<String, Boolean> selEntry : selection.entrySet()) {
            boolean isSelected = selEntry.getValue();
            if (isSelected) {
                toDownload++;
            }
        }
        if (toDownload == 0) {
            return;
        }

        int downloaded = 0;

        for (Map.Entry<String, Boolean> selEntry : selection.entrySet()) {
            if (stopFetching) {
                break;
            }
            inspector.setProgress(downloaded, toDownload);
            boolean isSelected = selEntry.getValue();
            if (isSelected) {
                downloaded++;
                try {
                    BibEntry entry = downloadEntry(selEntry.getKey());
                    inspector.addEntry(entry);
                } catch (IOException e) {
                    LOGGER.warn("Cannot download entry from Google scholar", e);
                }
            }
        }

    }

    @Override
    public String getTitle() {
        return "Google Scholar";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_GOOGLE_SCHOLAR;
    }

    @Override
    public JPanel getOptionsPanel() {
        return null;
    }

    @Override
    public void stopFetching() {
        stopFetching = true;
    }

    /**
     * @param query The search term to query Google Scholar for.
     * @return a list of IDs
     * @throws java.io.IOException
     */
    private Map<String, JLabel> getCitations(String query) throws IOException {
        String urlQuery;
        LinkedHashMap<String, JLabel> res = new LinkedHashMap<>();

        urlQuery = GoogleScholarFetcher.SEARCH_URL.replace(GoogleScholarFetcher.QUERY_MARKER,
                URLEncoder.encode(query, StandardCharsets.UTF_8.name()));
        int count = 1;
        String nextPage;
        while (((nextPage = getCitationsFromUrl(urlQuery, res)) != null) && (count < 2)) {
            urlQuery = nextPage;
            count++;
            if (stopFetching) {
                break;
            }
        }
        return res;
    }

    private String getCitationsFromUrl(String urlQuery, Map<String, JLabel> ids) throws IOException {
        String cont = URLDownload.createURLDownloadWithBrowserUserAgent(urlQuery).downloadToString(StandardCharsets.UTF_8);

        Matcher m = GoogleScholarFetcher.SCHOLAR_ID_PATTERN.matcher(cont);
        int lastRegionStart = 0;

        while (m.find()) {

            String citationsPageURL = CITATIONS_PAGE_URL_BASE+m.group(1)+CITATIONS_PAGE_URL_SUFFIX;

            String citationsPage = URLDownload.createURLDownloadWithBrowserUserAgent(citationsPageURL).downloadToString(StandardCharsets.UTF_8);

            Matcher citationPageMatcher = GoogleScholarFetcher.BIBTEX_LINK_PATTERN.matcher(citationsPage);
            citationPageMatcher.find();
            String link = citationPageMatcher.group(1).replace("&amp;", "&");;

            String pText;
            String part = cont.substring(lastRegionStart, m.start());
            Matcher titleS = GoogleScholarFetcher.TITLE_START_PATTERN.matcher(part);
            Matcher titleE = GoogleScholarFetcher.TITLE_END_PATTERN.matcher(part);
            boolean fS = titleS.find();
            boolean fE = titleE.find();
            if (fS && fE) {
                if (titleS.end() < titleE.start()) {
                    pText = part.substring(titleS.end(), titleE.start());
                } else {
                    pText = part;
                }
            } else {
                pText = link;
            }

            pText = pText.replace("[PDF]", "");
            pText = pText.replace("[HTML]", "");
            JLabel preview = new JLabel("<html>" + pText + "</html>");
            ids.put(link, preview);

            // See if we can extract the link Google Scholar puts on the entry's title.
            // That will be set as "url" for the entry if downloaded:
            Matcher linkMatcher = GoogleScholarFetcher.LINK_PATTERN.matcher(pText);
            if (linkMatcher.find()) {
                entryLinks.put(link, linkMatcher.group(1));
            }

            lastRegionStart = m.end();
        }

        /*m = NEXT_PAGE_PATTERN.matcher(cont);
        if (m.find()) {
            System.out.println("NEXT: "+URL_START+m.group(1).replace("&amp;", "&"));
            return URL_START+m.group(1).replace("&amp;", "&");
        }
        else*/
        return null;
    }

    private BibEntry downloadEntry(String link) throws IOException {
        try {
            String s = URLDownload.createURLDownloadWithBrowserUserAgent(link).downloadToString(StandardCharsets.UTF_8);
            BibtexParser bp = new BibtexParser(Globals.prefs.getImportFormatPreferences());
            ParserResult pr = bp.parse(new StringReader(s));
            if ((pr != null) && (pr.getDatabase() != null)) {
                Collection<BibEntry> entries = pr.getDatabase().getEntries();
                if (entries.size() == 1) {
                    BibEntry entry = entries.iterator().next();
                    entry.clearField(BibEntry.KEY_FIELD);
                    // If the entry's url field is not set, and we have stored an url for this
                    // entry, set it:
                    if (!entry.hasField(FieldName.URL)) {
                        String storedUrl = entryLinks.get(link);
                        if (storedUrl != null) {
                            entry.setField(FieldName.URL, storedUrl);
                        }
                    }

                    // Clean up some remaining HTML code from Elsevier(?) papers
                    // Search for: Poincare algebra
                    // to see an example
                    entry.getField(FieldName.TITLE).ifPresent(title -> {
                        String newtitle = title.replaceAll("<.?i>([^<]*)</i>", "$1");
                        if (!newtitle.equals(title)) {
                            entry.setField(FieldName.TITLE, newtitle);
                        }
                    });
                    return entry;
                } else if (entries.isEmpty()) {
                    LOGGER.warn("No entry found! (" + link + ")");
                    return null;
                } else {
                    LOGGER.debug(entries.size() + " entries found! (" + link + ")");
                    return null;
                }
            }
            LOGGER.warn("Parser failed! (" + link + ")");
            return null;
        } catch (MalformedURLException ex) {
            LOGGER.error("Malformed URL.", ex);
            return null;
        }
    }
}
