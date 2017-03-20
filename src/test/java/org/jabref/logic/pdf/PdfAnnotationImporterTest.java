package org.jabref.logic.pdf;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PdfAnnotationImporterTest {

    private final AnnotationImporter importer = new PdfAnnotationImporter();

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
    public void highlightNoNoteMinimal() {
        final FileAnnotation expectedLinkedAnnotation = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 28, 39), 1,
                "", FileAnnotationType.HIGHLIGHT, Optional.empty());
        final FileAnnotation expected = new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 28, 39), 1,
                "World", FileAnnotationType.HIGHLIGHT, Optional.of(expectedLinkedAnnotation));

        assertEquals(Collections.singletonList(expected),
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-highlight-no-note.pdf")));
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

