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
import java.lang.reflect.Array;
import java.net.ConnectException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.model.entry.EntryType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.sf.jabref.*;
import net.sf.jabref.importer.*;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.formatter.bibtexfields.UnitFormatter;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeper;
import net.sf.jabref.logic.formatter.casechanger.CaseKeeperList;
import net.sf.jabref.logic.journals.Abbreviations;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.util.Util;

public class IEEEXploreFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(IEEEXploreFetcher.class);
    private static final String URL_SEARCH = "http://ieeexplore.ieee.org/rest/search?reload=true";
    private static final String URL_BIBTEX_START = "http://ieeexplore.ieee.org/xpl/downloadCitations?reload=true&recordIds=";
    private static final String URL_BIBTEX_END = "&download-format=download-bibtex&x=0&y=0";
    private static final String DIALOG_TITLE = Localization.lang("Search %0", "IEEEXplore");
    private static final int MAX_FETCH = 100;

    final CaseKeeperList caseKeeperList = new CaseKeeperList();
    private final CaseKeeper caseKeeper = new CaseKeeper();
    private final UnitFormatter unitFormatter = new UnitFormatter();
    private final HTMLConverter htmlConverter = new HTMLConverter();
    private final JCheckBox absCheckBox = new JCheckBox(Localization.lang("Include abstracts"), false);

    private boolean shouldContinue;

    public IEEEXploreFetcher() {
        super();
        CookieHandler.setDefault(new CookieManager());
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
        //IEEE API seems to use .QT. as a marker for the quotes for exact phrase searching
        String terms = query.replaceAll("\"", "\\.QT\\.");

        shouldContinue = true;
        int parsed = 0;
        int pageNumber = 1;

        String postData = makeSearchPostRequestPayload(pageNumber, terms);

        try {
            //open the search URL
            URL url = new URL(IEEEXploreFetcher.URL_SEARCH);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            //add request header
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Content-type", "application/json");

            //retrieve the search results
            String page = Util.getPostResults(con, postData, null);

            //the page can be blank if the search did not work (not sure the exact conditions that lead to this, but declaring it an invalid search for now)
            if (page.isEmpty()) {
                status.showMessage(Localization.lang("You have entered an invalid search '%0'.", query),
                        DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            //parses the JSON data returned by the query
            //TODO: a faster way would be to parse the JSON tokens one at a time just to extract the article number, but this seems to be fast enough...
            JSONObject searchResultsJson = new JSONObject(page);
            int hits = searchResultsJson.getInt("totalRecords");

            //if no search results were found
            if (hits == 0) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", query),
                        DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            //if max hits were exceeded, display the warning
            if (hits > IEEEXploreFetcher.MAX_FETCH) {
                status.showMessage(
                        Localization.lang("%0 entries found. To reduce server load, only %1 will be downloaded.",
                                String.valueOf(hits), String.valueOf(IEEEXploreFetcher.MAX_FETCH)),
                        DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
            }

            //fetch the raw Bibtex results from IEEEXplore
            URL bibtexURL = new URL(createBibtexQueryURL(searchResultsJson));
            String bibtexPage = Util.getResults(bibtexURL);

            //preprocess the result (eg. convert HTML escaped characters to latex and do other formatting not performed by BibtexParser)
            bibtexPage = preprocessBibtexResultsPage(bibtexPage);

            //parse the page into Bibtex entries
            Collection<BibtexEntry> parsedBibtexCollection = BibtexParser.fromString(bibtexPage);
            if (parsedBibtexCollection == null) {
                status.showMessage(Localization.lang("Error occured parsing Bibtex returned from IEEEXplore"),
                        DIALOG_TITLE, JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            int nEntries = parsedBibtexCollection.size();
            Iterator<BibtexEntry> parsedBibtexCollectionIterator = parsedBibtexCollection.iterator();
            while (parsedBibtexCollectionIterator.hasNext() && shouldContinue) {
                dialog.addEntry(cleanup(parsedBibtexCollectionIterator.next()));
                dialog.setProgress(parsed, nEntries);
                parsed++;
            }

            return true;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            status.showMessage(Localization.lang("Connection to IEEEXplore failed"), DIALOG_TITLE,
                    JOptionPane.ERROR_MESSAGE);
        } catch (UnknownHostException e) {
            status.showMessage(Localization.lang("Connection to IEEEXplore failed"), DIALOG_TITLE,
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            status.showMessage(e.getMessage(), DIALOG_TITLE, JOptionPane.ERROR_MESSAGE);
            LOGGER.warn("Search IEEEXplore: " + e.getMessage(), e);
        } catch (JSONException e) {
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

    private String makeSearchPostRequestPayload(int startIndex, String terms) {
        return "{\"queryText\":" + JSONObject.quote(terms) + ",\"refinements\":[],\"pageNumber\":\"" + startIndex
                + "\",\"searchWithin\":[],\"newsearch\":\"true\",\"searchField\":\"Search_All\",\"rowsPerPage\":\"100\"}";
    }

    private String createBibtexQueryURL(JSONObject searchResultsJson) {

        //buffer to use for building the URL for fetching the bibtex data from IEEEXplore
        StringBuffer bibtexQueryURLStringBuf = new StringBuffer();
        bibtexQueryURLStringBuf.append(URL_BIBTEX_START);

        //loop over each record and create a comma-separate list of article numbers which will be used to download the raw Bibtex
        JSONArray recordsJsonArray = searchResultsJson.getJSONArray("records");
        for (int n = 0; n < recordsJsonArray.length(); n++) {
            if (!recordsJsonArray.getJSONObject(n).isNull("articleNumber")) {
                bibtexQueryURLStringBuf.append(recordsJsonArray.getJSONObject(n).getString("articleNumber") + ",");
            }
        }
        //delete the last comma
        bibtexQueryURLStringBuf.deleteCharAt(bibtexQueryURLStringBuf.length() - 1);

        //add the abstract setting
        boolean includeAbstract = absCheckBox.isSelected();
        if (includeAbstract) {
            bibtexQueryURLStringBuf.append("&citations-format=citation-abstract");
        } else {
            bibtexQueryURLStringBuf.append("&citations-format=citation-only");
        }

        //append the remaining URL
        bibtexQueryURLStringBuf.append(URL_BIBTEX_END);

        return bibtexQueryURLStringBuf.toString();
    }

    private String preprocessBibtexResultsPage(String bibtexPage) {
        //for some reason, the escaped HTML characters in the titles are in the format "#xNNNN" (they are missing the ampersand)
        //add the ampersands back in before passing to the HTML formatter so they can be properly converted
        //TODO: Maybe edit the HTMLconverter to also recognize escaped characters even when the & is missing?
        //Pattern escapedPattern = Pattern.compile("(?<!&)#([x]*)([0]*)(\\p{XDigit}+);");
        bibtexPage = bibtexPage.replaceAll("(?<!&)(#[x]*[0]*\\p{XDigit}+;)", "&$1");

        //Also, percent signs are not escaped by the IEEEXplore Bibtex output nor, it would appear, the subsequent processing in JabRef
        //TODO: Maybe find a better spot for this if it applies more universally
        bibtexPage = bibtexPage.replaceAll("(?<!\\\\)%", "\\\\%");

        //Format the bibtexResults using the HTML formatter (clears up numerical and text escaped characters and remaining HTML tags)
        bibtexPage = htmlConverter.format(bibtexPage);

        return bibtexPage;
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
        String author = entry.getField("author");
        if (author != null) {
            author = author.replaceAll("\\s+", " ");

            //reorder the "Jr." "Sr." etc to the correct ordering
            String[] authorSplit = author.split("(^\\s*|\\s*$|\\s+and\\s+)");
            for (int n = 0; n < (Array.getLength(authorSplit)); n++) {
                authorSplit[n] = authorSplit[n].replaceAll("(.+?),(.+?),(.+)", "$1,$3,$2");
            }
            author = String.join(" and ", authorSplit);

            author = author.replaceAll("\\.", ". ");
            author = author.replaceAll("  ", " ");
            author = author.replaceAll("\\. -", ".-");
            author = author.replaceAll("; ", " and ");
            author = author.replaceAll(" ,", ",");
            author = author.replaceAll("  ", " ");
            author = author.replaceAll("[ ,;]+$", "");
            //TODO: remove trailing commas
            entry.setField("author", author);
        }

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

            Pattern publicationPattern = Pattern.compile("(.*), \\d*\\.*\\s?(.*)");

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
                Pattern proceedingPattern = Pattern.compile("(.*?)\\.?\\s?Proceedings\\s?(.*)");
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


}