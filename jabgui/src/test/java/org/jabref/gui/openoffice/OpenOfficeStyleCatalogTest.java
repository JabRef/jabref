package org.jabref.gui.openoffice;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.engine.storage.DocumentStateStore;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.BstStyleLoader;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class OpenOfficeStyleCatalogTest {

    private final JStyleLoader jStyleLoader = mock(JStyleLoader.class);
    private final BstStyleLoader bstStyleLoader = mock(BstStyleLoader.class);
    private final JStyle jStyle = mock(JStyle.class);
    private final BstStyle bstStyle = mock(BstStyle.class);
    private final CitationStyle cslStyle = new CitationStyle("ieee.csl", "http://www.zotero.org/styles/ieee", "in-text", "IEEE", "IEEE", true, true, false, false, "source", true);
    private final CitationStyle externalCslStyle = new CitationStyle("external/research.csl", "https://example.org/styles/research", "in-text", "Research", "Research", true, true, false, false, "source", false);
    private final XTextDocument document = mock(XTextDocument.class);
    private final OpenOfficeStyleCatalog catalog = new OpenOfficeStyleCatalog(jStyleLoader, () -> List.of(cslStyle, externalCslStyle), bstStyleLoader);

    @BeforeEach
    void loadCatalogs() {
        when(jStyle.getPath()).thenReturn("shared-path");
        when(bstStyle.getPath()).thenReturn("shared-path");
        when(jStyleLoader.getStyles()).thenReturn(List.of(jStyle));
        when(bstStyleLoader.getStyles()).thenReturn(List.of(bstStyle));
    }

    @Test
    void pathMatchesAreQualifiedByStoredFamily() {
        assertEquals(List.of(Optional.of(jStyle), Optional.of(bstStyle)), List.of(
                catalog.find(new DocumentStateStore.State(DocumentStateStore.Family.JSTYLE, "shared-path")),
                catalog.find(new DocumentStateStore.State(DocumentStateStore.Family.BST, "shared-path"))));
    }

    @Test
    void cslCanBeRestoredByFilePathOrStyleUri() {
        assertEquals(List.of(Optional.of(cslStyle), Optional.of(cslStyle)), List.of(
                catalog.find(new DocumentStateStore.State(DocumentStateStore.Family.CSL, cslStyle.getPath())),
                catalog.find(new DocumentStateStore.State(DocumentStateStore.Family.CSL, cslStyle.getStyleId()))));
    }

    @Test
    void missingStoredStyleDoesNotSelectAnUnrelatedDefault() {
        assertEquals(Optional.empty(), catalog.find(new DocumentStateStore.State(DocumentStateStore.Family.JSTYLE, "missing-path")));
    }

    @ParameterizedTest
    @EnumSource(DocumentStateStore.Family.class)
    void canonicalRestorationUsesExplicitStoredFamilyWithoutReadingZoteroPreferences(DocumentStateStore.Family family) throws WrappedTargetException {
        OOStyle expected = switch (family) {
            case BST ->
                    bstStyle;
            case CSL ->
                    cslStyle;
            case JSTYLE ->
                    jStyle;
        };
        String identity = family.name() + "\n" + expected.getPath();
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<ZoteroDocumentPreferences> zotero = mockStatic(ZoteroDocumentPreferences.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStateStore.PROPERTY_NAME))
                      .thenReturn(Optional.of(identity));

            assertEquals(Optional.of(expected), catalog.restore(document, false));
            properties.verify(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStateStore.PROPERTY_NAME));
            properties.verifyNoMoreInteractions();
            zotero.verifyNoInteractions();
        }
    }

    @Test
    void zoteroRestorationDoesNotReadCanonicalDocumentState() throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<ZoteroDocumentPreferences> zotero = mockStatic(ZoteroDocumentPreferences.class)) {
            zotero.when(() -> ZoteroDocumentPreferences.findCitationStyle(document, List.of(cslStyle, externalCslStyle)))
                  .thenReturn(Optional.of(cslStyle));

            assertEquals(Optional.of(cslStyle), catalog.restore(document, true));
            properties.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"JSTYLE", "BST"})
    void cslInferenceRejectsOtherStoredFamiliesWithoutReadingStaleZoteroPreferences(String family) throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<ZoteroDocumentPreferences> zotero = mockStatic(ZoteroDocumentPreferences.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStateStore.PROPERTY_NAME))
                      .thenReturn(Optional.of(family + "\nshared-path"));
            zotero.when(() -> ZoteroDocumentPreferences.findCitationStyle(document, List.of(cslStyle, externalCslStyle)))
                  .thenReturn(Optional.of(cslStyle));

            assertEquals(Optional.empty(), catalog.inferCslStyle(document));
            properties.verify(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStateStore.PROPERTY_NAME));
            properties.verifyNoMoreInteractions();
            zotero.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void canonicalCslInferenceFindsExternalStylesByPathOrUri(boolean useUri) throws WrappedTargetException {
        String identity = useUri ? externalCslStyle.getStyleId() : externalCslStyle.getPath();
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<ZoteroDocumentPreferences> zotero = mockStatic(ZoteroDocumentPreferences.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStateStore.PROPERTY_NAME))
                      .thenReturn(Optional.of("CSL\n" + identity));

            assertEquals(Optional.of(externalCslStyle), catalog.inferCslStyle(document));
            zotero.verifyNoInteractions();
        }
    }

    @Test
    void canonicalCslInferenceDoesNotReplaceAMissingStyleWithStaleZoteroPreferences() throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class);
             MockedStatic<ZoteroDocumentPreferences> zotero = mockStatic(ZoteroDocumentPreferences.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStateStore.PROPERTY_NAME))
                      .thenReturn(Optional.of("CSL\nmissing-style"));
            zotero.when(() -> ZoteroDocumentPreferences.findCitationStyle(document, List.of(cslStyle, externalCslStyle)))
                  .thenReturn(Optional.of(cslStyle));

            assertEquals(Optional.empty(), catalog.inferCslStyle(document));
            zotero.verifyNoInteractions();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ZOTERO_PREF_1", "CSL_PREF_1"})
    void nativeCslInferenceFindsExternalStyleByStoredUri(String property) throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, property))
                      .thenReturn(Optional.of("<data><style id=\"https://example.org/styles/research\"/></data>"));

            assertEquals(Optional.of(externalCslStyle), catalog.inferCslStyle(document));
        }
    }

    @Test
    void cslInferenceWithoutStoredStyleReturnsEmpty() throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> _ = mockStatic(UnoUserDefinedProperty.class)) {
            assertEquals(Optional.empty(), catalog.inferCslStyle(document));
        }
    }

    @Test
    void nativeCslInferenceWithUnavailableStyleReturnsEmpty() throws WrappedTargetException {
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, "ZOTERO_PREF_1"))
                      .thenReturn(Optional.of("<data><style id=\"https://example.org/styles/missing\"/></data>"));

            assertEquals(Optional.empty(), catalog.inferCslStyle(document));
        }
    }

    @Test
    void cslInferenceUsesStylesAddedAfterRegistryConstruction() throws WrappedTargetException {
        AtomicReference<List<CitationStyle>> availableStyles = new AtomicReference<>(List.of(cslStyle));
        OpenOfficeStyleCatalog liveCatalog = new OpenOfficeStyleCatalog(jStyleLoader, availableStyles::get, bstStyleLoader);
        try (MockedStatic<UnoUserDefinedProperty> properties = mockStatic(UnoUserDefinedProperty.class)) {
            properties.when(() -> UnoUserDefinedProperty.getStringValue(document, DocumentStateStore.PROPERTY_NAME))
                      .thenReturn(Optional.of("CSL\nhttps://example.org/styles/research"));

            assertEquals(Optional.empty(), liveCatalog.inferCslStyle(document));
            availableStyles.set(List.of(cslStyle, externalCslStyle));
            assertEquals(Optional.of(externalCslStyle), liveCatalog.inferCslStyle(document));
        }
    }

    @Test
    void reloadingASelectedJstyleUsesTheSelectedCatalogEntry() throws IOException {
        assertSame(jStyle, catalog.reload(jStyle));
        verify(jStyle).ensureUpToDate();
    }

    @Test
    void reloadingCslDoesNotReloadAnUnrelatedJstyle() throws IOException {
        assertSame(cslStyle, catalog.reload(cslStyle));
        verify(jStyle, never()).ensureUpToDate();
    }

    @Test
    void reloadFailureIsNotSilentlyReplacedByADefault() throws IOException {
        IOException failure = new IOException("changed external style is unavailable");
        doThrow(failure).when(jStyle).ensureUpToDate();

        assertSame(failure, assertThrows(IOException.class, () -> catalog.reload(jStyle)));
    }
}
