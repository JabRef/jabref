package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IacrEprintFetcher implements IdBasedFetcher {

    public static final String NAME = "IACR eprints";

    private static final Log LOGGER = LogFactory.getLog(IacrEprintFetcher.class);
    private static final Pattern DATE_FROM_WEBSITE_PATTERN = Pattern.compile("[a-z ]+(\\d{1,2} [A-Za-z][a-z]{2} \\d{4})");
    private static final DateTimeFormatter DATE_FORMAT_WEBSITE = DateTimeFormatter.ofPattern("d MMM yyyy");
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

        try {
            return BibtexParser.singleFromString(actualEntry, prefs);
        } catch (ParseException e) {
            throw new FetcherException(Localization.lang("Entry from IACR could not be parsed."), e);
        }
    }

    private void setAdditionalFields(BibEntry entry, String identifier) throws FetcherException {
        String descriptiveHtml = getHtml(DESCRIPTION_URL_PREFIX + identifier);
        String version = getVersion(identifier, descriptiveHtml);

        entry.setField(FieldName.VERSION, version);
        entry.setField(FieldName.URL, DESCRIPTION_URL_PREFIX + identifier + "/" + version);
        entry.setField(FieldName.ABSTRACT, getAbstract(descriptiveHtml));

        String dateStringAsInHtml = getValueBetween("<b>Date: </b>", "<p />", descriptiveHtml);
        entry.setField(FieldName.DATE, getLatestDate(dateStringAsInHtml));
    }

    private String getVersion(String identifier, String descriptiveHtml) throws FetcherException {
        String startOfVersionString = "<b>Version: </b><a href=\"/" + identifier + "/";
        String version = getValueBetween(startOfVersionString, "\"", descriptiveHtml);
        return version;
    }

    private String getAbstract(String descriptiveHtml) throws FetcherException {
        String abstractText = getValueBetween("<b>Abstract: </b>", "<p />", descriptiveHtml);
        // for some reason, all spaces are doubled...
        abstractText = abstractText.replaceAll("\\s(\\s)", "$1");
        return abstractText;
    }

    private String getLatestDate(String dateStringAsInHtml) throws FetcherException {
        String[] rawDates = dateStringAsInHtml.split(",");
        List<String> formattedDates = new ArrayList<>();
        for (String rawDate : rawDates) {
            TemporalAccessor date = parseDateFromWebsite(rawDate);
            if (date != null) {
                formattedDates.add(DATE_FORMAT_BIBTEX.format(date));
            }
        }

        if (formattedDates.isEmpty()) {
            throw new FetcherException(Localization.lang("Entry from IACR could not be parsed."));
        }

        Collections.sort(formattedDates, Collections.reverseOrder());
        return formattedDates.get(0);
    }

    private TemporalAccessor parseDateFromWebsite(String dateStringFromWebsite) {
        TemporalAccessor date = null;
        Matcher dateMatcher = DATE_FROM_WEBSITE_PATTERN.matcher(dateStringFromWebsite.trim());
        if (dateMatcher.find()) {
            try {
                date = DATE_FORMAT_WEBSITE.parse(dateMatcher.group(1));
            } catch (DateTimeParseException e) {
                LOGGER.warn("Date from IACR could not be parsed", e);
            }
        }
        return date;
    }

    private String getHtml(String url) throws FetcherException {
        try {
            URLDownload download = new URLDownload(url);
            return download.asString(WEBSITE_CHARSET);
        } catch (IOException e) {
            throw new FetcherException(Localization.lang("Could not retrieve entry data from IACR at '%0'.", url), e);
        }
    }

    private String getValueBetween(String from, String to, String haystack) throws FetcherException {
        String value = StringUtils.substringBetween(haystack, from, to);
        if (value == null) {
            throw new FetcherException(Localization.lang("Could not extract required data from IACR HTML."));
        } else {
            return value;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
