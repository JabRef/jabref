package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmpPdfExporter extends Exporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(XmpPdfExporter.class);

    private final XmpPreferences xmpPreferences;

    public XmpPdfExporter(XmpPreferences xmpPreferences) {
        super("pdf", Localization.lang("XMP-annotated PDF"), StandardFileType.PDF);
        this.xmpPreferences = xmpPreferences;
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path pdfFile, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(pdfFile);
        Objects.requireNonNull(entries);

        Path filePath = pdfFile.toAbsolutePath();

        if (!Files.exists(filePath)) {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage();
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(25, 500);
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    contentStream.showText("This PDF was created by JabRef. It demonstrates the embedding of XMP data in PDF files. Please open the file metadata view of your PDF viewer to see the attached files. Note that the normal usage is to embed the BibTeX data in an existing PDF.");
                    contentStream.endText();
                }
                document.save(filePath.toString());
            } catch (IOException e) {
                LOGGER.error("Could not create PDF file", e);
            }
            new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile, entries, databaseContext.getDatabase());
        }
    }
}
