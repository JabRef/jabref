package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.util.GrobidService;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

/**
 * Wraps the GrobidService function to be used as an Importer.
 */
public class PdfGrobidImporter extends Importer {

    private final GrobidService grobidService;
    private final ImportFormatPreferences importFormatPreferences;

    public PdfGrobidImporter(ImportFormatPreferences importFormatPreferences) {
        this.grobidService = new GrobidService(importFormatPreferences.grobidPreferences());
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "Grobid";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.PDF;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException(
                "PdfGrobidImporter does not support importDatabase(BufferedReader reader)."
                        + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(String data) throws IOException {
        Objects.requireNonNull(data);
        throw new UnsupportedOperationException(
                "PdfGrobidImporter does not support importDatabase(String data)."
                        + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(Path filePath) {
        Objects.requireNonNull(filePath);
        try {
            List<BibEntry> result = grobidService.processPDF(filePath, importFormatPreferences);
            result.forEach(entry -> entry.addFile(new LinkedFile("", filePath.toAbsolutePath(), "PDF")));
            return new ParserResult(result);
        } catch (Exception exception) {
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
    public boolean isRecognizedFormat(Path filePath) throws IOException {
        Objects.requireNonNull(filePath);
        Optional<String> extension = FileUtil.getFileExtension(filePath);
        if (extension.isEmpty()) {
            return false;
        }
        return getFileType().getExtensions().contains(extension.get());
    }

    @Override
    public String getId() {
        return "grobidPdf";
    }

    @Override
    public String getDescription() {
        return "Wraps the GrobidService function to be used as an Importer.";
    }
}
