package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IacrEprintFetcher implements IdBasedFetcher {

    public static final String NAME = "IACR eprints";

    private static final Logger LOGGER = LoggerFactory.getLogger(IacrEprintFetcher.class);
    private static final Pattern DATE_FROM_WEBSITE_AFTER_2000_PATTERN = Pattern.compile("[a-z ]+(\\d{1,2} [A-Za-z][a-z]{2} \\d{4})");
    private static final DateTimeFormatter DATE_FORMAT_WEBSITE_AFTER_2000 = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US);
    private static final Pattern DATE_FROM_WEBSITE_BEFORE_2000_PATTERN = Pattern.compile("[A-Za-z ]+? ([A-Za-z][a-z]{2,10} \\d{1,2}(th|st|nd|rd)?, \\d{4})\\.?");
    private static final DateTimeFormatter DATE_FORMAT_WEBSITE_BEFORE_2000_LONG_MONTHS = DateTimeFormatter.ofPattern("MMMM d['th']['st']['nd']['rd'] yyyy", Locale.US);
    private static final DateTimeFormatter DATE_FORMAT_WEBSITE_BEFORE_2000_SHORT_MONTHS = DateTimeFormatter.ofPattern("MMM d['th']['st']['nd']['rd'] yyyy", Locale.US);
    private static final DateTimeFormatter DATE_FORMAT_BIBTEX = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Predicate<String> IDENTIFIER_PREDICATE = Pattern.compile("\\d{4}/\\d{3,5}").asPredicate();
    private static final String CITATION_URL_PREFIX = "https://eprint.iacr.org/eprint-bin/cite.pl?entry=";
    private static final String DESCRIPTION_URL_PREFIX = "https://eprint.iacr.org/";
    private static final Charset WEBSITE_CHARSET = StandardCharsets.ISO_8859_1;

    private final ImportFormatPreferences prefs;

    public IacrEprintFetcher(ImportFormatPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        String identifierWithoutLettersAndSpaces = identifier.replaceAll("[^0-9/]", " ").trim();

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
        String actualEntry = getRequiredValueBetween("<PRE>", "</PRE>", bibtexCitationHtml);

        try {
            return BibtexParser.singleFromString(actualEntry, prefs, new DummyFileUpdateMonitor());
        } catch (ParseException e) {
            throw new FetcherException(Localization.lang("Entry from %0 could not be parsed.", "IACR"), e);
        }
    }

    private void setAdditionalFields(BibEntry entry, String identifier) throws FetcherException {
        String entryUrl = DESCRIPTION_URL_PREFIX + identifier;
        String descriptiveHtml = getHtml(entryUrl);
        entry.setField(FieldName.ABSTRACT, getAbstract(descriptiveHtml));
        String dateStringAsInHtml = getRequiredValueBetween("<b>Date: </b>", "<p />", descriptiveHtml);
        entry.setField(FieldName.DATE, getLatestDate(dateStringAsInHtml));

        if (isFromOrAfterYear2000(entry)) {
            String version = getVersion(identifier, descriptiveHtml);
            entry.setField(FieldName.VERSION, version);
            entry.setField(FieldName.URL, entryUrl + "/" + version);
        } else {
            // No version information for entries before year 2000
            entry.setField(FieldName.URL, entryUrl);
        }
    }

    private String getVersion(String identifier, String descriptiveHtml) throws FetcherException {
        String startOfVersionString = "<b>Version: </b><a href=\"/" + identifier + "/";
        String version = getRequiredValueBetween(startOfVersionString, "\"", descriptiveHtml);
        return version;
    }

    private String getAbstract(String descriptiveHtml) throws FetcherException {
        String abstractText = getRequiredValueBetween("<b>Abstract: </b>", "<p />", descriptiveHtml);
        // for some reason, all spaces are doubled...
        abstractText = abstractText.replaceAll("\\s(\\s)", "$1");
        return abstractText;
    }

    private String getLatestDate(String dateStringAsInHtml) throws FetcherException {
        if (dateStringAsInHtml.contains("withdrawn")) {
            throw new FetcherException(Localization.lang("This paper has been withdrawn."));
        }
        String[] rawDates = dateStringAsInHtml.split(", \\D");
        List<String> formattedDates = new ArrayList<>();
        for (String rawDate : rawDates) {
            TemporalAccessor date = parseSingleDateFromWebsite(rawDate);
            if (date != null) {
                formattedDates.add(DATE_FORMAT_BIBTEX.format(date));
            }
        }

        if (formattedDates.isEmpty()) {
            throw new FetcherException(Localization.lang("Entry from %0 could not be parsed.", "IACR"));
        }

        Collections.sort(formattedDates, Collections.reverseOrder());
        return formattedDates.get(0);
    }

    private TemporalAccessor parseSingleDateFromWebsite(String dateStringFromWebsite) {
        TemporalAccessor date = null;
        // Some entries contain double spaces in the date string (which would break our regexs below)
        String dateStringWithoutDoubleSpaces = dateStringFromWebsite.replaceAll("\\s\\s+", " ");

        Matcher dateMatcherAfter2000 = DATE_FROM_WEBSITE_AFTER_2000_PATTERN.matcher(dateStringWithoutDoubleSpaces.trim());
        if (dateMatcherAfter2000.find()) {
            try {
                date = DATE_FORMAT_WEBSITE_AFTER_2000.parse(dateMatcherAfter2000.group(1));
            } catch (DateTimeParseException e) {
                LOGGER.warn("Date from IACR could not be parsed", e);
            }
        }

        // Entries before year 2000 use a variety of date formats - fortunately, we can match them with only two different
        // date formats (each of which differ from the unified format of post-2000 entries).
        Matcher dateMatcherBefore2000 = DATE_FROM_WEBSITE_BEFORE_2000_PATTERN.matcher(dateStringWithoutDoubleSpaces.trim());
        if (dateMatcherBefore2000.find()) {
            String dateWithoutComma = dateMatcherBefore2000.group(1).replace(",", "");
            try {
                date = DATE_FORMAT_WEBSITE_BEFORE_2000_LONG_MONTHS.parse(dateWithoutComma);
            } catch (DateTimeParseException e) {
                try {
                    date = DATE_FORMAT_WEBSITE_BEFORE_2000_SHORT_MONTHS.parse(dateWithoutComma);
                } catch (DateTimeException e1) {
                    LOGGER.warn("Date from IACR could not be parsed", e);
                    LOGGER.warn("Date from IACR could not be parsed", e1);
                }
            }
        }

        return date;
    }

    private String getHtml(String url) throws FetcherException {
        try {
            URLDownload download = new URLDownload(url);
            return download.asString(WEBSITE_CHARSET);
        } catch (IOException e) {
            throw new FetcherException(Localization.lang("Could not retrieve entry data from '%0'.", url), e);
        }
    }

    private String getRequiredValueBetween(String from, String to, String haystack) throws FetcherException {
        String value = StringUtils.substringBetween(haystack, from, to);
        if (value == null) {
            throw new FetcherException(Localization.lang("Entry from %0 could not be parsed.", "IACR"));
        } else {
            return value;
        }
    }

    private boolean isFromOrAfterYear2000(BibEntry entry) throws FetcherException {
        Optional<String> yearField = entry.getField(FieldName.YEAR);
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
