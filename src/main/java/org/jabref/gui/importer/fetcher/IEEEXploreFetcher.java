package org.jabref.gui.importer.fetcher;

import java.awt.BorderLayout;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.jabref.Globals;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.ImportInspector;
import org.jabref.logic.importer.OutputPrinter;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IEEEXploreFetcher implements EntryFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IEEEXploreFetcher.class);
    private static final String URL_SEARCH = "https://ieeexplore.ieee.org/rest/search?reload=true";
    private static final String URL_BIBTEX_START = "https://ieeexplore.ieee.org/xpl/downloadCitations?reload=true&recordIds=";
    private static final String URL_BIBTEX_END = "&download-format=download-bibtex&x=0&y=0";
    private static final String DIALOG_TITLE = Localization.lang("Search %0", "IEEEXplore");
    private static final int MAX_FETCH = 100;

    private static final Pattern PUBLICATION_PATTERN = Pattern.compile("(.*), \\d*\\.*\\s?(.*)");
    private static final Pattern PROCEEDINGS_PATTERN = Pattern.compile("(.*?)\\.?\\s?Proceedings\\s?(.*)");
    private static final Pattern MONTH_PATTERN = Pattern.compile("(\\d*+)\\s*([a-z]*+)-*(\\d*+)\\s*([a-z]*+)");

    private static final Pattern PREPROCESSING_PATTERN = Pattern.compile("(?<!&)(#[x]*[0]*\\p{XDigit}+;)");

    private static final Pattern SUB_DETECTION_1 = Pattern.compile("/sub ([^/]+)/");
    private static final Pattern SUB_DETECTION_2 = Pattern.compile("\\(sub\\)([^(]+)\\(/sub\\)");
    private static final String SUB_TEXT_RESULT = "\\\\textsubscript\\{$1\\}";
    private static final Pattern SUPER_DETECTION_1 = Pattern.compile("/sup ([^/]+)/");
    private static final Pattern SUPER_DETECTION_2 = Pattern.compile("\\(sup\\)([^(]+)\\(/sup\\)");
    private static final String SUPER_TEXT_RESULT = "\\\\textsuperscript\\{$1\\}";

    private final Formatter protectTermsFormatter = new ProtectTermsFormatter(Globals.protectedTermsLoader);
    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();
    private final HtmlToLatexFormatter htmlToLatexFormatter = new HtmlToLatexFormatter();
    private final JCheckBox absCheckBox = new JCheckBox(Localization.lang("Include abstracts"), false);
    private final JournalAbbreviationLoader abbreviationLoader;
    private boolean shouldContinue;


    public IEEEXploreFetcher(JournalAbbreviationLoader abbreviationLoader) {
        super();
        this.abbreviationLoader = Objects.requireNonNull(abbreviationLoader);
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
            URLDownload dl = new URLDownload(IEEEXploreFetcher.URL_SEARCH);

            //add request header
            dl.addHeader("Accept", "application/json");
            dl.addHeader("Content-Type", "application/json");
            dl.addHeader("Referer", "https://ieeexplore.ieee.org/search/searchresult.jsp");

            // set post data
            dl.setPostData(postData);

            //retrieve the search results
            String page = dl.asString();

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
            String bibtexPage = new URLDownload(createBibtexQueryURL(searchResultsJson))
                    .asString(Globals.prefs.getDefaultEncoding());

            //preprocess the result (eg. convert HTML escaped characters to latex and do other formatting not performed by BibtexParser)
            bibtexPage = preprocessBibtexResultsPage(bibtexPage);

            //parse the page into Bibtex entries
            Collection<BibEntry> parsedBibtexCollection = new BibtexParser(Globals.prefs.getImportFormatPreferences(), Globals.getFileUpdateMonitor())
                    .parseEntries(bibtexPage);
            int nEntries = parsedBibtexCollection.size();
            Iterator<BibEntry> parsedBibtexCollectionIterator = parsedBibtexCollection.iterator();
            while (parsedBibtexCollectionIterator.hasNext() && shouldContinue) {
                dialog.addEntry(cleanup(parsedBibtexCollectionIterator.next()));
                dialog.setProgress(parsed, nEntries);
                parsed++;
            }

            return true;

        } catch (ParseException | IOException | JSONException e) {
            LOGGER.error("Error while fetching from " + getTitle(), e);
            ((ImportInspectionDialog)dialog).showErrorMessage(this.getTitle(), e.getLocalizedMessage());
        }

        return false;
    }

    @Override
    public String getTitle() {
        return "IEEEXplore";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_IEEEXPLORE;
    }

    /**
     * This method is called by the dialog when the user has canceled the import.
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
        StringBuilder bibtexQueryURLStringBuf = new StringBuilder();
        bibtexQueryURLStringBuf.append(URL_BIBTEX_START);

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
        bibtexQueryURLStringBuf.append(URL_BIBTEX_END);

        return bibtexQueryURLStringBuf.toString();
    }

    private String preprocessBibtexResultsPage(String bibtexPage) {
        //for some reason, the escaped HTML characters in the titles are in the format "#xNNNN" (they are missing the ampersand)
        //add the ampersands back in before passing to the HTML formatter so they can be properly converted
        //TODO: Maybe edit the HTMLconverter to also recognize escaped characters even when the & is missing?
        String result = PREPROCESSING_PATTERN.matcher(bibtexPage).replaceAll("&$1");

        //Also, percent signs are not escaped by the IEEEXplore Bibtex output nor, it would appear, the subsequent processing in JabRef
        //TODO: Maybe find a better spot for this if it applies more universally
        result = result.replaceAll("(?<!\\\\)%", "\\\\%");

        //Format the bibtexResults using the HTML formatter (clears up numerical and text escaped characters and remaining HTML tags)
        result = htmlToLatexFormatter.format(result);

        return result;
    }

    private BibEntry cleanup(BibEntry entry) {
        if (entry == null) {
            return null;
        }

        // clean up title
        entry.getField(FieldName.TITLE).ifPresent(dirtyTitle -> {
            // USe the alt-text and replace image links
            String title = dirtyTitle.replaceAll("[ ]?img src=[^ ]+ alt=\"([^\"]+)\">[ ]?", "\\$$1\\$");
            // Try to sort out most of the /spl / conversions
            // Deal with this specific nested type first
            title = title.replaceAll("/sub /spl infin//", "\\$_\\\\infty\\$");
            title = title.replaceAll("/sup /spl infin//", "\\$\\^\\\\infty\\$");
            // Replace general expressions
            title = title.replaceAll("/[sS]pl ([^/]+)/", "\\$\\\\$1\\$");
            // Deal with subscripts and superscripts
            title = SUPER_DETECTION_1.matcher(title).replaceAll(SUPER_TEXT_RESULT);
            title = SUB_DETECTION_1.matcher(title).replaceAll(SUB_TEXT_RESULT);
            title = SUPER_DETECTION_2.matcher(title).replaceAll(SUPER_TEXT_RESULT);
            title = SUB_DETECTION_2.matcher(title).replaceAll(SUB_TEXT_RESULT);

            // Replace \infin with \infty
            title = title.replaceAll("\\\\infin", "\\\\infty");

            // Unit formatting
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                title = unitsToLatexFormatter.format(title);
            }

            // Automatic case keeping
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                title = protectTermsFormatter.format(title);
            }
            // Write back
            entry.setField(FieldName.TITLE, title);
        });

        // clean up author
        entry.getField(FieldName.AUTHOR).ifPresent(dirtyAuthor -> {
            String author = dirtyAuthor.replaceAll("\\s+", " ");

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
            entry.setField(FieldName.AUTHOR, author);
        });

        // clean up month
        entry.getField(FieldName.MONTH).filter(month -> !month.isEmpty()).ifPresent(dirtyMonth -> {
            String month = dirtyMonth.replace(".", "");
            month = month.toLowerCase(Locale.ROOT);

            Matcher mm = MONTH_PATTERN.matcher(month);
            StringBuilder date = new StringBuilder(month);
            if (mm.find()) {
                if (mm.group(3).isEmpty()) {
                    if (mm.group(2).isEmpty()) {
                        date = new StringBuilder().append(mm.group(1)).append(',');
                    } else {
                        date = new StringBuilder().append('#').append(mm.group(2).substring(0, 3)).append('#');
                        if (!mm.group(1).isEmpty()) {
                            date.append(' ').append(mm.group(1)).append(',');
                        }
                    }
                } else if (mm.group(2).isEmpty()) {
                    if (mm.group(4).isEmpty()) {
                        date.append(',');
                    } else {
                        date = new StringBuilder().append('#').append(mm.group(4).substring(0, 3)).append('#')
                                .append(mm.group(1)).append("--").append(mm.group(3)).append(',');
                    }
                } else {
                    date = new StringBuilder().append('#').append(mm.group(2).substring(0, 3)).append('#')
                            .append(mm.group(1)).append("--#").append(mm.group(4).substring(0, 3)).append('#')
                            .append(mm.group(3)).append(',');
                }
            }
            entry.setField(FieldName.MONTH, date.toString());
        });

        // clean up pages
        entry.getField(FieldName.PAGES).ifPresent(pages -> {
            String[] pageNumbers = pages.split("-");
            if (pageNumbers.length == 2) {
                if (pageNumbers[0].equals(pageNumbers[1])) { // single page
                    entry.setField(FieldName.PAGES, pageNumbers[0]);
                } else {
                    entry.setField(FieldName.PAGES, pages.replace("-", "--"));
                }
            }
        });

        // clean up publication field
        String type = entry.getType();
        String sourceField = "";
        if ("article".equals(type)) {
            sourceField = FieldName.JOURNAL;
            entry.clearField(FieldName.BOOKTITLE);
        } else if ("inproceedings".equals(type)) {
            sourceField = FieldName.BOOKTITLE;
        }
        if (entry.hasField(sourceField)) {
            String fullName = entry.getField(sourceField).get();
            if ("article".equals(type)) {
                int ind = fullName.indexOf(": Accepted for future publication");
                if (ind > 0) {
                    fullName = fullName.substring(0, ind);
                    entry.setField(FieldName.YEAR, "to be published");
                    entry.clearField(FieldName.MONTH);
                    entry.clearField(FieldName.PAGES);
                    entry.clearField(FieldName.NUMBER);
                }
                String[] parts = fullName.split("[\\[\\]]"); //[see also...], [legacy...]
                fullName = parts[0];
                if (parts.length == 3) {
                    fullName += parts[2];
                }
                entry.getField(FieldName.NOTE).filter(note -> "Early Access".equals(note)).ifPresent(note -> {
                    entry.setField(FieldName.YEAR, "to be published");
                    entry.clearField(FieldName.MONTH);
                    entry.clearField(FieldName.PAGES);
                    entry.clearField(FieldName.NUMBER);
                });
            } else {
                fullName = fullName.replace("Conference Proceedings", "Proceedings")
                        .replace("Proceedings of", "Proceedings").replace("Proceedings.", "Proceedings");
                fullName = fullName.replace("International", "Int.");
                fullName = fullName.replace("Symposium", "Symp.");
                fullName = fullName.replace("Conference", "Conf.");
                fullName = fullName.replace(" on", " ").replace("  ", " ");
            }

            Matcher m1 = PUBLICATION_PATTERN.matcher(fullName);
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
                JournalAbbreviationPreferences journalAbbreviationPreferences = Globals.prefs.getJournalAbbreviationPreferences();
                if (journalAbbreviationPreferences.useIEEEAbbreviations()) {
                    fullName = abbreviationLoader
                            .getRepository(journalAbbreviationPreferences)
                            .getMedlineAbbreviation(fullName)
                            .orElse(fullName);
                }
            }
            if ("inproceedings".equals(type)) {
                Matcher m2 = PROCEEDINGS_PATTERN.matcher(fullName);
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
                Optional<String> year = entry.getField(FieldName.YEAR);
                if (year.isPresent()) {
                    fullName = fullName.replaceAll(", " + year.get() + "\\.?", "");
                }

                if (!fullName.contains("Abstract") && !fullName.contains("Summaries")
                        && !fullName.contains("Conference Record")) {
                    fullName = "Proc. " + fullName;
                }
            }
            entry.setField(sourceField, fullName);
        }

        // clean up abstract
        entry.getField(FieldName.ABSTRACT).ifPresent(dirtyAbstr -> {
            // Try to sort out most of the /spl / conversions
            // Deal with this specific nested type first
            String abstr = dirtyAbstr.replaceAll("/sub /spl infin//", "\\$_\\\\infty\\$");
            abstr = abstr.replaceAll("/sup /spl infin//", "\\$\\^\\\\infty\\$");
            // Replace general expressions
            abstr = abstr.replaceAll("/[sS]pl ([^/]+)/", "\\$\\\\$1\\$");
            // Deal with subscripts and superscripts
            abstr = SUPER_DETECTION_1.matcher(abstr).replaceAll(SUPER_TEXT_RESULT);
            abstr = SUB_DETECTION_1.matcher(abstr).replaceAll(SUB_TEXT_RESULT);
            abstr = SUPER_DETECTION_2.matcher(abstr).replaceAll(SUPER_TEXT_RESULT);
            abstr = SUB_DETECTION_2.matcher(abstr).replaceAll(SUB_TEXT_RESULT);
            // Replace \infin with \infty
            abstr = abstr.replace("\\infin", "\\infty");
            // Write back
            entry.setField(FieldName.ABSTRACT, abstr);
        });

        // Clean up url
        entry.getField(FieldName.URL)
                .ifPresent(url -> entry.setField(FieldName.URL, "https://ieeexplore.ieee.org" + url.replace("tp=&", "")));

        // Replace ; as keyword separator
        entry.getField(FieldName.KEYWORDS).ifPresent(keys -> entry.setField(FieldName.KEYWORDS,
                keys.replace(";", Globals.prefs.get(JabRefPreferences.KEYWORD_SEPARATOR))));
        return entry;
    }

}
