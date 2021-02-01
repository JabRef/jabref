package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Objects;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.logic.xmp.XmpUtilShared;

/**
 * Wraps the XMPUtility function to be used as an Importer.
 */
public class PdfXmpImporter extends Importer {

    private final XmpPreferences xmpPreferences;

    public PdfXmpImporter(XmpPreferences xmpPreferences) {
        this.xmpPreferences = xmpPreferences;
    }

    @Override
    public String getName() {
        return Localization.lang("XMP-annotated PDF");
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.PDF;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException(
                "PdfXmpImporter does not support importDatabase(BufferedReader reader)."
                        + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(String data) throws IOException {
        Objects.requireNonNull(data);
        throw new UnsupportedOperationException(
                "PdfXmpImporter does not support importDatabase(String data)."
                        + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(Path filePath, Charset defaultEncoding) {
        Objects.requireNonNull(filePath);
        try {
            return new ParserResult(XmpUtilReader.readXmp(filePath, xmpPreferences));
        } catch (IOException exception) {
            return ParserResult.fromError(exception);
        }
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        return false;
    }

    /**
     * Returns whether the given stream contains data that is a.) a pdf and b.)
     * contains at least one BibEntry.
     */
    @Override
    public boolean isRecognizedFormat(Path filePath, Charset defaultEncoding) throws IOException {
        Objects.requireNonNull(filePath);
        return XmpUtilShared.hasMetadata(filePath, xmpPreferences);
    }

    @Override
    public String getId() {
        return "xmp";
    }

    @Override
    public String getDescription() {
        return "Wraps the XMPUtility function to be used as an Importer.";
    }
}
