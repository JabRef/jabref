package org.jabref.logic.pdf;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PdfAnnotationImporterTest {

    private final AnnotationImporter importer = new PdfAnnotationImporter();

    @Test
    public void noAnnotationsWriteProtected() {

        List<FileAnnotation> annotations = importer
                .importAnnotations(Paths.get("src/test/resources/pdfs/write-protected.pdf"));
        assertEquals(Collections.emptyList(), annotations);
    }

    @Test
    public void noAnnotationsEncrypted() {
        List<FileAnnotation> annotations = importer
                .importAnnotations(Paths.get("src/test/resources/pdfs/encrypted.pdf"));
        assertEquals(Collections.emptyList(), annotations);
    }

    @Test
    public void twoAnnotationsThesisExample() {

        List<FileAnnotation> annotations = importer
                .importAnnotations(Paths.get("src/test/resources/pdfs/thesis-example.pdf"));
        assertEquals(2, annotations.size());
    }

    @Test
    public void noAnnotationsMinimal() {
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal.pdf"));
        assertEquals(Collections.emptyList(), annotations);
    }

    @Test
    public void inlineNoteMinimal() {
        final List<FileAnnotation> expected = Collections.singletonList(
                new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 25), 1,
                        "inline note annotation", FileAnnotationType.FREETEXT, Optional.empty()));

        assertEquals(expected,
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-inlinenote.pdf")));
    }

    @Test
    public void popupNoteMinimal() {
        // Fixme: Comparison Fails because seconds are missing, should be 24.
        final List<FileAnnotation> expected = Collections.singletonList(
                new FileAnnotation("Linus Dietz", LocalDateTime.of(2017, 3, 12, 20, 17), 1,
                        "A simple pop-up note", FileAnnotationType.TEXT, Optional.empty()));

        // Start old test style
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-popup.pdf"));
        FileAnnotation note = annotations.get(0);
        assertEquals("Linus Dietz", note.getAuthor());
        assertEquals(LocalDateTime.of(2017, 3, 12, 20, 17, 24), note.getTimeModified());
        assertEquals(1, note.getPage());
        assertEquals("A simple pop-up note", note.getContent());
        assertEquals(FileAnnotationType.TEXT, note.getAnnotationType());
        // End old test style

        assertEquals(expected,
                importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-popup.pdf")));
    }

    @Test
    public void highlightNoNoteMinimal() {
        List<FileAnnotation> annotations = importer
                .importAnnotations(Paths.get("src/test/resources/pdfs/minimal-highlight-no-note.pdf"));
        assertEquals(1, annotations.size());

        FileAnnotation note = annotations.get(0);
        assertEquals("Linus Dietz", note.getAuthor());
        assertEquals("World", note.getContent());
        assertEquals(FileAnnotationType.HIGHLIGHT, note.getAnnotationType());
        assertEquals(1, note.getPage());

        assertTrue(note.hasLinkedAnnotation());
        assertEquals("", note.getLinkedFileAnnotation().getContent());
        assertEquals("Linus Dietz", note.getLinkedFileAnnotation().getAuthor());
    }

    @Test
    public void highlightWithNoteMinimal() {
        List<FileAnnotation> annotations = importer
                .importAnnotations(Paths.get("src/test/resources/pdfs/minimal-highlight-with-note.pdf"));

        assertEquals(1, annotations.size());

        FileAnnotation note = annotations.get(0);
        assertEquals("Linus Dietz", note.getAuthor());
        assertEquals("World", note.getContent());
        assertEquals(FileAnnotationType.HIGHLIGHT, note.getAnnotationType());
        assertEquals(1, note.getPage());

        assertTrue(note.hasLinkedAnnotation());
        assertEquals("linked note to highlight", note.getLinkedFileAnnotation().getContent());
        assertEquals("Linus Dietz", note.getLinkedFileAnnotation().getAuthor());
    }

    @Test
    public void underlineWithNoteMinimal() {
        List<FileAnnotation> annotations = importer
                .importAnnotations(Paths.get("src/test/resources/pdfs/minimal-underline.pdf"));
        assertEquals(1, annotations.size());

        FileAnnotation note = annotations.get(0);
        assertEquals("Linus Dietz", note.getAuthor());
        assertEquals("Hello", note.getContent());
        assertEquals(FileAnnotationType.UNDERLINE, note.getAnnotationType());
        assertEquals(1, note.getPage());

        assertTrue(note.hasLinkedAnnotation());
        assertEquals("underlined", note.getLinkedFileAnnotation().getContent());
        assertEquals("Linus Dietz", note.getLinkedFileAnnotation().getAuthor());
    }

    @Test
    public void polygonNoNoteMinimal() {
        List<FileAnnotation> annotations = importer
                .importAnnotations(Paths.get("src/test/resources/pdfs/minimal-polygon.pdf"));
        assertEquals(1, annotations.size());
        FileAnnotation polygon = annotations.get(0);
        assertEquals("Linus Dietz", polygon.getAuthor());
        assertEquals("polygon annotation", polygon.getContent());
        assertFalse(polygon.hasLinkedAnnotation());
        assertEquals(FileAnnotationType.POLYGON, polygon.getAnnotationType());
        assertEquals(1, polygon.getPage());
    }
}
