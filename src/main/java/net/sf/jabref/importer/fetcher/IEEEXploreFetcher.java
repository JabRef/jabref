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

import java.awt.BorderLayout;

import java.io.IOException;

import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.model.entry.EntryType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.importer.*;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.logic.formatter.bibtexfields.UnitFormatter;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeper;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeperList;
import net.sf.jabref.logic.journals.Abbreviations;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.util.Util;

public class IEEEXploreFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(IEEEXploreFetcher.class);

    final CaseKeeperList caseKeeperList = new CaseKeeperList();
    private final CaseKeeper caseKeeper = new CaseKeeper();
    private final UnitFormatter unitFormatter = new UnitFormatter();

    private final HTMLConverter htmlConverter = new HTMLConverter();

    private final JCheckBox absCheckBox = new JCheckBox(Localization.lang("Include abstracts"), false);

    private static final int MAX_FETCH = 100;
    private final int perPage = IEEEXploreFetcher.MAX_FETCH;
    private int hits;
    private int unparseable;
    private int parsed;
    private int piv;
    private boolean shouldContinue;
    private boolean includeAbstract;

    private String terms;
    private final String endUrl = "&rowsPerPage=" + Integer.toString(perPage) + "&pageNumber=";
    private String searchUrl;

    private final Pattern hitsPattern = Pattern.compile("([0-9,]+) Results");
    private final Pattern typePattern = Pattern.compile("<span class=\"type\">\\s*(.+)");
    private final HashMap<String, String> fieldPatterns = new HashMap<>();
    private final Pattern absPattern = Pattern.compile("<p>\\s*(.+)");

    Pattern stdEntryPattern = Pattern.compile(".*<strong>(.+)</strong><br>" + "\\s+(.+)");

    private final Pattern publicationPattern = Pattern.compile("(.*), \\d*\\.*\\s?(.*)");
    private final Pattern proceedingPattern = Pattern.compile("(.*?)\\.?\\s?Proceedings\\s?(.*)");
    Pattern abstractLinkPattern = Pattern.compile("<a href=\'(.+)\'>\\s*<span class=\"more\">View full.*</span> </a>");

    Pattern ieeeArticleNumberPattern = Pattern.compile("<a href=\".*arnumber=(\\d+).*\">");

    private final Pattern authorPattern = Pattern.compile("<span id=\"preferredName\" class=\"(.*)\">");
    private static final String START_URL = "http://ieeexplore.ieee.org/search/freesearchresult.jsp?queryText=";

    private static final String DIALOG_TITLE = Localization.lang("Search %0", "IEEEXplore");


    // Common words in IEEE Xplore that should always be

    public IEEEXploreFetcher() {
        super();
        CookieHandler.setDefault(new CookieManager());

        fieldPatterns.put("title", "<a\\s*href=[^<]+>\\s*(.+)\\s*</a>");
        //fieldPatterns.put("author", "</h3>\\s*(.+)");
        //fieldPatterns.put("author", "(?s)</h3>\\s*(.+)</br>");
        // fieldPatterns.put("author", "<span id=\"preferredName\" class=\"(.+)\">");
        fieldPatterns.put("volume", "Volume:\\s*([A-Za-z-]*\\d+)");
        fieldPatterns.put("number", "Issue:\\s*(\\d+)");
        //fieldPatterns.put("part", "Part (\\d+),&nbsp;(.+)");
        fieldPatterns.put("year", "(?:Copyright|Publication) Year:\\s*(\\d{4})");
        fieldPatterns.put("pages", "Page\\(s\\):\\s*(\\d+)\\s*-\\s*(\\d*)");
        //fieldPatterns.put("doi", "Digital Object Identifier:\\s*<a href=.*>(.+)</a>");
        fieldPatterns.put("doi", "<a href=\"http://dx.doi.org/(.+)\" target");
        fieldPatterns.put("url", "<a href=\"(/stamp/stamp[^\"]+)");
    }

    @Override
    public JPanel getOptionsPanel() {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        pan.add(absCheckBox, BorderLayout.NORTH);

        return pan;
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        terms = query;
        shouldContinue = true;
        parsed = 0;
        unparseable = 0;
        piv = 0;
        int pageNumber = 1;

        searchUrl = makeUrl(pageNumber);//start at page 1

        try {
            URL url = new URL(searchUrl);
            String page = Util.getResults(url);

            if (page.contains("You have entered an invalid search")) {
                status.showMessage(Localization.lang("You have entered an invalid search '%0'.", terms),
                        DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            if (page.contains("Bad request")) {
                status.showMessage(Localization.lang("Bad Request '%0'.", terms),
                        DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            if (page.contains("No results were found.")) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", terms),
                        DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            if (page.contains("Error Page")) {
                // @formatter:off
                status.showMessage(
                        Localization.lang("Intermittent errors on the IEEE Xplore server. Please try again in a while."),
                       DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
                // @formatter:on
                return false;
            }

            hits = getNumberOfHits(page, "display-status", hitsPattern);

            includeAbstract = absCheckBox.isSelected();
            if (hits > IEEEXploreFetcher.MAX_FETCH) {
                // @formatter:off
                status.showMessage(Localization.lang("%0 entries found. To reduce server load, only %1 will be downloaded.",
                        new String[] {String.valueOf(hits), String.valueOf(IEEEXploreFetcher.MAX_FETCH)}),
                        DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
                // @formatter:on
            }

            parse(dialog, page);
            return true;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            status.showMessage(Localization.lang("Connection to IEEEXplore failed"),
                    DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            status.showMessage(e.getMessage(), DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);
            LOGGER.warn("Search IEEEXplore: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public String getTitle() {
        return "IEEEXplore";
    }

    @Override
    public String getHelpPage() {
        return "IEEEXploreHelp.html";
    }

    /**
     * This method is called by the dialog when the user has cancelled the import.
     */
    @Override
    public void stopFetching() {
        shouldContinue = false;
    }

    private String makeUrl(int startIndex) {
        return IEEEXploreFetcher.START_URL + terms.replaceAll(" ", "+") + endUrl + startIndex;
    }

    private void parse(ImportInspector dialog, String text) {
        BibtexEntry entry;
        while (((entry = parseNextEntry(text)) != null) && shouldContinue) {
            if (entry.getField("title") != null) {
                dialog.addEntry(entry);
                dialog.setProgress(parsed + unparseable, hits);
                parsed++;
            }
        }
    }

    private BibtexEntry cleanup(BibtexEntry entry) {
        if (entry == null) {
            return null;
        }

        // clean up title
        String title = entry.getField("title");
        if (title != null) {
            // USe the alt-text and replace image links
            title = title.replaceAll("[ ]?img src=[^ ]+ alt=\"([^\"]+)\">[ ]?", "\\$$1\\$");
            // Try to sort out most of the /spl / conversions
            // Deal with this specific nested type first
            title = title.replaceAll("/sub /spl infin//", "\\$_\\\\infty\\$");
            title = title.replaceAll("/sup /spl infin//", "\\$\\^\\\\infty\\$");
            // Replace general expressions
            title = title.replaceAll("/[sS]pl ([^/]+)/", "\\$\\\\$1\\$");
            // Deal with subscripts and superscripts
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_CONVERT_TO_EQUATION)) {
                title = title.replaceAll("/sup ([^/]+)/", "\\$\\^\\{$1\\}\\$");
                title = title.replaceAll("/sub ([^/]+)/", "\\$_\\{$1\\}\\$");
                title = title.replaceAll("\\(sup\\)([^(]+)\\(/sup\\)", "\\$\\^\\{$1\\}\\$");
                title = title.replaceAll("\\(sub\\)([^(]+)\\(/sub\\)", "\\_\\{$1\\}\\$");
            } else {
                title = title.replaceAll("/sup ([^/]+)/", "\\\\textsuperscript\\{$1\\}");
                title = title.replaceAll("/sub ([^/]+)/", "\\\\textsubscript\\{$1\\}");
                title = title.replaceAll("\\(sup\\)([^(]+)\\(/sup\\)", "\\\\textsuperscript\\{$1\\}");
                title = title.replaceAll("\\(sub\\)([^(]+)\\(/sub\\)", "\\\\textsubscript\\{$1\\}");
            }

            // Replace \infin with \infty
            title = title.replaceAll("\\\\infin", "\\\\infty");

            // Unit formatting
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                title = unitFormatter.format(title);
            }

            // Automatic case keeping
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                title = caseKeeper.format(title);
            }
            // Write back
            entry.setField("title", title);
        }

        // clean up author
        /*   	String author = (String)entry.getField("author");
           	if (author != null) {
            if (author.indexOf("a href=") >= 0) {  // Author parsing failed because it was empty
        	entry.setField("author","");  // Maybe not needed anymore due to another change
            } else {
            	author = author.replaceAll("\\s+", " ");
            	author = author.replaceAll("\\.", ". ");
            	author = author.replaceAll("([^;]+),([^;]+),([^;]+)","$1,$3,$2"); // Change order in case of Jr. etc
            	author = author.replaceAll("  ", " ");
            	author = author.replaceAll("\\. -", ".-");
                       author = author.replaceAll("; ", " and ");
            	author = author.replaceAll(" ,", ",");
            	author = author.replaceAll("  ", " ");
            	author = author.replaceAll("[ ,;]+$", "");
            	entry.setField("author", author);
            }
        }*/
        // clean up month
        String month = entry.getField("month");
        if ((month != null) && !month.isEmpty()) {
            month = month.replaceAll("\\.", "");
            month = month.toLowerCase();

            Pattern monthPattern = Pattern.compile("(\\d*+)\\s*([a-z]*+)-*(\\d*+)\\s*([a-z]*+)");
            Matcher mm = monthPattern.matcher(month);
            String date = month;
            if (mm.find()) {
                if (mm.group(3).isEmpty()) {
                    if (!mm.group(2).isEmpty()) {
                        date = "#" + mm.group(2).substring(0, 3) + "#";
                        if (!mm.group(1).isEmpty()) {
                            date += " " + mm.group(1) + ",";
                        }
                    } else {
                        date = mm.group(1) + ",";
                    }
                } else if (mm.group(2).isEmpty()) {
                    if (!mm.group(4).isEmpty()) {
                        date = "#" + mm.group(4).substring(0, 3) + "# " + mm.group(1) + "--" + mm.group(3) + ",";
                    } else {
                        date += ",";
                    }
                } else {
                    date = "#" + mm.group(2).substring(0, 3) + "# " + mm.group(1) + "--#" + mm.group(4).substring(0, 3)
                            + "# " + mm.group(3) + ",";
                }
            }
            //date = date.trim();
            //if (!date.isEmpty()) {
            entry.setField("month", date);
            //}
        }

        // clean up pages
        String field = "pages";
        String pages = entry.getField(field);
        if (pages != null) {
            String[] pageNumbers = pages.split("-");
            if (pageNumbers.length == 2) {
                if (pageNumbers[0].equals(pageNumbers[1])) {// single page
                    entry.setField(field, pageNumbers[0]);
                } else {
                    entry.setField(field, pages.replaceAll("-", "--"));
                }
            }
        }

        // clean up publication field
        EntryType type = entry.getType();
        String sourceField = "";
        if ("Article".equals(type.getName())) {
            sourceField = "journal";
            entry.clearField("booktitle");
        } else if ("Inproceedings".equals(type.getName())) {
            sourceField = "booktitle";
        }
        String fullName = entry.getField(sourceField);
        if (fullName != null) {
            if ("Article".equals(type.getName())) {
                int ind = fullName.indexOf(": Accepted for future publication");
                if (ind > 0) {
                    fullName = fullName.substring(0, ind);
                    entry.setField("year", "to be published");
                    entry.clearField("month");
                    entry.clearField("pages");
                    entry.clearField("number");
                }
                String[] parts = fullName.split("[\\[\\]]"); //[see also...], [legacy...]
                fullName = parts[0];
                if (parts.length == 3) {
                    fullName += parts[2];
                }
                String note = entry.getField("note");
                if ("Early Access".equals(note)) {
                    entry.setField("year", "to be published");
                    entry.clearField("month");
                    entry.clearField("pages");
                    entry.clearField("number");
                }
            } else {
                fullName = fullName.replace("Conference Proceedings", "Proceedings")
                        .replace("Proceedings of", "Proceedings").replace("Proceedings.", "Proceedings");
                fullName = fullName.replaceAll("International", "Int.");
                fullName = fullName.replaceAll("Symposium", "Symp.");
                fullName = fullName.replaceAll("Conference", "Conf.");
                fullName = fullName.replaceAll(" on", " ").replace("  ", " ");
            }

            Matcher m1 = publicationPattern.matcher(fullName);
            String abrvPattern = ".*[^,] '?\\d+\\)?";
            if (m1.find()) {
                String prefix = m1.group(2).trim();
                String postfix = m1.group(1).trim();
                String abrv = "";
                String[] parts = prefix.split("\\. ", 2);
                if (parts.length == 2) {
                    if (parts[0].matches(abrvPattern)) {
                        prefix = parts[1];
                        abrv = parts[0];
                    } else {
                        prefix = parts[0];
                        abrv = parts[1];
                    }
                }
                if (!prefix.matches(abrvPattern)) {
                    fullName = prefix + " " + postfix + " " + abrv;
                    fullName = fullName.trim();
                } else {
                    fullName = postfix + " " + prefix;
                }
            }
            if ("Article".equals(type.getName())) {
                fullName = fullName.replace(" - ", "-"); //IEE Proceedings-

                fullName = fullName.trim();
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_IEEE_ABRV)) {
                    fullName = Abbreviations.journalAbbrev.getMedlineAbbreviation(fullName).orElse(fullName);
                }
            }
            if ("Inproceedings".equals(type.getName())) {
                Matcher m2 = proceedingPattern.matcher(fullName);
                if (m2.find()) {
                    String prefix = m2.group(2);
                    String postfix = m2.group(1).replaceAll("\\.$", "");
                    if (!prefix.matches(abrvPattern)) {
                        String abrv = "";

                        String[] parts = postfix.split("\\. ", 2);
                        if (parts.length == 2) {
                            if (parts[0].matches(abrvPattern)) {
                                postfix = parts[1];
                                abrv = parts[0];
                            } else {
                                postfix = parts[0];
                                abrv = parts[1];
                            }
                        }
                        fullName = prefix.trim() + " " + postfix.trim() + " " + abrv;

                    } else {
                        fullName = postfix.trim() + " " + prefix.trim();
                    }

                }

                fullName = fullName.trim();

                fullName = fullName.replaceAll("^[tT]he ", "").replaceAll("^\\d{4} ", "").replaceAll("[,.]$", "");
                String year = entry.getField("year");
                if (year != null) {
                    fullName = fullName.replaceAll(", " + year + "\\.?", "");
                }

                if (!fullName.contains("Abstract") && !fullName.contains("Summaries")
                        && !fullName.contains("Conference Record")) {
                    fullName = "Proc. " + fullName;
                }
            }
            entry.setField(sourceField, fullName);
        }

        // clean up abstract
        String abstr = entry.getField("abstract");
        if (abstr != null) {
            // Try to sort out most of the /spl / conversions
            // Deal with this specific nested type first
            abstr = abstr.replaceAll("/sub /spl infin//", "\\$_\\\\infty\\$");
            abstr = abstr.replaceAll("/sup /spl infin//", "\\$\\^\\\\infty\\$");
            // Replace general expressions
            abstr = abstr.replaceAll("/[sS]pl ([^/]+)/", "\\$\\\\$1\\$");
            // Deal with subscripts and superscripts
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_CONVERT_TO_EQUATION)) {
                abstr = abstr.replaceAll("/sup ([^/]+)/", "\\$\\^\\{$1\\}\\$");
                abstr = abstr.replaceAll("/sub ([^/]+)/", "\\$_\\{$1\\}\\$");
                abstr = abstr.replaceAll("\\(sup\\)([^(]+)\\(/sup\\)", "\\$\\^\\{$1\\}\\$");
                abstr = abstr.replaceAll("\\(sub\\)([^(]+)\\(/sub\\)", "\\_\\{$1\\}\\$");
            } else {
                abstr = abstr.replaceAll("/sup ([^/]+)/", "\\\\textsuperscript\\{$1\\}");
                abstr = abstr.replaceAll("/sub ([^/]+)/", "\\\\textsubscript\\{$1\\}");
                abstr = abstr.replaceAll("\\(sup\\)([^(]+)\\(/sup\\)", "\\\\textsuperscript\\{$1\\}");
                abstr = abstr.replaceAll("\\(sub\\)([^(]+)\\(/sub\\)", "\\\\textsubscript\\{$1\\}");
            }
            // Replace \infin with \infty
            abstr = abstr.replaceAll("\\\\infin", "\\\\infty");
            // Write back
            entry.setField("abstract", abstr);
        }

        // Clean up url
        String url = entry.getField("url");
        if (url != null) {
            entry.setField("url", "http://ieeexplore.ieee.org" + url.replace("tp=&", ""));
        }
        return entry;
    }

    private BibtexEntry parseNextEntry(String allText) {
        BibtexEntry entry = null;

        int index = allText.indexOf("<div class=\"detail", piv);
        int endIndex = allText.indexOf("</div>", index);

        if ((index >= 0) && (endIndex > 0)) {
            endIndex += 6;
            piv = endIndex;
            String text = allText.substring(index, endIndex);

            EntryType type = null;
            String sourceField = null;

            String typeName = "";
            Matcher typeMatcher = typePattern.matcher(text);
            if (typeMatcher.find()) {
                typeName = typeMatcher.group(1);
                if ("IEEE Journals &amp; Magazines".equalsIgnoreCase(typeName)
                        || "IEEE Early Access Articles".equalsIgnoreCase(typeName)
                        || "IET Journals &amp; Magazines".equalsIgnoreCase(typeName)
                        || "AIP Journals &amp; Magazines".equalsIgnoreCase(typeName)
                        || "AVS Journals &amp; Magazines".equalsIgnoreCase(typeName)
                        || "IBM Journals &amp; Magazines".equalsIgnoreCase(typeName)
                        || "TUP Journals &amp; Magazines".equalsIgnoreCase(typeName)
                        || "BIAI Journals &amp; Magazines".equalsIgnoreCase(typeName)
                        || "MIT Press Journals".equalsIgnoreCase(typeName)
                        || "Alcatel-Lucent Journal".equalsIgnoreCase(typeName)) {
                    type = EntryTypes.getType("article");
                    sourceField = "journal";
                } else if ("IEEE Conference Publications".equalsIgnoreCase(typeName)
                        || "IET Conference Publications".equalsIgnoreCase(typeName)
                        || "VDE Conference Publications".equalsIgnoreCase(typeName)) {
                    type = EntryTypes.getType("inproceedings");
                    sourceField = "booktitle";
                } else if ("IEEE Standards".equalsIgnoreCase(typeName) || "Standards".equalsIgnoreCase(typeName)) {
                    type = EntryTypes.getType("standard");
                    sourceField = "number";
                } else if ("IEEE eLearning Library Courses".equalsIgnoreCase(typeName)) {
                    type = EntryTypes.getType("electronic");
                    sourceField = "note";
                } else if ("Wiley-IEEE Press eBook Chapters".equalsIgnoreCase(typeName)
                        || "MIT Press eBook Chapters".equalsIgnoreCase(typeName)
                        || "IEEE USA Books &amp; eBooks".equalsIgnoreCase(typeName)) {
                    type = EntryTypes.getType("incollection");
                    sourceField = "booktitle";
                } else if ("Morgan and Claypool eBooks".equalsIgnoreCase(typeName)) {
                    type = EntryTypes.getType("book");
                    sourceField = "note";
                }
            }

            if (type == null) {
                type = EntryTypes.getType("misc");
                sourceField = "note";
                IEEEXploreFetcher.LOGGER.warn("Type detection failed. Use MISC instead. Type string: " + text);
                unparseable++;
            }

            entry = new BibtexEntry(IdGenerator.next(), type);

            if ("IEEE Standards".equalsIgnoreCase(typeName)) {
                entry.setField("organization", "IEEE");
            }

            if ("Wiley-IEEE Press eBook Chapters".equalsIgnoreCase(typeName)) {
                entry.setField("publisher", "Wiley-IEEE Press");
            } else if ("MIT Press eBook Chapters".equalsIgnoreCase(typeName)) {
                entry.setField("publisher", "MIT Press");
            } else if ("IEEE USA Books &amp; eBooks".equalsIgnoreCase(typeName)) {
                entry.setField("publisher", "IEEE USA");
            } else if ("Morgan \\& Claypool eBooks".equalsIgnoreCase(typeName)) {
                entry.setField("publisher", "Morgan and Claypool");
            }

            if ("IEEE Early Access Articles".equalsIgnoreCase(typeName)) {
                entry.setField("note", "Early Access");
            }

            Set<String> fields = fieldPatterns.keySet();
            for (String field : fields) {
                Matcher fieldMatcher = Pattern.compile(fieldPatterns.get(field)).matcher(text);
                if (fieldMatcher.find()) {
                    entry.setField(field, htmlConverter.format(fieldMatcher.group(1)));
                    if ("title".equals(field) && fieldMatcher.find()) {
                        String sec_title = htmlConverter.format(fieldMatcher.group(1));
                        if (entry.getType() == EntryTypes.getStandardType("standard")) {
                            sec_title = sec_title.replaceAll("IEEE Std ", "");
                        }
                        entry.setField(sourceField, sec_title);

                    }
                    if ("pages".equals(field) && (fieldMatcher.groupCount() == 2)) {
                        entry.setField(field, fieldMatcher.group(1) + "-" + fieldMatcher.group(2));
                    }
                }
            }

            Matcher authorMatcher = authorPattern.matcher(text);
            // System.out.println(text);
            StringBuilder authorNames = new StringBuilder("");
            int authorCount = 0;
            while (authorMatcher.find()) {
                if (authorCount >= 1) {
                    authorNames.append(" and ");
                }
                authorNames.append(htmlConverter.format(authorMatcher.group(1)));
                //System.out.println(authorCount + ": " + authorMatcher.group(1));
                authorCount++;
            }

            String authorString = authorNames.toString();
            if ((authorString == null) || authorString.startsWith("a href") || authorString.startsWith("Topic(s)")) { // Fix for some documents without authors
                entry.setField("author", "");
            } else {
                entry.setField("author", authorString);
            }

            if ((entry.getType() == EntryTypes.getStandardType("inproceedings"))
                    && "".equals(entry.getField("author"))) {
                entry.setType(EntryTypes.getStandardType("proceedings"));
            }

            if (includeAbstract) {
                index = text.indexOf("id=\"abstract");
                if (index >= 0) {
                    endIndex = text.indexOf("</div>", index) + 6;

                    text = text.substring(index, endIndex);
                    Matcher absMatcher = absPattern.matcher(text);
                    if (absMatcher.find()) {
                        // Clean-up abstract
                        String abstr = absMatcher.group(1);
                        abstr = abstr.replaceAll("<span class='snippet'>([\\w]+)</span>", "$1");

                        entry.setField("abstract", htmlConverter.format(abstr));
                    }
                }
            }
        }

        if (entry == null) {
            return null;
        }
        return cleanup(entry);
    }

    /**
     * Find out how many hits were found.
     *
     * @param page
     */
    private static int getNumberOfHits(String page, String marker, Pattern pattern) throws IOException {
        int ind = page.indexOf(marker);
        if (ind < 0) {
            LOGGER.debug(page);
            throw new IOException("Cannot parse number of hits");
        }
        String substring = page.substring(ind, page.length());
        Matcher m = pattern.matcher(substring);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        throw new IOException("Cannot parse number of hits");
    }
}