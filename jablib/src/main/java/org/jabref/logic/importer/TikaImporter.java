package org.jabref.logic.importer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 * Common class for all file importers that use Apache Tika to extract metadata from files.
 * <p>
 * Child classes should implement the rest of {@link Importer}.
 */
public abstract class TikaImporter extends Importer {
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

            BibEntry entry = extractMetadata(metadata);

            return ParserResult.fromEntry(entry);
        } catch (SAXException | TikaException e) {
            throw new IOException("Error parsing file: " + filePath, e);
        }
    }

    protected Parser getTikaParser() {
        return new AutoDetectParser();
    }

    private BibEntry extractMetadata(Metadata metadata) {
        System.out.println(metadata);
        Optional<String> title = Optional.ofNullable(metadata.get("dc:title"));
        Optional<Date> date = Optional.ofNullable(metadata.getDate(Property.internalDate("dcterms:created")));

        Optional<String> dateString = date.map(d -> {
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            return Integer.toString(cal.get(Calendar.YEAR));
        });

        List<String> creators = Arrays.stream(metadata.getValues("dc:creator"))
                .filter(StringUtil::isNotBlank)
                .toList();
        List<String> contributors = Arrays.stream(metadata.getValues("dc:contributor"))
                .filter(StringUtil::isNotBlank)
                .toList();

        List<String> authors = Stream.concat(creators.stream(), contributors.stream()).toList();

        return new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, title)
                .withField(StandardField.AUTHOR, !authors.isEmpty()
                        ? Optional.of(String.join(" and ", authors))
                        : Optional.empty())
                .withField(StandardField.YEAR, dateString);
    }
}
