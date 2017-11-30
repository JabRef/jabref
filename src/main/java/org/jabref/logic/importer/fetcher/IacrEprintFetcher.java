package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
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
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class IacrEprintFetcher implements IdBasedFetcher {

    private final ImportFormatPreferences prefs;
    private static final DateFormat DATE_FORMAT_WEBSITE = new SimpleDateFormat("dd MMM yyyy");
    private static final DateFormat DATE_FORMAT_BIBTEX = new SimpleDateFormat("yyyy-MM-dd");
    private static final Predicate<String> IDENTIFIER_REGEX = Pattern.compile("\\d{4}/\\d{1,4}").asPredicate();

    public IacrEprintFetcher(ImportFormatPreferences prefs) {
        this.prefs = prefs;
    }

    @Override
    public Optional<BibEntry> performSearchById(final String identifier) throws FetcherException {
        if (!IDENTIFIER_REGEX.test(identifier)) {
            throw new FetcherException("Wrong identifier format");
        }
        String downloaded;
        try {
            URL url = new URL("https://eprint.iacr.org/eprint-bin/cite.pl?entry=" + identifier);
            URLDownload download = new URLDownload(url);
            downloaded = download.asString();
        } catch (IOException e) {
            FetcherException ex = new FetcherException(e.getMessage());
            ex.addSuppressed(e);
            throw ex;
        }

        String entryString = downloaded.substring(downloaded.indexOf("<PRE>") + 5, downloaded.indexOf("</PRE>")).trim();
        try {
            Optional<BibEntry> entry = BibtexParser.singleFromString(entryString, prefs);
            if (entry.isPresent()) {
                setAdditionalFields(entry.get(), identifier);
            }
            return entry;
        } catch (IOException | ParseException e) {
            throw new FetcherException(e.toString());
        }
    }

    @Override
    public String getName() {
        return "IACR eprint";
    }

    private void setAdditionalFields(BibEntry entry, String identifier) throws IOException {
        String url = "https://eprint.iacr.org/" + identifier;
        entry.setField(FieldName.URL, url);

        URLDownload download = new URLDownload(url);
        String content = download.asString();

        String abstractText = getValueBetween("<b>Abstract: </b>", "<p />", content);
        // for some reason, all spaces are doubled...
        abstractText = abstractText.replaceAll("\\s(\\s)", "$1");
        entry.setField(FieldName.ABSTRACT, abstractText);

        String startOfVersionString = "<b>Version: </b><a href=\"/" + identifier + "/";
        String version = getValueBetween(startOfVersionString, "\"", content);
        entry.setField(FieldName.VERSION, version);

        String dates = getValueBetween("<b>Date: </b>", "<p />", content);
        entry.setField(FieldName.DATE, getDate(dates));
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

    private static String getValueBetween(String from, String to, String haystack) {
        int begin = haystack.indexOf(from) + from.length();
        int end = haystack.indexOf(to, begin);
        return haystack.substring(begin, end);
    }
}
