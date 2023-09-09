package org.jabref.logic.exporter;

        import java.io.IOException;
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

public class XmpPdfExporter extends Exporter {

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

        if (!pdfFile.toString().endsWith(".pdf")) {
            throw new IllegalArgumentException("Invalid PDF file extension");
        }

        try (PDDocument document = new PDDocument()) {

            document.save("C:/Users/wicht/Desktop/metadata.pdf");
            document.close();

            // Embed metadata using XmpUtilWriter
            new XmpUtilWriter(xmpPreferences).writeXmp(pdfFile, entries, databaseContext.getDatabase());

            // Save the PDF to the specified file
            document.save(pdfFile.toFile());
        } catch (IOException e) {
            throw new Exception("Error creating PDF", e);
        }
    }
}
