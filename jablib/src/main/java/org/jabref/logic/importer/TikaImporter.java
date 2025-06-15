package org.jabref.logic.importer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

/**
 * Common class for all file importers that use Apache Tika to extract metadata from files.
 * <p>
 * Child classes should implement the rest of {@link Importer}.
 */
public abstract class TikaImporter extends Importer {
    private final static Pattern imageDatePattern = Pattern.compile("(year|month|day|hour|minute|second)=(\\d+)");

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        throw new UnsupportedOperationException("TikaImporter (and descendants) do not support importDatabase(BufferedReader reader)."
                + "Instead use importDatabase(Path filePath).");
    }

    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath.toFile())) {
            Parser parser = getTikaParser();
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler();

            ParseContext parseContext = new ParseContext();
            parseContext.set(Parser.class, parser);

            parser.parse(inputStream, handler, metadata, parseContext);

            String fileName = FileUtil.getBaseName(filePath);
            BibEntry entry = extractMetadata(metadata, fileName);

            return ParserResult.fromEntry(entry);
        } catch (SAXException | TikaException e) {
            throw new IOException("Error parsing file: " + filePath, e);
        }
    }

    protected Parser getTikaParser() {
        // Use {@link AutoDetectParser} by default, subclasses can override this method to provide a different parser
        return new AutoDetectParser();
    }

    protected EntryType getEntryType() {
        // Default to `Misc`, subclasses should override this method if they have a specific entry type
        return StandardEntryType.Misc;
    }

    private BibEntry extractMetadata(Metadata metadata, String titleReplacement) {
        String title = Optional.ofNullable(metadata.get("dc:title")).orElse(titleReplacement);
        Optional<Date> date = extractDate(metadata);
        List<String> authors = extractAuthors(metadata);

        Optional<String> year = date.map(d -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            return Integer.toString(cal.get(Calendar.YEAR));
        });

        Optional<String> dateIso = date.map(d -> {
            LocalDate localDate = d.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        });

        Optional<String> month = date.map(d -> {
            LocalDate localDate = d.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            String[] bibtexMonths = {
                    "jan", "feb", "mar", "apr", "may", "jun",
                    "jul", "aug", "sep", "oct", "nov", "dec"
            };

            return bibtexMonths[localDate.getMonthValue() - 1];
        });

        return new BibEntry(getEntryType())
                .withField(StandardField.TITLE, title)
                .withField(StandardField.AUTHOR, !authors.isEmpty()
                        ? Optional.of(String.join(" and ", authors))
                        : Optional.empty())
                .withField(StandardField.YEAR, year)
                .withField(StandardField.DATE, dateIso)
                .withField(StandardField.MONTH, month);
    }

    private static List<String> extractAuthors(Metadata metadata) {
        List<String> creators = Arrays.stream(metadata.getValues("dc:creator"))
                .filter(StringUtil::isNotBlank)
                .toList();
        List<String> contributors = Arrays.stream(metadata.getValues("dc:contributor"))
                .filter(StringUtil::isNotBlank)
                .toList();

        return Stream.concat(creators.stream(), contributors.stream()).toList();
    }

    private static Optional<Date> extractDate(Metadata metadata) {
        Optional<Date> date = Optional.ofNullable(metadata.getDate(Property.internalDate("dcterms:created")));

        if (date.isEmpty()) {
            Optional<String> dateEntry = Optional.ofNullable(metadata.get("tIME"))
                    .or(() -> Optional.ofNullable(metadata.get("Document ImageModificationTime")));

            if (dateEntry.isPresent()) {
                date = Optional.of(parseDateForImages(dateEntry.get()));
            }
        }

        return date;
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
}
