package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.engine.storage.MarkStore;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.WriterDocumentAccessException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.style.CitationType;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class CitationEngineTest {

    private final XTextDocument document = mock(XTextDocument.class);
    private final XComponentContext context = mock(XComponentContext.class);
    private final BibEntryTypesManager entryTypes = new BibEntryTypesManager();
    private final OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();

    @ParameterizedTest
    @MethodSource("nonCslModes")
    void switchingFamiliesRestoresCslCapabilitiesWithoutChangingTheSavedMode(OOStyle nonCslStyle, boolean zoteroMode) throws RenderException {
        preferences.setZoteroCompatibilityMode(zoteroMode);
        try (MockedStatic<WriterReferenceMarks> marks = mockStatic(WriterReferenceMarks.class);
             CitationEngine engine = new CitationEngine(document, context, preferences, entryTypes)) {
            marks.when(() -> WriterReferenceMarks.getListOfNames(document)).thenReturn(List.of());
            StyleCapabilities cslCapabilities = engine.capabilities(preferences.getCurrentStyle());
            assertEquals(!zoteroMode, cslCapabilities.supportedTypes().contains(CitationType.INVISIBLE_CIT));

            preferences.setCurrentStyle(nonCslStyle);
            assertEquals(new RendererFactory(entryTypes, preferences.getPandocPath()).create(nonCslStyle).capabilities(),
                    engine.capabilities(preferences.getCurrentStyle()));
            assertFalse(engine.requiresFormatConversion(nonCslStyle).get());
            preferences.setCurrentStyle(CSLStyleLoader.getDefaultStyle());

            assertEquals(cslCapabilities, engine.capabilities(preferences.getCurrentStyle()));
            assertEquals(zoteroMode, preferences.getZoteroCompatibilityMode());
        }
    }

    @ParameterizedTest
    @MethodSource("nonCslStyles")
    void nonCslActionsUseCanonicalStorageWhenTheSavedZoteroModeIsOn(OOStyle style) throws WriterDocumentAccessException, WrappedTargetException {
        preferences.setCurrentStyle(style);
        preferences.setZoteroCompatibilityMode(true);
        WriterDocumentAccessException failure = new WriterDocumentAccessException("Canonical storage unavailable");
        try (MockedConstruction<MarkStore> stores = mockConstruction(MarkStore.class,
                (store, _) -> when(store.readAll(document)).thenThrow(failure));
             MockedConstruction<ZoteroCitationStrategy> strategies = mockConstruction(ZoteroCitationStrategy.class);
             MockedStatic<WriterReferenceMarks> marks = mockStatic(WriterReferenceMarks.class);
             CitationEngine engine = new CitationEngine(document, context, preferences, entryTypes)) {
            marks.when(() -> WriterReferenceMarks.getListOfNames(document)).thenReturn(List.of());
            assertSame(failure, engine.insert(List.of(new BibEntry().withCitationKey("alpha")), List.of(), style,
                    CitationType.AUTHORYEAR_PAR, "", mock(XTextCursor.class), false).getError().getCause());
            assertSame(failure, engine.refresh(List.of(), style).getError().getCause());
            assertSame(failure, engine.exportCited(List.of()).getError().getCause());

            assertEquals(3, stores.constructed().size());
            for (MarkStore store : stores.constructed()) {
                verify(store).readAll(document);
            }
            verifyNoInteractions(strategies.constructed().getFirst());
            assertTrue(preferences.getZoteroCompatibilityMode());
        }
    }

    // [utest->req~openoffice.legacy-jstyle-migration~1]
    @ParameterizedTest
    @MethodSource("nonCslModes")
    void nonCslInsertionDetectsLegacyMarksRegardlessOfTheSavedZoteroChoice(OOStyle style, boolean zoteroMode) {
        preferences.setZoteroCompatibilityMode(zoteroMode);
        try (MockedStatic<WriterReferenceMarks> marks = mockStatic(WriterReferenceMarks.class);
             CitationEngine engine = new CitationEngine(document, context, preferences, entryTypes)) {
            marks.when(() -> WriterReferenceMarks.getListOfNames(document))
                 .thenReturn(List.of("unrelated", "JABREF_unknown CID_7 old NORMAL", "JR_cite_1_alpha"));

            assertTrue(engine.requiresFormatConversion(style).get());
            assertEquals(zoteroMode, preferences.getZoteroCompatibilityMode());
        }
    }

    // [utest->req~openoffice.legacy-jstyle-migration~1]
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void cslOnlyMigratesLegacyMarksOutsideNativeZoteroMode(boolean zoteroMode) {
        preferences.setZoteroCompatibilityMode(zoteroMode);
        try (MockedStatic<WriterReferenceMarks> marks = mockStatic(WriterReferenceMarks.class);
             CitationEngine engine = new CitationEngine(document, context, preferences, entryTypes)) {
            marks.when(() -> WriterReferenceMarks.getListOfNames(document)).thenReturn(List.of("JR_cite_1_alpha"));

            assertEquals(!zoteroMode, engine.requiresFormatConversion(preferences.getCurrentStyle()).get());
        }
    }

    private static Stream<Arguments> nonCslModes() {
        return nonCslStyles().flatMap(style -> Stream.of(Arguments.of(style, false), Arguments.of(style, true)));
    }

    private static Stream<OOStyle> nonCslStyles() {
        return Stream.of(mock(JStyle.class), mock(BstStyle.class));
    }
}
