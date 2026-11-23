package org.jabref.gui.openoffice;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.engine.storage.DocumentStyleStore;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

@NullMarked
class CslStyleResolverTest {

    private final CitationStyle cslStyle = new CitationStyle("ieee.csl", "http://www.zotero.org/styles/ieee", "in-text", "IEEE", "IEEE", true, true, false, false, "source", true);
    private final CitationStyle externalCslStyle = new CitationStyle("external/research.csl", "https://example.org/styles/research", "in-text", "Research", "Research", true, true, false, false, "source", false);
    private final XTextDocument document = mock(XTextDocument.class);
    private final CslStyleResolver resolver = new CslStyleResolver(() -> List.of(cslStyle, externalCslStyle));

    @ParameterizedTest
    @ValueSource(strings = {"JSTYLE", "BST"})
    void cslInferenceRejectsOtherStoredFamiliesWithoutReadingStaleZoteroPreferences(String family) throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<ZoteroDocumentPreferences> zotero = mockStatic(ZoteroDocumentPreferences.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStyleStore.PROPERTY_NAME))
                      .thenReturn(Optional.of(family + "\nshared-path"));
            zotero.when(() -> ZoteroDocumentPreferences.findCitationStyle(document, List.of(cslStyle, externalCslStyle)))
                  .thenReturn(Optional.of(cslStyle));

            assertEquals(Optional.empty(), resolver.resolve(document));
            properties.verify(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStyleStore.PROPERTY_NAME));
            properties.verifyNoMoreInteractions();
            zotero.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void canonicalCslInferenceFindsBundledAndExternalStylesByPathOrUri(boolean external, boolean useUri) throws WrappedTargetException {
        CitationStyle expected = external ? externalCslStyle : cslStyle;
        String identity = useUri ? expected.getStyleId() : expected.getPath();
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<ZoteroDocumentPreferences> zotero = mockStatic(ZoteroDocumentPreferences.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStyleStore.PROPERTY_NAME))
                      .thenReturn(Optional.of("CSL\n" + identity));

            assertEquals(Optional.of(expected), resolver.resolve(document));
            zotero.verifyNoInteractions();
        }
    }

    @Test
    void canonicalCslInferenceDoesNotReplaceAMissingStyleWithStaleZoteroPreferences() throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<ZoteroDocumentPreferences> zotero = mockStatic(ZoteroDocumentPreferences.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStyleStore.PROPERTY_NAME))
                      .thenReturn(Optional.of("CSL\nmissing-style"));
            zotero.when(() -> ZoteroDocumentPreferences.findCitationStyle(document, List.of(cslStyle, externalCslStyle)))
                  .thenReturn(Optional.of(cslStyle));

            assertEquals(Optional.empty(), resolver.resolve(document));
            zotero.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ZOTERO_PREF_1", "CSL_PREF_1"})
    void nativeCslInferenceFindsExternalStyleByStoredUri(String property) throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property))
                      .thenReturn(Optional.of("<data><style id=\"https://example.org/styles/research\"/></data>"));

            assertEquals(Optional.of(externalCslStyle), resolver.resolve(document));
        }
    }

    @Test
    void cslInferenceWithoutStoredStyleReturnsEmpty() throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> _ = mockStatic(UnoUserDefinedProperty.class)) {
            assertEquals(Optional.empty(), resolver.resolve(document));
        }
    }

    @Test
    void nativeCslInferenceWithUnavailableStyleReturnsEmpty() throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, "ZOTERO_PREF_1"))
                      .thenReturn(Optional.of("<data><style id=\"https://example.org/styles/missing\"/></data>"));

            assertEquals(Optional.empty(), resolver.resolve(document));
        }
    }

    @Test
    void cslInferenceUsesStylesAddedAfterResolverConstruction() throws WrappedTargetException {
        AtomicReference<List<CitationStyle>> availableStyles = new AtomicReference<>(List.of(cslStyle));
        CslStyleResolver liveResolver = new CslStyleResolver(availableStyles::get);
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStyleStore.PROPERTY_NAME))
                      .thenReturn(Optional.of("CSL\nhttps://example.org/styles/research"));

            assertEquals(Optional.empty(), liveResolver.resolve(document));
            availableStyles.set(List.of(cslStyle, externalCslStyle));
            assertEquals(Optional.of(externalCslStyle), liveResolver.resolve(document));
        }
    }
}
