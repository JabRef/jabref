package org.jabref.logic.importer.fileformat.pdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.EncryptedPdfsNotSupportedException;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Intermediate class to bundle all PDF analysis steps. {@link PdfImporter}s are also {@link org.jabref.logic.importer.Importer}s,
 * which allows user for more fine-grained control of how {@link BibEntry} is extracted from a PDF file.
 * <p>
 * {@link PdfImporter}s are used in two places in JabRef:
 * 1. {@link PdfMergeMetadataImporter}: uses several {@link PdfImporter} and automatically
 *    merges them into 1 {@link BibEntry}.
 * 2. {@link org.jabref.gui.externalfiles.PdfMergeDialog}: also uses several {@link PdfImporter}, but
 *    it shows a merge dialog (instead of automatic merging).
 * <p>
 * Note, that this step should not add PDF file to {@link BibEntry}, it will be finally added either in
 * {@link PdfImporter#importDatabase(Path)} or {@link PdfMergeMetadataImporter}.
 */
public abstract class PdfImporter extends Importer {
    public abstract List<BibEntry> importDatabase(Path filePath, PDDocument document) throws IOException, ParseException;

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.readLine().startsWith("%PDF");
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        throw new UnsupportedOperationException("PdfImporter does not support importDatabase(BufferedReader reader). "
                + "Instead use importDatabase(Path filePath).");
    }

    @Override
    public ParserResult importDatabase(String data) throws IOException {
        throw new UnsupportedOperationException("PdfImporter does not support importDatabase(String data). "
                + "Instead use importDatabase(Path filePath).");
    }

    @Override
    public ParserResult importDatabase(Path filePath) {
        try (PDDocument document = new XmpUtilReader().loadWithAutomaticDecryption(filePath)) {
            return new ParserResult(importDatabase(filePath, document));
        } catch (EncryptedPdfsNotSupportedException e) {
            return ParserResult.fromErrorMessage(Localization.lang("Decryption not supported."));
        } catch (IOException | ParseException exception) {
            return ParserResult.fromError(exception);
        }
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.PDF;
    }
}
