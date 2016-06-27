package net.sf.jabref.gui.importer.fetcher;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.IEEEXploreFetcher;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IEEEXploreFetcherGUI extends IEEEXploreFetcher implements EntryFetcherGUI {

    private final JCheckBox absCheckBox = new JCheckBox(Localization.lang("Include abstracts"), false);


    public IEEEXploreFetcherGUI(JournalAbbreviationLoader abbreviationLoader) {
        super(abbreviationLoader);
    }

    @Override
    public JPanel getOptionsPanel() {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        pan.add(absCheckBox, BorderLayout.NORTH);

        return pan;
    }

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_IEEEXPLORE;
    }

    private String createBibtexQueryURL(JSONObject searchResultsJson) {

        //buffer to use for building the URL for fetching the bibtex data from IEEEXplore
        StringBuilder bibtexQueryURLStringBuf = new StringBuilder();
        bibtexQueryURLStringBuf.append(getUrlBibtexStart());

        //loop over each record and create a comma-separate list of article numbers which will be used to download the raw Bibtex
        JSONArray recordsJsonArray = searchResultsJson.getJSONArray("records");
        for (int n = 0; n < recordsJsonArray.length(); n++) {
            if (!recordsJsonArray.getJSONObject(n).isNull("articleNumber")) {
                bibtexQueryURLStringBuf.append(recordsJsonArray.getJSONObject(n).getString("articleNumber"))
                        .append(',');
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
        bibtexQueryURLStringBuf.append(getUrlBibtexEnd());

        return bibtexQueryURLStringBuf.toString();
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        //IEEE API seems to use .QT. as a marker for the quotes for exact phrase searching
        String terms = query.replaceAll("\"", "\\.QT\\.");

        setShouldContinue(true);
        int parsed = 0;
        int pageNumber = 1;

        String postData = makeSearchPostRequestPayload(pageNumber, terms);

        try {
            //open the search URL
            URLDownload dl = new URLDownload(getUrlSearch());

            //add request header
            dl.addParameters("Accept", "application/json");
            dl.addParameters("Content-Type", "application/json");

            // set post data
            dl.setPostData(postData);

            //retrieve the search results
            String page = dl.downloadToString(StandardCharsets.UTF_8);

            //the page can be blank if the search did not work (not sure the exact conditions that lead to this, but declaring it an invalid search for now)
            if (page.isEmpty()) {
                status.showMessage(Localization.lang("You have entered an invalid search '%0'.", query),
                        getDialogTitle(), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            //parses the JSON data returned by the query
            //TODO: a faster way would be to parse the JSON tokens one at a time just to extract the article number, but this seems to be fast enough...
            JSONObject searchResultsJson = new JSONObject(page);
            int hits = searchResultsJson.getInt("totalRecords");

            //if no search results were found
            if (hits == 0) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", query),
                        getDialogTitle(), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }

            //if max hits were exceeded, display the warning
            if (hits > getMaxFetch()) {
                status.showMessage(
                        Localization.lang("%0 entries found. To reduce server load, only %1 will be downloaded.",
                                String.valueOf(hits), String.valueOf(IEEEXploreFetcher.getMaxFetch())),
                        getDialogTitle(), JOptionPane.INFORMATION_MESSAGE);
            }

            //fetch the raw Bibtex results from IEEEXplore
            String bibtexPage = new URLDownload(createBibtexQueryURL(searchResultsJson)).downloadToString();

            //preprocess the result (eg. convert HTML escaped characters to latex and do other formatting not performed by BibtexParser)
            bibtexPage = preprocessBibtexResultsPage(bibtexPage);

            //parse the page into Bibtex entries
            Collection<BibEntry> parsedBibtexCollection = BibtexParser.fromString(bibtexPage);
            if (parsedBibtexCollection == null) {
                status.showMessage(Localization.lang("Error while fetching from %0", getTitle()), getDialogTitle(),
                        JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            int nEntries = parsedBibtexCollection.size();
            Iterator<BibEntry> parsedBibtexCollectionIterator = parsedBibtexCollection.iterator();
            while (parsedBibtexCollectionIterator.hasNext() && isShouldContinue()) {
                dialog.addEntry(cleanup(parsedBibtexCollectionIterator.next()));
                dialog.setProgress(parsed, nEntries);
                parsed++;
            }

            return true;

        } catch (MalformedURLException e) {
            getLogger().warn("Bad URL", e);
        } catch (ConnectException | UnknownHostException e) {
            status.showMessage(Localization.lang("Could not connect to %0", getTitle()), getDialogTitle(),
                    JOptionPane.ERROR_MESSAGE);
        } catch (IOException | JSONException e) {
            status.showMessage(e.getMessage(), getDialogTitle(), JOptionPane.ERROR_MESSAGE);
            getLogger().warn("Search IEEEXplore: " + e.getMessage(), e);
        }

        return false;
    }

    private String preprocessBibtexResultsPage(String bibtexPage) {
        //for some reason, the escaped HTML characters in the titles are in the format "#xNNNN" (they are missing the ampersand)
        //add the ampersands back in before passing to the HTML formatter so they can be properly converted
        //TODO: Maybe edit the HTMLconverter to also recognize escaped characters even when the & is missing?
        String result = getPreprocessingPattern().matcher(bibtexPage).replaceAll("&$1");

        //Also, percent signs are not escaped by the IEEEXplore Bibtex output nor, it would appear, the subsequent processing in JabRef
        //TODO: Maybe find a better spot for this if it applies more universally
        result = result.replaceAll("(?<!\\\\)%", "\\\\%");

        //Format the bibtexResults using the HTML formatter (clears up numerical and text escaped characters and remaining HTML tags)
        result = getHtmlToLatexFormatter().format(result);

        return result;
    }

    private String makeSearchPostRequestPayload(int startIndex, String terms) {
        return "{\"queryText\":" + JSONObject.quote(terms) + ",\"refinements\":[],\"pageNumber\":\"" + startIndex
                + "\",\"searchWithin\":[],\"newsearch\":\"true\",\"searchField\":\"Search_All\",\"rowsPerPage\":\"100\"}";
    }

    private BibEntry cleanup(BibEntry entry) {
        if (entry == null) {
            return null;
        }

        // clean up title
        if (entry.hasField("title")) {
            String title = entry.getField("title");
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
                title = getSuperDetection1().matcher(title).replaceAll(getSuperEqResult());
                title = getSubDetection1().matcher(title).replaceAll(getSubEqResult());
                title = getSuperDetection2().matcher(title).replaceAll(getSuperEqResult());
                title = getSubDetection2().matcher(title).replaceAll(getSubEqResult());
            } else {
                title = getSuperDetection1().matcher(title).replaceAll(getSuperTextResult());
                title = getSubDetection1().matcher(title).replaceAll(getSubTextResult());
                title = getSuperDetection2().matcher(title).replaceAll(getSuperTextResult());
                title = getSubDetection2().matcher(title).replaceAll(getSubTextResult());
            }

            // Replace \infin with \infty
            title = title.replaceAll("\\\\infin", "\\\\infty");

            // Unit formatting
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                title = getUnitsToLatexFormatter().format(title);
            }

            // Automatic case keeping
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                title = getProtectTermsFormatter().format(title);
            }
            // Write back
            entry.setField("title", title);
        }

        // clean up author
        if (entry.hasField("author")) {
            String author = entry.getField("author");
            author = author.replaceAll("\\s+", " ");

            //reorder the "Jr." "Sr." etc to the correct ordering
            String[] authorSplit = author.split("(^\\s*|\\s*$|\\s+and\\s+)");
            List<String> authorResult = new ArrayList<>();
            for (String authorSplitPart : authorSplit) {
                authorResult.add(authorSplitPart.replaceAll("(.+?),(.+?),(.+)", "$1,$3,$2"));
            }
            author = String.join(" and ", authorResult);

            author = author.replace(".", ". ").replace("  ", " ").replace(". -", ".-").replace("; ", " and ")
                    .replace(" ,", ",").replace("  ", " ");
            author = author.replaceAll("[ ,;]+$", "");
            //TODO: remove trailing commas
            entry.setField("author", author);
        }

        // clean up month
        String month = entry.getField("month");
        if ((month != null) && !month.isEmpty()) {
            month = month.replace(".", "");
            month = month.toLowerCase();

            Matcher monthMatcher = getMonthPattern().matcher(month);
            StringBuilder date = new StringBuilder(month);
            if (monthMatcher.find()) {
                if (monthMatcher.group(3).isEmpty()) {
                    if (monthMatcher.group(2).isEmpty()) {
                        date = new StringBuilder().append(monthMatcher.group(1)).append(',');
                    } else {
                        date = new StringBuilder().append('#').append(monthMatcher.group(2).substring(0, 3))
                                .append('#');
                        if (!monthMatcher.group(1).isEmpty()) {
                            date.append(' ').append(monthMatcher.group(1)).append(',');
                        }
                    }
                } else if (monthMatcher.group(2).isEmpty()) {
                    if (monthMatcher.group(4).isEmpty()) {
                        date.append(',');
                    } else {
                        date = new StringBuilder().append('#').append(monthMatcher.group(4).substring(0, 3)).append('#')
                                .append(monthMatcher.group(1)).append("--").append(monthMatcher.group(3)).append(',');
                    }
                } else {
                    date = new StringBuilder().append('#').append(monthMatcher.group(2).substring(0, 3)).append('#')
                            .append(monthMatcher.group(1)).append("--#").append(monthMatcher.group(4).substring(0, 3))
                            .append('#').append(monthMatcher.group(3)).append(',');
                }
            }
            entry.setField("month", date.toString());
        }

        // clean up pages
        if (entry.hasField("pages")) {
            String pages = entry.getField("pages");
            String[] pageNumbers = pages.split("-");
            if (pageNumbers.length == 2) {
                if (pageNumbers[0].equals(pageNumbers[1])) {// single page
                    entry.setField("pages", pageNumbers[0]);
                } else {
                    entry.setField("pages", pages.replace("-", "--"));
                }
            }
        }

        // clean up publication field
        String type = entry.getType();
        String sourceField = "";
        if ("article".equals(type)) {
            sourceField = "journal";
            entry.clearField("booktitle");
        } else if ("inproceedings".equals(type)) {
            sourceField = "booktitle";
        }
        if (entry.hasField(sourceField)) {
            String fullName = entry.getField(sourceField);
            if ("article".equals(type)) {
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
                fullName = fullName.replace("International", "Int.");
                fullName = fullName.replace("Symposium", "Symp.");
                fullName = fullName.replace("Conference", "Conf.");
                fullName = fullName.replace(" on", " ").replace("  ", " ");
            }

            Matcher m1 = getPublicationPattern().matcher(fullName);
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
                if (prefix.matches(abrvPattern)) {
                    fullName = postfix + " " + prefix;
                } else {
                    fullName = prefix + " " + postfix + " " + abrv;
                    fullName = fullName.trim();
                }
            }
            if ("article".equals(type)) {
                fullName = fullName.replace(" - ", "-"); //IEE Proceedings-

                fullName = fullName.trim();
                if (Globals.prefs.getBoolean(JabRefPreferences.USE_IEEE_ABRV)) {
                    fullName = getAbbreviationLoader().getRepository().getMedlineAbbreviation(fullName)
                            .orElse(fullName);
                }
            }
            if ("inproceedings".equals(type)) {
                Matcher m2 = getProceedingsPattern().matcher(fullName);
                if (m2.find()) {
                    String prefix = m2.group(2);
                    String postfix = m2.group(1).replaceAll("\\.$", "");
                    if (prefix.matches(abrvPattern)) {
                        fullName = postfix.trim() + " " + prefix.trim();
                    } else {
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
        if (entry.hasField("abstract")) {
            String abstr = entry.getField("abstract");
            // Try to sort out most of the /spl / conversions
            // Deal with this specific nested type first
            abstr = abstr.replaceAll("/sub /spl infin//", "\\$_\\\\infty\\$");
            abstr = abstr.replaceAll("/sup /spl infin//", "\\$\\^\\\\infty\\$");
            // Replace general expressions
            abstr = abstr.replaceAll("/[sS]pl ([^/]+)/", "\\$\\\\$1\\$");
            // Deal with subscripts and superscripts
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_CONVERT_TO_EQUATION)) {
                abstr = getSuperDetection1().matcher(abstr).replaceAll(getSuperEqResult());
                abstr = getSubDetection1().matcher(abstr).replaceAll(getSubEqResult());
                abstr = getSuperDetection2().matcher(abstr).replaceAll(getSuperEqResult());
                abstr = getSubDetection2().matcher(abstr).replaceAll(getSubEqResult());
            } else {
                abstr = getSuperDetection1().matcher(abstr).replaceAll(getSuperTextResult());
                abstr = getSubDetection1().matcher(abstr).replaceAll(getSubTextResult());
                abstr = getSuperDetection2().matcher(abstr).replaceAll(getSuperTextResult());
                abstr = getSubDetection2().matcher(abstr).replaceAll(getSubTextResult());
            }
            // Replace \infin with \infty
            abstr = abstr.replace("\\infin", "\\infty");
            // Write back
            entry.setField("abstract", abstr);
        }

        // Clean up url
        entry.getFieldOptional("url")
                .ifPresent(url -> entry.setField("url", "http://ieeexplore.ieee.org" + url.replace("tp=&", "")));

        // Replace ; as keyword separator
        entry.getFieldOptional("keywords").ifPresent(keys -> entry.setField("keywords",
                keys.replace(";", Globals.prefs.get(JabRefPreferences.GROUP_KEYWORD_SEPARATOR))));
        return entry;
    }

}
