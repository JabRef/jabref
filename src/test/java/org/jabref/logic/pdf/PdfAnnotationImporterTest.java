package org.jabref.logic.pdf;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.jabref.model.pdf.FileAnnotation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PdfAnnotationImporterTest {

    private AnnotationImporter importer = new PdfAnnotationImporter();

    @Test
    public void noAnnotationsWriteProtected() {

        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/write-protected.pdf"));
        assertEquals(Collections.emptyList(), annotations);
    }

    @Test
    public void noAnnotationsEncrypted() {
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/encrypted.pdf"));
        assertEquals(Collections.emptyList(), annotations);
    }

    @Test
    public void twoAnnotationsThesisExample() {

        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/thesis-example.pdf"));
        assertEquals(2, annotations.size());
    }

    @Test
    public void noAnnotationsMinimal() {
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal.pdf"));
        assertEquals(Collections.emptyList(), annotations);
    }

    @Test
    public void inlineNoteMinimal() {
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-inlinenote.pdf"));
        assertEquals(1, annotations.size());

        FileAnnotation note = annotations.get(0);
        assertEquals("Linus Dietz", note.getAuthor());
        assertEquals("inline note annotation", note.getContent());
        assertFalse(note.hasLinkedAnnotation());
        assertEquals("FreeText", note.getAnnotationType());
        assertEquals(1, note.getPage());
    }

    @Test
    public void popupNoteMinimal() {
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-popup.pdf"));
        assertEquals(1, annotations.size());

        FileAnnotation note = annotations.get(0);
        assertEquals("Linus Dietz", note.getAuthor());
        assertEquals("A simple pop-up note", note.getContent());
        assertFalse(note.hasLinkedAnnotation());
        assertEquals("Text", note.getAnnotationType());
        assertEquals(1, note.getPage());
    }

    @Test
    public void highlightNoNoteMinimal() {
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-highlight-no-note.pdf"));
        assertEquals(1, annotations.size());

        FileAnnotation note = annotations.get(0);
        assertEquals("Linus Dietz", note.getAuthor());
        assertEquals("World", note.getContent());
        assertEquals("Highlight", note.getAnnotationType());
        assertEquals(1, note.getPage());

        assertTrue(note.hasLinkedAnnotation());
        assertEquals("", note.getLinkedFileAnnotation().getContent());
        assertEquals("Linus Dietz", note.getLinkedFileAnnotation().getAuthor());
    }

    @Test
    public void highlightWithNoteMinimal() {
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-highlight-with-note.pdf"));

        assertEquals(1, annotations.size());

        FileAnnotation note = annotations.get(0);
        assertEquals("Linus Dietz", note.getAuthor());
        assertEquals("World", note.getContent());
        assertEquals("Highlight", note.getAnnotationType());
        assertEquals(1, note.getPage());

        assertTrue(note.hasLinkedAnnotation());
        assertEquals("linked note to highlight", note.getLinkedFileAnnotation().getContent());
        assertEquals("Linus Dietz", note.getLinkedFileAnnotation().getAuthor());
    }


    @Test
    public void underlineWithNoteMinimal() {
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-underline.pdf"));
        assertEquals(1, annotations.size());

        FileAnnotation note = annotations.get(0);
        assertEquals("Linus Dietz", note.getAuthor());
        assertEquals("Hello", note.getContent());
        assertEquals("Underline", note.getAnnotationType());
        assertEquals(1, note.getPage());

        assertTrue(note.hasLinkedAnnotation());
        assertEquals("underlined", note.getLinkedFileAnnotation().getContent());
        assertEquals("Linus Dietz", note.getLinkedFileAnnotation().getAuthor());
    }

    @Test
    public void polygonNoNoteMinimal() {
        List<FileAnnotation> annotations = importer.importAnnotations(Paths.get("src/test/resources/pdfs/minimal-polygon.pdf"));
        assertEquals(1, annotations.size());
        FileAnnotation polygon = annotations.get(0);
        assertEquals("Linus Dietz", polygon.getAuthor());
        assertEquals("polygon annotation", polygon.getContent());
        assertFalse(polygon.hasLinkedAnnotation());
        assertEquals("Polygon", polygon.getAnnotationType());
        assertEquals(1, polygon.getPage());
    }
}

