package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.engine.storage.CitationMarkerWriter;
import org.jabref.logic.openoffice.engine.storage.DocumentStyleStore;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
class NonUndoableStylePropertiesTest {
    private final XTextDocument document = mock(XTextDocument.class);

    @Test
    void ordinaryCanonicalStyleWritesStillUseRawProperties() throws Exception {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            new DocumentStyleStore().store(document, new DocumentStyleStore.State(DocumentStyleStore.Family.CSL, "style-id"));
            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(document, DocumentStyleStore.PROPERTY_NAME, "CSL\nstyle-id"));
            undoable.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @CsvSource({
            "ZOTERO_PREF_1, not XML",
            "CSL_PREF_1, not XML",
            "ZOTERO_PREF_1, <data><style id=\"old\"/></data>",
            "CSL_PREF_1, <data><style id=\"old\"/></data>"
    })
    void ordinaryCanonicalStyleWritesIgnoreMalformedOrReadOnlyNativePreferences(String propertyName, String xml) throws Exception {
        CitationStyle style = mock(CitationStyle.class);
        when(style.getStyleId()).thenReturn("style-id");
        MarkStore store = mock(MarkStore.class);
        CitationMarkerWriter writer = mock(CitationMarkerWriter.class);
        UpdatePipeline pipeline = new UpdatePipeline(document, OpenOfficePreferences.getDefault(), writer, mock(RendererFactory.class));
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, propertyName)).thenReturn(Optional.of(xml));
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of(propertyName));
            properties.when(() -> UnoUserDefinedProperty.setStringProperty(eq(document), eq("ZOTERO_PREF_1"), anyString()))
                      .thenThrow(new PropertyVetoException("Read-only native preferences"));
            properties.when(() -> UnoUserDefinedProperty.setStringProperty(eq(document), eq("CSL_PREF_1"), anyString()))
                      .thenThrow(new PropertyVetoException("Read-only native preferences"));

            pipeline.apply(style, store, List.of(), new RenderResult(Map.of(), List.of(), List.of()), false);

            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(document, DocumentStyleStore.PROPERTY_NAME, "CSL\nstyle-id"));
            properties.verifyNoMoreInteractions();
            undoable.verifyNoInteractions();
            verifyNoInteractions(store);
        }
    }

    @Test
    void ordinaryZoteroStyleWritesStillUseRawProperties() throws Exception {
        CitationStyle style = mock(CitationStyle.class);
        when(style.getStyleId()).thenReturn("https://styles.test/numeric");
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(eq(document), anyString())).thenReturn(Optional.empty());
            properties.when(() -> UnoUserDefinedProperty.getListOfNames(document)).thenReturn(List.of());
            assertTrue(ZoteroDocumentPreferences.writeCitationStyle(document, style));
            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(eq(document), eq("ZOTERO_PREF_1"), anyString()));
            undoable.verifyNoInteractions();
        }
    }
}
