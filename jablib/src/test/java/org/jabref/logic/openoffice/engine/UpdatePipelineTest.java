package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.engine.storage.MarkWriter;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.uno.UnoStyle;

import com.sun.star.style.XStyle;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class UpdatePipelineTest {

    @ParameterizedTest
    @ValueSource(strings = {"paragraph", "header", "character"})
    void missingWriterStyleReportsItsNameAndCorrectiveActionBeforeRendering(String family) throws Exception {
        String missingStyle = "Required research " + family + " style";
        XTextDocument document = mock(XTextDocument.class);
        JStyle style = mock(JStyle.class);
        when(style.getReferenceParagraphFormat()).thenReturn("paragraph".equals(family) ? missingStyle : "Standard");
        when(style.getReferenceHeaderParagraphFormat()).thenReturn("header".equals(family) ? missingStyle : "Heading 2");
        when(style.isFormatCitations()).thenReturn(true);
        when(style.getCitationCharacterFormat()).thenReturn(missingStyle);
        RendererFactory renderers = mock(RendererFactory.class);
        CitationRenderer renderer = mock(CitationRenderer.class);
        when(renderers.create(style)).thenReturn(renderer);
        MarkWriter writer = mock(MarkWriter.class);
        UpdatePipeline pipeline = new UpdatePipeline(document, mock(OpenOfficePreferences.class), writer, renderers);
        XStyle existing = mock(XStyle.class);
        try (MockedStatic<UnoStyle> styles = mockStatic(UnoStyle.class)) {
            styles.when(() -> UnoStyle.getParagraphStyle(document, "Standard")).thenReturn(Optional.of(existing));
            styles.when(() -> UnoStyle.getParagraphStyle(document, "Heading 2")).thenReturn(Optional.of(existing));

            MissingWriterStyleException error = assertThrows(MissingWriterStyleException.class,
                    () -> pipeline.render(style, List.of(), List.of(), true));

            String expected = "character".equals(family)
                              ? Localization.lang("You must create the following character style in LibreOffice: %0", missingStyle)
                              : Localization.lang("You must create the following paragraph style in LibreOffice: %0", missingStyle);
            assertEquals(Optional.of(expected), error.localizedMessage());
            verifyNoInteractions(renderer, writer);
        }
    }
}
