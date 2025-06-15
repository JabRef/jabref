package org.jabref.logic.importer.fileformat.docs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Property;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 * General importer for Open Document Format files.
 */
public abstract class OdfImporter extends Importer {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.isZip(input);
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        throw new UnsupportedOperationException("OdfImporter (and descendants) does not support importDatabase(BufferedReader reader)."
                + "Instead use importDatabase(Path filePath).");
    }

    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath.toFile())) {
            AutoDetectParser parser = new AutoDetectParser();
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler();

            parser.parse(inputStream, handler, metadata);

            BibEntry entry = extractMetadata(metadata);

            return ParserResult.fromEntry(entry);
        } catch (SAXException | TikaException e) {
            throw new IOException("Error parsing file: " + filePath, e);
        }
    }

    private BibEntry extractMetadata(Metadata metadata) {
        Optional<String> title = Optional.ofNullable(metadata.get("dc:title"));
        Optional<Date> date = Optional.ofNullable(metadata.getDate(Property.internalDate("dcterms:created")));

        List<String> authors = Arrays.asList(metadata.getValues("dc:contributor"));

        return new BibEntry()
                .withField(StandardField.TITLE, title)
                .withField(StandardField.AUTHOR, !authors.isEmpty() ? Optional.of(String.join(" and ", authors)) : Optional.empty())
                .withField(StandardField.YEAR, date.map(Date::getYear).map(Object::toString));
    }
}
