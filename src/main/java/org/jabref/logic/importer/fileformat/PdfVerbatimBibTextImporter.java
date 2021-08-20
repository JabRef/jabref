package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.EncryptedPdfsNotSupportedException;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * This importer imports a verbatim BibTeX entry from the first page of the PDF.
 */
public class PdfVerbatimBibTextImporter extends Importer {

    private final ImportFormatPreferences importFormatPreferences;

    public PdfVerbatimBibTextImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.readLine().startsWith("%PDF");
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException("PdfVerbatimBibTextImporter does not support importDatabase(BufferedReader reader)."
                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(String data) throws IOException {
        Objects.requireNonNull(data);
        throw new UnsupportedOperationException("PdfVerbatimBibTextImporter does not support importDatabase(String data)."
                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(Path filePath, Charset defaultEncoding) {
        List<BibEntry> result = new ArrayList<>(1);
        try (PDDocument document = XmpUtilReader.loadWithAutomaticDecryption(filePath)) {
            String firstPageContents = getFirstPageContents(document);
            BibtexParser parser = new BibtexParser(importFormatPreferences, new DummyFileUpdateMonitor());
            result = parser.parseEntries(firstPageContents);
        } catch (EncryptedPdfsNotSupportedException e) {
            return ParserResult.fromErrorMessage(Localization.lang("Decryption not supported."));
        } catch (IOException | ParseException e) {
            return ParserResult.fromError(e);
        }

        result.forEach(entry -> entry.addFile(new LinkedFile("", filePath.toAbsolutePath(), "PDF")));
        result.forEach(entry -> entry.setCommentsBeforeEntry(""));
        return new ParserResult(result);
    }

    private String getFirstPageContents(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();

        stripper.setStartPage(1);
        stripper.setEndPage(1);
        stripper.setSortByPosition(true);
        stripper.setParagraphEnd(System.lineSeparator());
        StringWriter writer = new StringWriter();
        stripper.writeText(document, writer);

        return writer.toString();
    }

    @Override
    public String getName() {
        return "PdfVerbatimBibText";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.PDF;
    }

    @Override
    public String getDescription() {
        return "PdfVerbatimBibTextImporter imports a verbatim BibTeX entry from the first page of the PDF.";
    }

}
