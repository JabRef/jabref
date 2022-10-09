package org.jabref.logic.importer.fileformat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabaseModeDetection;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a full class to read .bib files. It is used for <code>--import</code> and <code>--importToOpen </code>, too.
 */
public class BibtexImporter extends Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexImporter.class);

    // Signature written at the top of the .bib file in earlier versions.
    private static final String SIGNATURE = "This file was created with JabRef";

    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor;

    public BibtexImporter(ImportFormatPreferences importFormatPreferences, FileUpdateMonitor fileMonitor) {
        this.importFormatPreferences = importFormatPreferences;
        this.fileMonitor = fileMonitor;
    }

    /**
     * @return true as we have no effective way to decide whether a file is in bibtex format or not. See
     *         https://github.com/JabRef/jabref/pull/379#issuecomment-158685726 for more details.
     */
    @Override
    public boolean isRecognizedFormat(BufferedReader reader) {
        Objects.requireNonNull(reader);
        return true;
    }

    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {
        // We want to check if there is a JabRef encoding heading in the file, because that would tell us
        // which character encoding is used.

        // In general, we have to use InputStream and not a Reader, because a Reader requires an encoding specification.
        // We do not want to do a byte-by-byte reading or doing wild try/catch magic.
        // We therefore use a charset detection library and then read JabRefs "% Encoding" mark

        Charset detectedCharset;
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            bufferedInputStream.mark(8192);
            detectedCharset = getCharset(bufferedInputStream);
            bufferedInputStream.reset();
            LOGGER.debug("Detected charset: {}", detectedCharset.name());
        }

        Charset encoding;
        boolean encodingExplicitlySupplied;
        try (BufferedReader reader = Files.newBufferedReader(filePath, detectedCharset)) {
            Optional<Charset> suppliedEncoding = getSuppliedEncoding(reader);
            LOGGER.debug("Supplied encoding: {}", suppliedEncoding);
            encodingExplicitlySupplied = suppliedEncoding.isPresent();

            // in case no encoding information is present, use the detected one
            encoding = suppliedEncoding.orElse(detectedCharset);
            LOGGER.debug("Encoding used to read the file: {}", encoding);
        }

        // We replace unreadable characters
        // Unfortunately, no warning will be issued to the user
        // As this is a very seldom case, we accept that
        CharsetDecoder decoder = encoding.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPLACE);

        try (InputStreamReader inputStreamReader = new InputStreamReader(Files.newInputStream(filePath), decoder);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {
            ParserResult parserResult = this.importDatabase(reader);
            parserResult.getMetaData().setEncoding(encoding);
            parserResult.getMetaData().setEncodingExplicitlySupplied(encodingExplicitlySupplied);
            parserResult.setPath(filePath);
            if (parserResult.getMetaData().getMode().isEmpty()) {
                parserResult.getMetaData().setMode(BibDatabaseModeDetection.inferMode(parserResult.getDatabase()));
            }
            return parserResult;
        }
    }

    /**
     * This method does not set the metadata encoding information. The caller needs to set the encoding of the supplied
     * reader manually to the meta data
     */
    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        return new BibtexParser(importFormatPreferences, fileMonitor).parse(reader);
    }

    @Override
    public String getName() {
        return "BibTeX";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.BIBTEX_DB;
    }

    @Override
    public String getDescription() {
        return "This importer enables `--importToOpen someEntry.bib`";
    }

    /**
     * Searches the file for "Encoding: myEncoding" and returns the found supplied encoding.
     */
    private static Optional<Charset> getSuppliedEncoding(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Line does not start with %, so there are no comment lines for us and we can stop parsing
                if (!line.startsWith("%")) {
                    return Optional.empty();
                }

                // Only keep the part after %
                line = line.substring(1).trim();

                if (line.startsWith(BibtexImporter.SIGNATURE)) {
                    // Signature line, so keep reading and skip to next line
                } else if (line.startsWith(SavePreferences.ENCODING_PREFIX)) {
                    // Line starts with "Encoding: ", so the rest of the line should contain the name of the encoding
                    // Except if there is already a @ symbol signaling the starting of a BibEntry
                    Integer atSymbolIndex = line.indexOf('@');
                    String encoding;
                    if (atSymbolIndex > 0) {
                        encoding = line.substring(SavePreferences.ENCODING_PREFIX.length(), atSymbolIndex);
                    } else {
                        encoding = line.substring(SavePreferences.ENCODING_PREFIX.length());
                    }

                    return Optional.of(Charset.forName(encoding));
                } else {
                    // Line not recognized so stop parsing
                    return Optional.empty();
                }
            }
        } catch (IOException ignored) {
            LOGGER.error("Supplied encoding could not be determined", ignored);
        }
        return Optional.empty();
    }
}
