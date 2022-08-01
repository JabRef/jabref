package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.DummyFileUpdateMonitor;

public class IacrEprintFetcher implements IdBasedFetcher {

    public static final String NAME = "IACR eprints";

    private static final Pattern WITHOUT_LETTERS_SPACE = Pattern.compile("[^0-9/]");

    private static final Predicate<String> IDENTIFIER_PREDICATE = Pattern.compile("\\d{4}/\\d{3,5}").asPredicate();
    private static final String CITATION_URL_PREFIX = "https://eprint.iacr.org/";
    private static final String DESCRIPTION_URL_PREFIX = "https://eprint.iacr.org/";
    private static final String VERSION_URL_PREFIX = "https://eprint.iacr.org/archive/versions/";

    private final ImportFormatPreferences prefs;

    public IacrEprintFetcher(ImportFormatPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        String identifierWithoutLettersAndSpaces = WITHOUT_LETTERS_SPACE.matcher(identifier).replaceAll(" ").trim();

        if (!IDENTIFIER_PREDICATE.test(identifierWithoutLettersAndSpaces)) {
            throw new FetcherException(Localization.lang("Invalid identifier: '%0'.", identifier));
        }

        Optional<BibEntry> entry = createEntryFromIacrCitation(identifierWithoutLettersAndSpaces);

        if (entry.isPresent()) {
            setAdditionalFields(entry.get(), identifierWithoutLettersAndSpaces);
        }

        return entry;
    }

    private Optional<BibEntry> createEntryFromIacrCitation(String validIdentifier) throws FetcherException {
        String bibtexCitationHtml = getHtml(CITATION_URL_PREFIX + validIdentifier);
        if (bibtexCitationHtml.contains("No such report found")) {
            throw new FetcherException(Localization.lang("No results found."));
        }
        String actualEntry = getRequiredValueBetween("<pre id=\"bibtex\">", "</pre>", bibtexCitationHtml);

        try {
            return BibtexParser.singleFromString(actualEntry, prefs, new DummyFileUpdateMonitor());
        } catch (ParseException e) {
            throw new FetcherException(Localization.lang("Entry from %0 could not be parsed.", "IACR"), e);
        }
    }

    private void setAdditionalFields(BibEntry entry, String identifier) throws FetcherException {
        String entryUrl = DESCRIPTION_URL_PREFIX + identifier;
        String descriptiveHtml = getHtml(entryUrl);

        entry.setField(StandardField.ABSTRACT, getAbstract(descriptiveHtml));
        entry.setField(StandardField.DATE, getDate(descriptiveHtml));

        // Version information for entries after year 2000
        if (isFromOrAfterYear2000(entry)) {
            String entryVersion = VERSION_URL_PREFIX + identifier;
            String versionHtml = getHtml(entryVersion);
            String version = getVersion(identifier, versionHtml);
            entry.setField(StandardField.VERSION, version);
            entry.setField(StandardField.URL, entryUrl + "/" + version);
        }
    }

    private String getVersion(String identifier, String versionHtml) throws FetcherException {
        String startOfVersionString = "<li><a href=\"/archive/" + identifier + "/";
        String version = getRequiredValueBetween(startOfVersionString, "\">", versionHtml);
        return version;
    }

    private String getAbstract(String descriptiveHtml) throws FetcherException {
        String startOfAbstractString = "<h5 class=\"mt-3\">Abstract</h5>\n    <p style=\"white-space: pre-wrap;\">";
        String abstractText = getRequiredValueBetween(startOfAbstractString, "</p>", descriptiveHtml);
        return abstractText;
    }

    private String getDate(String descriptiveHtml) throws FetcherException {
        String startOfHistoryString = "<dt>History</dt>\n      \n      \n      <dd>";
        String dateStringAsInHtml = getRequiredValueBetween(startOfHistoryString, ":", descriptiveHtml);
        return dateStringAsInHtml;
    }

    private String getHtml(String url) throws FetcherException {
        try {
            URLDownload download = new URLDownload(url);
            return download.asString();
        } catch (IOException e) {
            throw new FetcherException(Localization.lang("Could not retrieve entry data from '%0'.", url), e);
        }
    }

    private String getRequiredValueBetween(String from, String to, String haystack) throws FetcherException {
        String value = StringUtil.substringBetween(haystack, from, to);
        if (value == null) {
            throw new FetcherException(Localization.lang("Entry from %0 could not be parsed.", "IACR"));
        } else {
            return value;
        }
    }

    private boolean isFromOrAfterYear2000(BibEntry entry) throws FetcherException {
        Optional<String> yearField = entry.getField(StandardField.YEAR);
        if (yearField.isPresent()) {
            return Integer.parseInt(yearField.get()) > 2000;
        }
        throw new FetcherException(Localization.lang("Entry from %0 could not be parsed.", "IACR"));
    }

    @Override
    public String getName() {
        return NAME;
    }
}
