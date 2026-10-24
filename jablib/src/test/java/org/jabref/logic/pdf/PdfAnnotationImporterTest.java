package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jabref.model.pdf.AnnotationArea;
import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PdfAnnotationImporterTest {

    private static final PDRectangle MEDIA_BOX = new PDRectangle(0, 0, 595, 842);

    /// Lower left (20, 30), upper right (575, 812): a page whose rendered area is inset on all sides.
    private static final PDRectangle CROP_BOX = new PDRectangle(20, 30, 555, 782);

    private static final PDRectangle ANNOTATION_RECTANGLE = new PDRectangle(100, 700, 20, 20);

    private final AnnotationImporter importer = new PdfAnnotationImporter();

    @Test
    void invalidPath() {
        assertEquals(List.of(), importer.importAnnotations(Path.of("/asdf/does/not/exist.pdf")));
    }

    @Test
    void invalidDirectory() {
        assertEquals(List.of(), importer.importAnnotations(Path.of("src/test/resources/pdfs")));
    }

    @Test
    void invalidDocumentType() {
        assertEquals(List.of(), importer.importAnnotations(Path.of("src/test/resources/pdfs/write-protected.docx")));
    }

    @Test
    void noAnnotationsWriteProtected() {
        assertEquals(List.of(), importer.importAnnotations(Path.of("src/test/resources/pdfs/write-protected.pdf")));
    }

    @Test
    void noAnnotationsEncrypted() {
        assertEquals(List.of(), importer.importAnnotations(Path.of("src/test/resources/pdfs/encrypted.pdf")));
    }

    @Test
    void twoAnnotationsThesisExample() {
        assertEquals(2, importer.importAnnotations(Path.of("src/test/resources/pdfs/thesis-example.pdf")).size());
    }

    @Test
    void noAnnotationsMinimal() {
        assertEquals(List.of(), importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal.pdf")));
    }

    @Test
    void inlineNoteMinimal() {
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 25), 1,
                "inline note annotation", FileAnnotationType.FREETEXT, Optional.empty());

        assertEquals(List.of(expected),
                importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-inlinenote.pdf")));
    }

    @Test
    void popupNoteMinimal() {
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 17, 24), 1,
                "A simple pop-up note", FileAnnotationType.TEXT, Optional.empty());

        assertEquals(List.of(expected),
                importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-popup.pdf")));
    }

    /// A sticky note carries no page text, so only its rectangle can position a viewer.
    /// The fixture declares no crop box, leaving `/MediaBox [0 0 595.276 841.89]` as the rendered
    /// box, and `/Rect [383.8842587459 702.2328263541 401.7425387459 719.6492580792]` counted from
    /// the bottom left, so y becomes 841.89 - 719.649.
    @Test
    void popupNoteAreaUsesTopLeftOrigin() {
        final AnnotationArea area = importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-popup.pdf"))
                                            .getFirst()
                                            .getArea()
                                            .orElseThrow();

        assertEquals(383.884f, area.x(), 0.001f);
        assertEquals(122.241f, area.y(), 0.001f);
        assertEquals(17.858f, area.width(), 0.001f);
        assertEquals(17.416f, area.height(), 0.001f);
    }

    @Test
    void areaIsRelativeToCropBoxInsteadOfMediaBox(@TempDir Path tempDir) throws IOException {
        Path file = writeSinglePageWithStickyNote(tempDir, CROP_BOX, 0);

        assertEquals(Optional.of(new AnnotationArea(80f, 92f, 20f, 20f)),
                importer.importAnnotations(file).getFirst().getArea());
    }

    @Test
    void rotatedPageReportsNoArea(@TempDir Path tempDir) throws IOException {
        Path file = writeSinglePageWithStickyNote(tempDir, MEDIA_BOX, 90);

        assertEquals(Optional.empty(), importer.importAnnotations(file).getFirst().getArea());
    }

    private static Path writeSinglePageWithStickyNote(Path directory, PDRectangle cropBox, int rotation) throws IOException {
        Path file = directory.resolve("annotated.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(MEDIA_BOX);
            page.setCropBox(cropBox);
            page.setRotation(rotation);

            PDAnnotationText stickyNote = new PDAnnotationText();
            stickyNote.setRectangle(ANNOTATION_RECTANGLE);
            stickyNote.setContents("a sticky note");
            page.getAnnotations().add(stickyNote);

            document.addPage(page);
            document.save(file.toFile());
        }
        return file;
    }

    @Test
    void highlightMinimalFoxit() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("lynyus", LocalDateTime.of(2017, 5, 31, 15, 16, 1), 1,
                "this is a foxit highlight", FileAnnotationType.HIGHLIGHT, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("lynyus", LocalDateTime.of(2017, 5, 31, 15, 16, 1), 1,
                "Hello", FileAnnotationType.HIGHLIGHT, Optional.of(expectedLinkedAnnotation));
        assertEquals(List.of(expected),
                importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-foxithighlight.pdf")));
    }

    @Test
    void highlightNoNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 28, 39), 1,
                "", FileAnnotationType.HIGHLIGHT, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 28, 39), 1,
                "World", FileAnnotationType.HIGHLIGHT, Optional.of(expectedLinkedAnnotation));

        assertEquals(List.of(expected),
                importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-highlight-no-note.pdf")));
    }

    @Test
    void squigglyWithNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("lynyus", LocalDateTime.of(2017, 6, 1, 2, 40, 25), 1,
                "Squiggly note", FileAnnotationType.SQUIGGLY, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("lynyus", LocalDateTime.of(2017, 6, 1, 2, 40, 25), 1,
                "ello", FileAnnotationType.SQUIGGLY, Optional.of(expectedLinkedAnnotation));

        assertEquals(List.of(expected),
                importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-squiggly.pdf")));
    }

    @Test
    void strikeoutWithNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("lynyus", LocalDateTime.of(2017, 6, 1, 13, 2, 3), 1,
                "striked out", FileAnnotationType.STRIKEOUT, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("lynyus", LocalDateTime.of(2017, 6, 1, 13, 2, 3), 1,
                "World", FileAnnotationType.STRIKEOUT, Optional.of(expectedLinkedAnnotation));

        assertEquals(List.of(expected),
                importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-strikeout.pdf")));
    }

    @Test
    void highlightWithNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 32, 2), 1,
                "linked note to highlight", FileAnnotationType.HIGHLIGHT, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 32, 2), 1,
                "World", FileAnnotationType.HIGHLIGHT, Optional.of(expectedLinkedAnnotation));

        assertEquals(List.of(expected),
                importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-highlight-with-note.pdf")));
    }

    @Test
    void underlineWithNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 36, 9), 1,
                "underlined", FileAnnotationType.UNDERLINE, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 36, 9), 1,
                "Hello", FileAnnotationType.UNDERLINE, Optional.of(expectedLinkedAnnotation));

        assertEquals(List.of(expected),
                importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-underline.pdf")));
    }

    @Test
    void polygonNoNoteMinimal() {
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 16, 9, 21, 1), 1,
                "polygon annotation", FileAnnotationType.POLYGON, Optional.empty());

        assertEquals(List.of(expected),
                importer.importAnnotations(Path.of("src/test/resources/pdfs/minimal-polygon.pdf")));
    }
}

