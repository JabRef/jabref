package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class IacrEprintFetcher implements IdBasedFetcher {

    private final ImportFormatPreferences prefs;
    private static final DateFormat DATE_FORMAT_WEBSITE = new SimpleDateFormat("dd MMM yyyy");
    private static final DateFormat DATE_FORMAT_BIBTEX = new SimpleDateFormat("yyyy-MM-dd");
    private static final Predicate<String> IDENTIFIER_PREDICATE = Pattern.compile("\\d{4}/\\d{3,5}").asPredicate();
    private static final String CITATION_URL_PREFIX = "https://eprint.iacr.org/eprint-bin/cite.pl?entry=";

    public IacrEprintFetcher(ImportFormatPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        String identifierWithoutLettersAndSpaces = identifier.replaceAll("[^0-9/]", " ").trim();

        if (!IDENTIFIER_PREDICATE.test(identifierWithoutLettersAndSpaces)) {
            throw new FetcherException(Localization.lang("Invalid IACR identifier: '%0'.", identifier));
        }

        Optional<BibEntry> entry = createEntryFromIacrCitation(identifierWithoutLettersAndSpaces);

        if (entry.isPresent()) {
            setAdditionalFields(entry.get(), identifierWithoutLettersAndSpaces);
        }

        return entry;
    }

    private Optional<BibEntry> createEntryFromIacrCitation(String validIdentifier) throws FetcherException {
        String bibtexCitationHtml = getHtml(CITATION_URL_PREFIX + validIdentifier);
        String actualEntry = getValueBetween("<PRE>", "</PRE>", bibtexCitationHtml);

        Optional<BibEntry> entry;
        try {
            entry = BibtexParser.singleFromString(actualEntry, prefs);
        } catch (ParseException e) {
            throw new FetcherException(Localization.lang("Entry from IACR could not be parsed."), e);
        }
        return entry;
    }

    @Override
    public String getName() {
        return "IACR eprint";
    }

    private void setAdditionalFields(BibEntry entry, String identifier) throws FetcherException {
        String url = "https://eprint.iacr.org/" + identifier;
        entry.setField(FieldName.URL, url);

        String content = getHtml(url);

        String abstractText = getValueBetween("<b>Abstract: </b>", "<p />", content);
        // for some reason, all spaces are doubled...
        abstractText = abstractText.replaceAll("\\s(\\s)", "$1");
        entry.setField(FieldName.ABSTRACT, abstractText);

        String startOfVersionString = "<b>Version: </b><a href=\"/" + identifier + "/";
        String version = getValueBetween(startOfVersionString, "\"", content);
        entry.setField(FieldName.VERSION, version);

        String dateStringAsInHtml = getValueBetween("<b>Date: </b>", "<p />", content);
        entry.setField(FieldName.DATE, getDate(dateStringAsInHtml));
    }

    public static String getDate(String dateContent) {
        String[] rawDates = dateContent.split(",");
        List<String> formattedDates = new ArrayList<>();

        for (String rawDate : rawDates) {
            try {
                rawDate = rawDate.trim();
                Matcher dateMatcher = Pattern.compile("[a-z ]+(\\d{1,2} [A-Za-z][a-z]{2} \\d{4})").matcher(rawDate);
                if (dateMatcher.find()) {
                    Date date = DATE_FORMAT_WEBSITE.parse(dateMatcher.group(1));
                    formattedDates.add(DATE_FORMAT_BIBTEX.format(date));
                }
            } catch (Exception e) {
                // Just skip
            }
        }

        Collections.sort(formattedDates, Collections.reverseOrder());
        return formattedDates.get(0);
    }

    private String getHtml(String url) throws FetcherException {
        try {
            URLDownload download = new URLDownload(url);
            return download.asString(prefs.getEncoding());
        } catch (IOException e) {
            throw new FetcherException(Localization.lang("Could not retrieve entry data from IACR at '%0'.", url), e);
        }
    }

    private static String getValueBetween(String from, String to, String haystack) throws FetcherException {
        try {
            int begin = haystack.indexOf(from) + from.length();
            int end = haystack.indexOf(to, begin);
            return haystack.substring(begin, end);
        } catch (IndexOutOfBoundsException e) {
            throw new FetcherException(Localization.lang("Could not extract required data from IACR HTML."));
        }
    }
}
