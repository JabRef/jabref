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

import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.Objects;
import java.util.regex.Pattern;

import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.l10n.Localization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IEEEXploreFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(IEEEXploreFetcher.class);

    private static final Pattern PREPROCESSING_PATTERN = Pattern.compile("(?<!&)(#[x]*[0]*\\p{XDigit}+;)");

    private static final String URL_SEARCH = "http://ieeexplore.ieee.org/rest/search?reload=true";
    private static final String URL_BIBTEX_START = "http://ieeexplore.ieee.org/xpl/downloadCitations?reload=true&recordIds=";
    private static final String URL_BIBTEX_END = "&download-format=download-bibtex&x=0&y=0";
    private static final String DIALOG_TITLE = Localization.lang("Search %0", "IEEEXplore");

    private static final Pattern PUBLICATION_PATTERN = Pattern.compile("(.*), \\d*\\.*\\s?(.*)");
    private static final Pattern PROCEEDINGS_PATTERN = Pattern.compile("(.*?)\\.?\\s?Proceedings\\s?(.*)");
    private static final Pattern MONTH_PATTERN = Pattern.compile("(\\d*+)\\s*([a-z]*+)-*(\\d*+)\\s*([a-z]*+)");

    private static final Pattern SUB_DETECTION_1 = Pattern.compile("/sub ([^/]+)/");
    private static final Pattern SUB_DETECTION_2 = Pattern.compile("\\(sub\\)([^(]+)\\(/sub\\)");
    private static final String SUB_TEXT_RESULT = "\\\\textsubscript\\{$1\\}";
    private static final String SUB_EQ_RESULT = "\\$_\\{$1\\}\\$";
    private static final Pattern SUPER_DETECTION_1 = Pattern.compile("/sup ([^/]+)/");
    private static final Pattern SUPER_DETECTION_2 = Pattern.compile("\\(sup\\)([^(]+)\\(/sup\\)");
    private static final String SUPER_TEXT_RESULT = "\\\\textsuperscript\\{$1\\}";
    private static final String SUPER_EQ_RESULT = "\\$\\^\\{$1\\}\\$";
    private static final int MAX_FETCH = 100;

    private final ProtectTermsFormatter protectTermsFormatter = new ProtectTermsFormatter();
    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();
    private final HtmlToLatexFormatter htmlToLatexFormatter = new HtmlToLatexFormatter();
    private final JournalAbbreviationLoader abbreviationLoader;

    private boolean shouldContinue;


    protected boolean isShouldContinue() {
        return shouldContinue;
    }

    protected void setShouldContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    protected static Log getLogger() {
        return LOGGER;
    }

    protected static Pattern getPreprocessingPattern() {
        return PREPROCESSING_PATTERN;
    }

    protected static String getUrlSearch() {
        return URL_SEARCH;
    }

    protected static String getUrlBibtexStart() {
        return URL_BIBTEX_START;
    }

    protected static String getUrlBibtexEnd() {
        return URL_BIBTEX_END;
    }

    protected static String getDialogTitle() {
        return DIALOG_TITLE;
    }

    protected static Pattern getPublicationPattern() {
        return PUBLICATION_PATTERN;
    }

    protected static Pattern getProceedingsPattern() {
        return PROCEEDINGS_PATTERN;
    }

    protected static Pattern getMonthPattern() {
        return MONTH_PATTERN;
    }

    protected static Pattern getSubDetection1() {
        return SUB_DETECTION_1;
    }

    protected static Pattern getSubDetection2() {
        return SUB_DETECTION_2;
    }

    protected static String getSubTextResult() {
        return SUB_TEXT_RESULT;
    }

    protected static String getSubEqResult() {
        return SUB_EQ_RESULT;
    }

    protected static Pattern getSuperDetection1() {
        return SUPER_DETECTION_1;
    }

    protected static Pattern getSuperDetection2() {
        return SUPER_DETECTION_2;
    }

    protected static String getSuperTextResult() {
        return SUPER_TEXT_RESULT;
    }

    protected static String getSuperEqResult() {
        return SUPER_EQ_RESULT;
    }

    protected ProtectTermsFormatter getProtectTermsFormatter() {
        return protectTermsFormatter;
    }

    protected UnitsToLatexFormatter getUnitsToLatexFormatter() {
        return unitsToLatexFormatter;
    }

    protected static int getMaxFetch() {
        return MAX_FETCH;
    }

    protected HtmlToLatexFormatter getHtmlToLatexFormatter() {
        return htmlToLatexFormatter;
    }

    protected JournalAbbreviationLoader getAbbreviationLoader() {
        return abbreviationLoader;
    }

    protected boolean shouldContinue() {
        return shouldContinue;
    }

    protected void setContinue(boolean shouldContinue) {
        this.shouldContinue = shouldContinue;
    }

    public IEEEXploreFetcher(JournalAbbreviationLoader abbreviationLoader) {
        super();
        this.abbreviationLoader = Objects.requireNonNull(abbreviationLoader);
        CookieHandler.setDefault(new CookieManager());
    }

    @Override
    public String getTitle() {
        return "IEEEXplore";
    }

    /**
     * This method is called by the dialog when the user has canceled the import.
     */
    @Override
    public void stopFetching() {
        shouldContinue = false;
    }

}
