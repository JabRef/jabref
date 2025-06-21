package org.jabref.logic.importer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.jabref.logic.importer.util.TikaMetadataParser;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

/**
 * Common class for all file importers that use Apache Tika to extract metadata from files.
 * <p>
 * Child classes should implement the rest of {@link Importer} and method {@link #extractMetadata(TikaMetadataParser, String)} to extract the {@link BibEntry} from the Tika metadata.
 * <p>
 * In case you need to use a specific Tika parser, you can override {@link #getTikaParser()} to return a different parser instance.
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

            String fileName = FileUtil.getBaseName(filePath);
            BibEntry entry = extractMetadata(new TikaMetadataParser(metadata));

            if (!entry.hasField(StandardField.TITLE)) {
                entry.setField(StandardField.TITLE, fileName);
            }

            return ParserResult.fromEntry(entry);
        } catch (SAXException | TikaException e) {
            throw new IOException("Error parsing file: " + filePath, e);
        }
    }

    protected Parser getTikaParser() {
        return new AutoDetectParser();
    }

    /**
     * Extracts common metadata from the given Tika metadata object and returns a {@link BibEntry}.
     * <p>
     * This function will add fields that are most standard and common across different file types. Inheritors are
     * recommended to override {@link TikaImporter#extractAdditionalMetadata(BibEntry, TikaMetadataParser)}
     * process additional metadata that is specific to the file type they are importing.
     */
    protected final BibEntry extractMetadata(Metadata metadata) {
        TikaMetadataParser metadataParser = new TikaMetadataParser(metadata);

        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.TITLE, metadataParser.getDcTitle())
                .withField(StandardField.AUTHOR, TikaMetadataParser.formatBibtexAuthors(metadataParser.getDcCreators()));

        metadataParser.getDcTermsCreated().ifPresent(date -> TikaMetadataParser.addDateCreated(entry, date));

        extractAdditionalMetadata(entry, metadataParser);

        return entry;
    }

    /**
     * Extracts additional metadata that is specific to the file type being imported. Inheritors are allowed to mutate
     * the given {@link BibEntry} to add more fields or modify existing ones.
     */
    protected void extractAdditionalMetadata(BibEntry entry, TikaMetadataParser metadataParser) {

    }
}
