package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.jabref.logic.util.strings.StringUtil.quoteForHTML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileAnnotationPreviewTest {

    private FileAnnotation createMockAnnotation(FileAnnotationType type, int page, String content, boolean hasLinked) {
        FileAnnotation annotation = mock(FileAnnotation.class);
        when(annotation.getAnnotationType()).thenReturn(type);
        when(annotation.getPage()).thenReturn(page);
        when(annotation.getContent()).thenReturn(content);
        when(annotation.hasLinkedAnnotation()).thenReturn(hasLinked);
        return annotation;
    }

    private String escape(String text) {
        return quoteForHTML(text);
    }

    @Nested
    @DisplayName("Edge Cases and Filtering")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should return empty string when annotation map is empty")
        void renderReturnsEmptyStringWhenMapIsEmpty() {
            assertEquals("", FileAnnotationPreview.render(Collections.emptyMap()),
                    "An empty map must produce an empty string output");
        }

        @Test
        @DisplayName("Should handle maps with empty content or null elements gracefully")
        void renderFiltersOutNullAnnotationsAndEmptyContent() {
            Path path = Path.of("test.pdf");
            FileAnnotation nullAnnotation = null;
            FileAnnotation blankAnnotation = createMockAnnotation(FileAnnotationType.TEXT, 1, "", false);

            Map<Path, List<FileAnnotation>> annotations = Map.of(path, Arrays.asList(nullAnnotation, blankAnnotation));

            String expectedHtml = "<br><br><b>PDF Annotations</b><br><br><i>" + escape("test.pdf") + "</i><br>";

            assertEquals(expectedHtml, FileAnnotationPreview.render(annotations));
        }
    }

    @Nested
    @DisplayName("HTML Content Formatting and Ordering")
    class FormattingTests {

        @Test
        @DisplayName("Should format valid annotations and safely include type and page metadata")
        void renderFormatsValidAnnotationsCorrectlyWithHtmlEscaping() {
            Path path = Path.of("article.pdf");
            FileAnnotation annotation = createMockAnnotation(FileAnnotationType.HIGHLIGHT, 3, "This & That", false);

            Map<Path, List<FileAnnotation>> annotations = Map.of(path, List.of(annotation));

            String expectedHtml = "<br><br><b>PDF Annotations</b><br><br><i>" + escape("article.pdf") + "</i><br>"
                    + "<b>" + escape("Highlight") + " (Page 3):</b> " + escape("This & That") + "<br>";

            assertEquals(expectedHtml, FileAnnotationPreview.render(annotations));
        }

        @Test
        @DisplayName("Should sort annotations structurally by page number in ascending order")
        void renderOrdersAnnotationsByNumber() {
            Path path = Path.of("book.pdf");
            FileAnnotation page10 = createMockAnnotation(FileAnnotationType.TEXT, 10, "Late", false);
            FileAnnotation page2 = createMockAnnotation(FileAnnotationType.TEXT, 2, "Early", false);

            Map<Path, List<FileAnnotation>> annotations = new LinkedHashMap<>();
            annotations.put(path, List.of(page10, page2));

            String expectedHtml = "<br><br><b>PDF Annotations</b><br><br><i>" + escape("book.pdf") + "</i><br>"
                    + "<b>" + escape("Text") + " (Page 2):</b> " + escape("Early") + "<br>"
                    + "<b>" + escape("Text") + " (Page 10):</b> " + escape("Late") + "<br>";

            assertEquals(expectedHtml, FileAnnotationPreview.render(annotations));
        }

        @Test
        @DisplayName("Should append secondary linked note comments when present")
        void renderAppendsLinkedAnnotationsWhenPresent() {
            Path path = Path.of("document.pdf");
            FileAnnotation mainAnnotation = createMockAnnotation(FileAnnotationType.TEXT, 1, "Main", true);
            FileAnnotation linkedAnnotation = createMockAnnotation(FileAnnotationType.TEXT, 1, "Comment", false);

            when(mainAnnotation.getLinkedFileAnnotation()).thenReturn(linkedAnnotation);

            Map<Path, List<FileAnnotation>> annotations = Map.of(path, List.of(mainAnnotation));

            String expectedHtml = "<br><br><b>PDF Annotations</b><br><br><i>" + escape("document.pdf") + "</i><br>"
                    + "<b>" + escape("Text") + " (Page 1):</b> " + escape("Main") + " — <i>Note: " + escape("Comment") + "</i><br>";

            assertEquals(expectedHtml, FileAnnotationPreview.render(annotations));
        }
    }
}
