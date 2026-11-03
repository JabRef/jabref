package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.engine.storage.DocumentStateStore;
import org.jabref.logic.openoffice.engine.storage.UndoableDocumentProperties;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;

import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@NullMarked
class OrdinaryStylePropertiesTest {
    private final XTextDocument document = mock(XTextDocument.class);

    @Test
    void ordinaryCanonicalStyleWritesStillUseRawProperties() throws Exception {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<UndoableDocumentProperties> undoable = mockStatic(UndoableDocumentProperties.class)) {
            new DocumentStateStore().store(document, new DocumentStateStore.State(DocumentStateStore.Family.CSL, "style-id"));
            properties.verify(() -> UnoUserDefinedProperty.setStringProperty(document, DocumentStateStore.PROPERTY_NAME, "CSL\nstyle-id"));
            undoable.verifyNoInteractions();
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
