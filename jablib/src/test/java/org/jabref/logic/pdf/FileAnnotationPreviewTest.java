package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.pdf.FileAnnotation;
import org.jabref.model.pdf.FileAnnotationType;

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
    class EdgeCasesAndFilteringTests {

        @Test
        void renderReturnsEmptyStringWhenMapIsEmpty() {
            assertEquals("", FileAnnotationPreview.render(Map.of()),
                    "An empty map must produce an empty string output");
        }

        @Test
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
    class HtmlContentFormattingAndOrderingTests {

        @Test
        void renderFormatsValidAnnotationsCorrectlyWithHtmlEscaping() {
            Path path = Path.of("article.pdf");
            FileAnnotation annotation = createMockAnnotation(FileAnnotationType.HIGHLIGHT, 3, "This & That", false);

            Map<Path, List<FileAnnotation>> annotations = Map.of(path, List.of(annotation));

            String expectedHeader = Localization.lang("%0 (Page %1):", "Highlight", "3");
            String expectedHtml = "<br><br><b>PDF Annotations</b><br><br><i>" + escape("article.pdf") + "</i><br>"
                    + "<b>" + escape(expectedHeader) + "</b> " + escape("This & That") + "<br>";

            assertEquals(expectedHtml, FileAnnotationPreview.render(annotations));
        }

        @Test
        void renderOrdersAnnotationsByPageNumberInAscendingOrder() {
            Path path = Path.of("book.pdf");
            FileAnnotation page10 = createMockAnnotation(FileAnnotationType.TEXT, 10, "Late", false);
            FileAnnotation page2 = createMockAnnotation(FileAnnotationType.TEXT, 2, "Early", false);

            Map<Path, List<FileAnnotation>> annotations = new LinkedHashMap<>();
            annotations.put(path, List.of(page10, page2));

            String headerPage2 = Localization.lang("%0 (Page %1):", "Text", "2");
            String headerPage10 = Localization.lang("%0 (Page %1):", "Text", "10");

            String expectedHtml = "<br><br><b>PDF Annotations</b><br><br><i>" + escape("book.pdf") + "</i><br>"
                    + "<b>" + escape(headerPage2) + "</b> " + escape("Early") + "<br>"
                    + "<b>" + escape(headerPage10) + "</b> " + escape("Late") + "<br>";

            assertEquals(expectedHtml, FileAnnotationPreview.render(annotations));
        }

        @Test
        void renderAppendsSecondaryLinkedNoteCommentsWhenPresent() {
            Path path = Path.of("document.pdf");
            FileAnnotation mainAnnotation = createMockAnnotation(FileAnnotationType.TEXT, 1, "Main", true);
            FileAnnotation linkedAnnotation = createMockAnnotation(FileAnnotationType.TEXT, 1, "Comment", false);

            when(mainAnnotation.getLinkedFileAnnotation()).thenReturn(linkedAnnotation);

            Map<Path, List<FileAnnotation>> annotations = Map.of(path, List.of(mainAnnotation));

            String expectedHeader = Localization.lang("%0 (Page %1):", "Text", "1");
            String expectedNote = Localization.lang(" — Note: %0", "Comment");

            String expectedHtml = "<br><br><b>PDF Annotations</b><br><br><i>" + escape("document.pdf") + "</i><br>"
                    + "<b>" + escape(expectedHeader) + "</b> " + escape("Main") + "<i>" + escape(expectedNote) + "</i><br>";

            assertEquals(expectedHtml, FileAnnotationPreview.render(annotations));
        }
    }
}
