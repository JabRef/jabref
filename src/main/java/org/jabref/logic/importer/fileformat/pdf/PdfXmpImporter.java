package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilReader;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Wraps the XMPUtility function to be used as an Importer.
 */
public class PdfXmpImporter extends PdfImporter {

    private final XmpPreferences xmpPreferences;

    public PdfXmpImporter(XmpPreferences xmpPreferences) {
        this.xmpPreferences = xmpPreferences;
    }

    public List<BibEntry> importDatabase(Path filePath, PDDocument document) throws IOException {
        return new XmpUtilReader().readXmp(filePath, xmpPreferences);
    }

    @Override
    public String getId() {
        return "pdfXmp";
    }

    @Override
    public String getName() {
        return Localization.lang("XMP-annotated PDF");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Imports BibTeX data using XMP data of a PDF.");
    }
}
