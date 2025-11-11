package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilReader;

import org.apache.pdfbox.pdmodel.PDDocument;

/**
 * Wraps the XMPUtility function to be used as an Importer.
 */
public class PdfXmpImporter extends PdfImporter {

    private final XmpPreferences xmpPreferences;
    private final XmpUtilReader xmpUtilReader;

    public PdfXmpImporter(XmpPreferences xmpPreferences) {
        this.xmpPreferences = xmpPreferences;
        xmpUtilReader = new XmpUtilReader();
    }

    @Override
    public ParserResult importDatabase(Path filePath, PDDocument document) throws IOException {
        return new ParserResult(xmpUtilReader.readXmp(filePath, document, xmpPreferences));
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
