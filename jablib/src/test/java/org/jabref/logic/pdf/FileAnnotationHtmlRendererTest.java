package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileAnnotationHtmlRendererTest {

    private static final LocalDateTime TIME = LocalDateTime.of(2024, 1, 15, 10, 30);

    private FileAnnotation createAnnotation(String content, int page, FileAnnotationType type) {
        return new FileAnnotation("Author", TIME, page, content, type, Optional.empty());
    }

    @Test
    void renderEmptyMapReturnsEmptyString() {
        String result = FileAnnotationHtmlRenderer.render(Collections.emptyMap());
        assertEquals("", result);
    }

    @Test
    void renderNullReturnsEmptyString() {
        String result = FileAnnotationHtmlRenderer.render(null);
        assertEquals("", result);
    }

    @Test
    void renderSingleHighlightAnnotation() {
        FileAnnotation annotation = createAnnotation("important text", 3, FileAnnotationType.HIGHLIGHT);
        Map<Path, List<FileAnnotation>> annotations = Map.of(Path.of("paper.pdf"), List.of(annotation));

        String result = FileAnnotationHtmlRenderer.render(annotations);

        assertTrue(result.contains("Highlight"));
        assertTrue(result.contains("p. 3"));
        assertTrue(result.contains("important text"));
        assertTrue(result.contains("paper.pdf"));
    }

    @Test
    void renderMultipleAnnotationsSortedByPage() {
        FileAnnotation page5 = createAnnotation("later text", 5, FileAnnotationType.TEXT);
        FileAnnotation page2 = createAnnotation("earlier text", 2, FileAnnotationType.HIGHLIGHT);
        Map<Path, List<FileAnnotation>> annotations = Map.of(Path.of("paper.pdf"), List.of(page5, page2));

        String result = FileAnnotationHtmlRenderer.render(annotations);

        int posEarlier = result.indexOf("earlier text");
        int posLater = result.indexOf("later text");
        assertTrue(posEarlier < posLater, "Page 2 annotation should appear before page 5");
    }

    @Test
    void renderFiltersEmptyAnnotations() {
        FileAnnotation empty = createAnnotation("", 1, FileAnnotationType.HIGHLIGHT);
        FileAnnotation valid = createAnnotation("real content", 2, FileAnnotationType.TEXT);
        Map<Path, List<FileAnnotation>> annotations = Map.of(Path.of("paper.pdf"), List.of(empty, valid));

        String result = FileAnnotationHtmlRenderer.render(annotations);

        assertTrue(result.contains("real content"));
        // empty annotation should not produce a type label for page 1
        assertFalse(result.contains("p. 1"));
    }

    @Test
    void renderEscapesHtmlCharacters() {
        FileAnnotation annotation = createAnnotation("<script>alert('xss')</script>", 1, FileAnnotationType.TEXT);
        Map<Path, List<FileAnnotation>> annotations = Map.of(Path.of("paper.pdf"), List.of(annotation));

        String result = FileAnnotationHtmlRenderer.render(annotations);

        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("&lt;script&gt;"));
    }

    @Test
    void renderMultipleFiles() {
        FileAnnotation ann1 = createAnnotation("from first file", 1, FileAnnotationType.HIGHLIGHT);
        FileAnnotation ann2 = createAnnotation("from second file", 1, FileAnnotationType.TEXT);
        Map<Path, List<FileAnnotation>> annotations = new LinkedHashMap<>();
        annotations.put(Path.of("first.pdf"), List.of(ann1));
        annotations.put(Path.of("second.pdf"), List.of(ann2));

        String result = FileAnnotationHtmlRenderer.render(annotations);

        assertTrue(result.contains("first.pdf"));
        assertTrue(result.contains("second.pdf"));
        assertTrue(result.contains("from first file"));
        assertTrue(result.contains("from second file"));
    }

    @Test
    void renderLinkedAnnotationShowsNote() {
        FileAnnotation note = createAnnotation("my note", 3, FileAnnotationType.TEXT, Optional.empty());
        FileAnnotation highlight = new FileAnnotation("Author", TIME, 3, "highlighted text", FileAnnotationType.HIGHLIGHT, Optional.of(note));
        Map<Path, List<FileAnnotation>> annotations = Map.of(Path.of("paper.pdf"), List.of(highlight));

        String result = FileAnnotationHtmlRenderer.render(annotations);

        assertTrue(result.contains("highlighted text"));
        assertTrue(result.contains("my note"));
    }

    private FileAnnotation createAnnotation(String content, int page, FileAnnotationType type, Optional<FileAnnotation> linked) {
        return new FileAnnotation("Author", TIME, page, content, type, linked);
    }
}
