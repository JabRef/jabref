package org.jabref.logic.pdf;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class PdfAnnotationImporterTest {

    private final AnnotationImporter importer = new PdfAnnotationImporter();

    @Test
    public void invalidPath() {
        assertEquals(Collections.emptyList(), importer.importAnnotations(Paths.get("/asdf/does/not/exist.pdf")));
    }

    @Test
    public void invalidDirectory() {
        assertEquals(Collections.emptyList(), importer.importAnnotations(Paths.get("src/test/resources/pdfs")));
    }

    @Test
    public void invalidDocumentType() {
        assertEquals(Collections.emptyList(), importer.importAnnotations(Paths.get("src/test/resources/pdfs/write-protected.docx")));
    }

    @Test
    public void noAnnotationsWriteProtected() {
        assertEquals(Collections.emptyList(), importer.importAnnotations(Paths.get("src/test/resources/pdfs/write-protected.pdf")));
    }

    @Test
    public void noAnnotationsEncrypted() {
        assertEquals(Collections.emptyList(), importer.importAnnotations(Paths.get("src/test/resources/pdfs/encrypted.pdf")));
    }

    @Test
    public void twoAnnotationsThesisExample() {
        assertEquals(2, importer.importAnnotations(Paths.get("src/test/resources/pdfs/thesis-example.pdf")).size());
    }

    @Test
    public void noAnnotationsMinimal() {
        assertEquals(Collections.emptyList(), importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal.pdf")));
    }

    @Test
    public void inlineNoteMinimal() {
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 25), 1,
                "inline note annotation", FileAnnotationType.FREETEXT, Optional.empty());

        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-inlinenote.pdf")));
    }

    @Test
    public void popupNoteMinimal() {
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 17, 24), 1,
                "A simple pop-up note", FileAnnotationType.TEXT, Optional.empty());

        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-popup.pdf")));
    }

    @Test
    public void highlightMinimalFoxit() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("lynyus", LocalDateTime.of(2017, 5, 31, 15, 16, 1), 1,
                "this is a foxit highlight", FileAnnotationType.HIGHLIGHT, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("lynyus", LocalDateTime.of(2017, 5, 31, 15, 16, 1), 1,
                "Hello", FileAnnotationType.HIGHLIGHT, Optional.of(expectedLinkedAnnotation));
        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-foxithighlight.pdf")));
    }

    @Test
    public void highlightNoNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 28, 39), 1,
                "", FileAnnotationType.HIGHLIGHT, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 28, 39), 1,
                "World", FileAnnotationType.HIGHLIGHT, Optional.of(expectedLinkedAnnotation));

        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-highlight-no-note.pdf")));
    }

    @Test
    public void squigglyWithNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("lynyus", LocalDateTime.of(2017, 6, 1, 2, 40, 25), 1,
                "Squiggly note", FileAnnotationType.SQUIGGLY, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("lynyus", LocalDateTime.of(2017, 6, 1, 2, 40, 25), 1,
                "ello", FileAnnotationType.SQUIGGLY, Optional.of(expectedLinkedAnnotation));

        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-squiggly.pdf")));
    }

    @Test
    public void strikeoutWithNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("lynyus", LocalDateTime.of(2017, 6, 1, 13, 2, 3), 1,
                "striked out", FileAnnotationType.STRIKEOUT, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("lynyus", LocalDateTime.of(2017, 6, 1, 13, 2, 3), 1,
                "World", FileAnnotationType.STRIKEOUT, Optional.of(expectedLinkedAnnotation));

        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-strikeout.pdf")));
    }

    @Test
    public void highlightWithNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 32, 2), 1,
                "linked note to highlight", FileAnnotationType.HIGHLIGHT, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 32, 2), 1,
                "World", FileAnnotationType.HIGHLIGHT, Optional.of(expectedLinkedAnnotation));

        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-highlight-with-note.pdf")));
    }

    @Test
    public void underlineWithNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 36, 9), 1,
                "underlined", FileAnnotationType.UNDERLINE, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 36, 9), 1,
                "Hello", FileAnnotationType.UNDERLINE, Optional.of(expectedLinkedAnnotation));

        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-underline.pdf")));
    }

    @Test
    public void polygonNoNoteMinimal() {
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 16, 9, 21, 1), 1,
                "polygon annotation", FileAnnotationType.POLYGON, Optional.empty());

        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-polygon.pdf")));
    }
}

