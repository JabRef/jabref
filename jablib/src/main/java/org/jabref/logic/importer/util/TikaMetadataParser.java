package org.jabref.logic.importer.util;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;

public class TikaMetadataParser {
    private final static Pattern imageDatePattern = Pattern.compile("(year|month|day|hour|minute|second)=(\\d+)");

    private final Metadata metadata;

    public TikaMetadataParser(Metadata metadata) {
        this.metadata = metadata;
    }

    public Optional<String> getDcTitle() {
        return Optional.ofNullable(metadata.get("dc:title"));
    }

    public Optional<Date> getDcTermsCreated() {
        return Optional.ofNullable(
                metadata.getDate(
                        Property.internalDate("dcterms:created")
                )
        );
    }

    public Optional<Date> getPngCreationTime() {
        return Optional.ofNullable(metadata.get("tIME"))
                .or(() -> Optional.ofNullable(metadata.get("Document ImageModificationTime")))
                .map(TikaMetadataParser::parseDateForImages);
    }

    private static Date parseDateForImages(String dateString) {
        Matcher matcher = imageDatePattern.matcher(dateString);

        int year = 0, month = 1, day = 1, hour = 0, minute = 0, second = 0;

        while (matcher.find()) {
            String key = matcher.group(1);
            int value = Integer.parseInt(matcher.group(2));

            switch (key) {
                case "year" -> year = value;
                case "month" -> month = value;
                case "day" -> day = value;
                case "hour" -> hour = value;
                case "minute" -> minute = value;
                case "second" -> second = value;
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, second);

        return calendar.getTime();
    }

    public Optional<String> getDcIdentifier() {
        return Optional.ofNullable(metadata.get("dc:identifier"));
    }

    public List<String> getDcCreators() {
        return Arrays.stream(metadata.getValues("dc:creator"))
                .filter(StringUtil::isNotBlank)
                .toList();
    }

    public List<String> getDcContributors() {
        return Arrays.stream(metadata.getValues("dc:creator"))
                .filter(StringUtil::isNotBlank)
                .toList();
    }

    public static String extractYearFromDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return Integer.toString(cal.get(Calendar.YEAR));
    }

    public static String extractBibtexMonthFromDate(Date date) {
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        String[] bibtexMonths = {
                "jan", "feb", "mar", "apr", "may", "jun",
                "jul", "aug", "sep", "oct", "nov", "dec"
        };

        return bibtexMonths[localDate.getMonthValue() - 1];
    }

    public static String formatBibtexDate(Date date) {
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        return localDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
    }

    public static Optional<String> formatBibtexAuthors(List<String> authors) {
        if (authors.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(String.join(" and ", authors));
    }

    /**
     * Adds year, month, and date fields to a BibEntry based on the provided date. Mutates the given object.
     */
    public static BibEntry addDateCreated(BibEntry entry, Date date) {
        return entry
                .withField(StandardField.YEAR, extractYearFromDate(date))
                .withField(StandardField.MONTH, extractBibtexMonthFromDate(date))
                .withField(StandardField.DATE, formatBibtexDate(date));
    }
}
