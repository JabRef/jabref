package org.jabref.logic.importer.fileformat.pdf;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.EncryptedPdfsNotSupportedException;
import org.jabref.logic.xmp.XmpUtilReader;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NonNull;

/// Intermediate class to bundle all PDF analysis steps. [PdfImporter]s are also [org.jabref.logic.importer.Importer]s,
/// which allows user for more fine-grained control of how [org.jabref.model.entry.BibEntry] is extracted from a PDF file.
///
/// [PdfImporter]s are used in two places in JabRef:
/// 1. [PdfMergeMetadataImporter]: uses several [PdfImporter] and automatically
///    merges them into 1 [org.jabref.model.entry.BibEntry].
/// 2. `org.jabref.gui.externalfiles.PdfMergeDialog` also uses several [PdfImporter], but
///    it shows a merge dialog (instead of automatic merging).
///
/// Note, that this step should not add PDF file to [org.jabref.model.entry.BibEntry], it will be finally added either in
/// [#importDatabase(Path)] or [PdfMergeMetadataImporter].
///
/// The result might be the metadata of the given PDF *or* the list of references in the references section (also called "citations"). Each implementation should denote which of these two it supports.
public abstract class PdfImporter extends Importer {
    public abstract ParserResult importDatabase(Path filePath, PDDocument document) throws IOException, ParseException;

    @Override
    public boolean isRecognizedFormat(@NonNull BufferedReader input) throws IOException {
        return input.readLine().startsWith("%PDF");
    }

    @Override
    public ParserResult importDatabase(@NonNull BufferedReader reader) throws IOException {
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
            return importDatabase(filePath, document);
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
