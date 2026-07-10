package org.jabref.gui.util;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class PdfMetadataExtractorTest {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @TempDir
    Path tempDir;

    private final PdfMetadataExtractor extractor = new PdfMetadataExtractor();

    @Test
    void getStringFormatsByteCounts() {
        assertEquals("0 B", PdfMetadataExtractor.getString(0));
        assertEquals("1 B", PdfMetadataExtractor.getString(1));
        assertEquals("1023 B", PdfMetadataExtractor.getString(1023));
        assertEquals("1.0 KB", PdfMetadataExtractor.getString(1024));
        assertEquals("1.0 MB", PdfMetadataExtractor.getString(1024L * 1024));
    }

    @Test
    void extractReturnsMetadataForPdfWithDocumentInformation() throws Exception {
        Path pdfFile = tempDir.resolve("metadata.pdf");
        Calendar creationDate = createUtcCalendar(2024, Calendar.JANUARY, 2, 3, 4, 5);
        Calendar modificationDate = createUtcCalendar(2024, Calendar.FEBRUARY, 6, 7, 8, 9);

        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());

            PDDocumentInformation information = new PDDocumentInformation();
            information.setTitle("Sample title");
            information.setAuthor("Sample author");
            information.setCreationDate(creationDate);
            information.setModificationDate(modificationDate);
            information.setKeywords("keyword-one, keyword-two");
            document.setDocumentInformation(information);

            document.save(pdfFile.toFile());
        }

        String expected = String.join(System.lineSeparator(), List.of(
                "File: metadata.pdf",
                "Pages: 1",
                "Size: " + PdfMetadataExtractor.getString(Files.size(pdfFile)),
                "Title: Sample title",
                "Author: Sample author",
                "Creation date: " + formatCalendar(creationDate),
                "Modified date: " + formatCalendar(modificationDate),
                "Keywords: keyword-one, keyword-two"
        ));

        assertEquals(expected, invokeExtract(pdfFile));
    }

    @Test
    void extractReturnsFallbackMessageWhenNoMetadataIsAvailable() throws Exception {
        Path pdfFile = tempDir.resolve("no-metadata.pdf");

        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdfFile.toFile());
        }

        String expected = String.join(System.lineSeparator(), List.of(
                "File: no-metadata.pdf",
                "Pages: 1",
                "Size: " + PdfMetadataExtractor.getString(Files.size(pdfFile)),
                "No extracted metadata available."
        ));

        assertEquals(expected, invokeExtract(pdfFile));
    }

    @Test
    void extractReturnsErrorMessageWhenPdfCannotBeLoaded() throws Exception {
        Path invalidPdf = tempDir.resolve("broken.pdf");
        Files.writeString(invalidPdf, "not a pdf");

        assertEquals("Could not extract Metadata from: broken.pdf", invokeExtract(invalidPdf));
    }

    private String invokeExtract(Path pdfFile) throws ReflectiveOperationException {
        Method extractMethod = PdfMetadataExtractor.class.getDeclaredMethod("extract", Path.class);
        extractMethod.setAccessible(true);
        return (String) extractMethod.invoke(extractor, pdfFile);
    }

    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(year, month, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static String formatCalendar(Calendar calendar) {
        return DATE_FORMATTER.format(calendar.toInstant().atZone(ZoneId.systemDefault()));
    }
}
