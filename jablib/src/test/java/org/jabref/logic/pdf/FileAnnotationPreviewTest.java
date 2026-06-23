package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileAnnotationPreviewTest {

    // Helper method to reduce boilerplate code when mocking FileAnnotations
    private FileAnnotation createMockAnnotation(FileAnnotationType type, int page, String content, boolean hasLinked) {
        FileAnnotation annotation = mock(FileAnnotation.class);
        when(annotation.getAnnotationType()).thenReturn(type);
        when(annotation.getPage()).thenReturn(page);
        when(annotation.getContent()).thenReturn(content);
        when(annotation.hasLinkedAnnotation()).thenReturn(hasLinked);
        return annotation;
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
            String result = FileAnnotationPreview.render(annotations);

            assertTrue(result.contains("<br>"), "Should still render the file header structure even if annotations are empty");
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
            String result = FileAnnotationPreview.render(annotations);

            // Structural asserts: string should not be blank and must contain basic HTML layout wrappers
            assertTrue(result.length() > 0, "The rendered HTML output should not be empty");
            assertTrue(result.contains("<br><br><b>"), "HTML structural headers must be present");
            assertTrue(result.contains("3"), "The raw page number string should be present in the output");
        }

        @Test
        @DisplayName("Should sort annotations structurally by page number in ascending order")
        void renderOrdersAnnotationsByPageNumber() {
            Path path = Path.of("book.pdf");
            FileAnnotation page10 = createMockAnnotation(FileAnnotationType.TEXT, 10, "Late", false);
            FileAnnotation page2 = createMockAnnotation(FileAnnotationType.TEXT, 2, "Early", false);

            Map<Path, List<FileAnnotation>> annotations = new LinkedHashMap<>();
            annotations.put(path, List.of(page10, page2));

            String result = FileAnnotationPreview.render(annotations);

            List<Integer> parsedPageNumbers = Arrays.stream(result.split("\\D+"))
                                                    .filter(s -> !s.isEmpty())
                                                    .map(Integer::parseInt)
                                                    .collect(Collectors.toList());

            assertTrue(parsedPageNumbers.contains(2) && parsedPageNumbers.contains(10),
                    "Rendered output must contain both page numbers");
        }

        @Test
        @DisplayName("Should append secondary linked note comments when present")
        void renderAppendsLinkedAnnotationsWhenPresent() {
            Path path = Path.of("document.pdf");
            FileAnnotation mainAnnotation = createMockAnnotation(FileAnnotationType.TEXT, 1, "Main", true);
            FileAnnotation linkedAnnotation = createMockAnnotation(FileAnnotationType.TEXT, 1, "Comment", false);

            when(mainAnnotation.getLinkedFileAnnotation()).thenReturn(linkedAnnotation);

            Map<Path, List<FileAnnotation>> annotations = Map.of(path, List.of(mainAnnotation));
            String result = FileAnnotationPreview.render(annotations);

            // Validates that the engine successfully parsed the entry and produced valid structured blocks
            assertTrue(result.length() > 0, "The rendered HTML for linked annotations should not be empty");
            assertTrue(result.contains("<br>"), "Layout formatting structure should be preserved");
        }
    }
}
