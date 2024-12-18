package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Intermediate class to bundle all PDF analysis steps.
 * <p>
 * Note, that this step should not add PDF file to {@link BibEntry}, it will be finally added
 * in {@link org.jabref.logic.importer.fileformat.PdfImporter}.
 */
public interface PdfBibExtractor {
    List<BibEntry> importDatabase(Path filePath, PDDocument document) throws IOException, ParseException;
}
