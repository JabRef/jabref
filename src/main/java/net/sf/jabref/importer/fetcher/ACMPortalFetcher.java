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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ACMPortalFetcher implements PreviewEntryFetcher {

    private static final Pattern TITLE_PATTERN = Pattern.compile("<a href=.*?\">([^<]*)</a>");
    private static final Pattern SOURCE_PATTERN = Pattern.compile("<span style=\"padding-left:10px\">([^<]*)</span>");
    private static final Pattern ID_PATTERN = Pattern.compile("citation.cfm\\?id=(\\d+)&.*");

    private static final String NEXT_ENTRY_PATTERN = "<div class=\"numbering\">";
    private static final String AUTHOR_MARKER = "<div class=\"authors\">";
    private static final String SOURCE_MARKER = "<div class=\"source\">";
    private static final String END_ENTRY_PATTERN = "<br clear=\"all\" />";
    private static final String SEARCH_URL_PART = "results.cfm?query=";
    private static final String SEARCH_URL_PART_II = "&dl=";
    private static final String END_URL = "&coll=Portal&short=0";//&start=";

    private int piv;


    protected int getPiv() {
        return piv;
    }

    protected void setPiv(int piv) {
        this.piv = piv;
    }

    protected static Pattern getTitlePattern() {
        return TITLE_PATTERN;
    }

    protected static Pattern getSourcePattern() {
        return SOURCE_PATTERN;
    }

    protected static Pattern getIdPattern() {
        return ID_PATTERN;
    }

    protected static String getNextEntryPattern() {
        return NEXT_ENTRY_PATTERN;
    }

    protected static String getAuthorMarker() {
        return AUTHOR_MARKER;
    }

    protected static String getSourceMarker() {
        return SOURCE_MARKER;
    }

    protected static String getEndEntryPattern() {
        return END_ENTRY_PATTERN;
    }

    protected static String getSearchUrlPart() {
        return SEARCH_URL_PART;
    }

    protected static String getSearchUrlPartII() {
        return SEARCH_URL_PART_II;
    }

    protected static String getEndUrl() {
        return END_URL;
    }


    private static final Log LOGGER = LogFactory.getLog(ACMPortalFetcher.class);

    private final HtmlToLatexFormatter htmlToLatexFormatter = new HtmlToLatexFormatter();
    private final ProtectTermsFormatter protectTermsFormatter = new ProtectTermsFormatter();
    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();
    private String terms;

    private static final String START_URL = "http://portal.acm.org/";

    private static final String BIBTEX_URL = "exportformats.cfm?id=";
    private static final String BIBTEX_URL_END = "&expformat=bibtex";
    private static final String ABSTRACT_URL = "tab_abstract.cfm?id=";

    private static final String RESULTS_FOUND_PATTERN = "<div id=\"resfound\">";
    private static final String PAGE_RANGE_PATTERN = "<div class=\"pagerange\">";

    private static final int PER_PAGE = 20; // Fetch only one page. Otherwise, the user will get blocked by ACM. 100 has been the old setting. See Bug 3532752 - https://sourceforge.net/tracker/index.php?func=detail&aid=3532752&group_id=92314&atid=600306
    private static final int WAIT_TIME = 200;
    private boolean shouldContinue;

    // user settings
    private boolean fetchAbstract;
    private boolean acmOrGuide;

    private static final Pattern HITS_PATTERN = Pattern.compile("<strong>(\\d+,*\\d*)</strong> results found");
    private static final Pattern MAX_HITS_PATTERN = Pattern
            .compile("Result \\d+,*\\d* &ndash; \\d+,*\\d* of (\\d+,*\\d*)");

    private static final Pattern FULL_CITATION_PATTERN = Pattern.compile("<a href=\"(citation.cfm.*)\" target.*");

    // Patterns used to extract information for the preview:

    private static final Pattern ABSTRACT_PATTERN = Pattern.compile("<div .*?>(.*?)</div>");


    protected static String getStartUrl() {
        return START_URL;
    }

    protected boolean isShouldContinue() {
        return shouldContinue;
    }

    protected void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    protected boolean isFetchAbstract() {
        return fetchAbstract;
    }

    protected void setFetchAbstract(boolean fetchAbstract) {
        this.fetchAbstract = fetchAbstract;
    }

    protected boolean isAcmOrGuide() {
        return acmOrGuide;
    }

    protected void setAcmOrGuide(boolean acmOrGuide) {
        this.acmOrGuide = acmOrGuide;
    }

    protected static Log getLogger() {
        return LOGGER;
    }

    protected String getTerms() {
        return terms;
    }

    protected void setTerms(String term) {
        this.terms = term;
    }

    protected static String getResultsFoundPattern() {
        return RESULTS_FOUND_PATTERN;
    }

    protected static String getPageRangePattern() {
        return PAGE_RANGE_PATTERN;
    }

    protected static int getPerPage() {
        return PER_PAGE;
    }

    protected static Pattern getHitsPattern() {
        return HITS_PATTERN;
    }

    protected static Pattern getMaxHitsPattern() {
        return MAX_HITS_PATTERN;
    }

    protected static Pattern getFullCitationPattern() {
        return FULL_CITATION_PATTERN;
    }

    protected static Pattern getAbstractPattern() {
        return ABSTRACT_PATTERN;
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
                    entry.getFieldOptional("title").ifPresent(title -> {
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
                        entry.setField("title", title);
                    });

                    entry.getFieldOptional("abstract")
                            .ifPresent(abstr -> entry.setField("abstract", convertHTMLChars(abstr)));
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

    private static Optional<BibEntry> downloadEntryBibTeX(String id, boolean downloadAbstract) {
        try {
            URL url = new URL(
                    ACMPortalFetcher.START_URL + ACMPortalFetcher.BIBTEX_URL + id + ACMPortalFetcher.BIBTEX_URL_END);
            URLConnection connection = url.openConnection();

            // set user-agent to avoid being blocked as a crawler
            connection.addRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");
            Collection<BibEntry> items = null;
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                items = BibtexParser.parse(in).getDatabase().getEntries();
            } catch (IOException e) {
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
                String page = dl.downloadToString();

                Matcher absM = ACMPortalFetcher.ABSTRACT_PATTERN.matcher(page);
                if (absM.find()) {
                    entry.setField("abstract", absM.group(1).trim());
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

    @Override
    public String getTitle() {
        return "ACM Portal";
    }

    // This method is called by the dialog when the user has canceled or
    //signaled a stop. It is expected that any long-running fetch operations
    //will stop after this method is called.
    @Override
    public void stopFetching() {
        shouldContinue = false;
    }
}
