package org.jabref.gui.openoffice;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.gui.DialogService;
import org.jabref.logic.JabRefException;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.engine.CitationEngine;
import org.jabref.logic.openoffice.engine.CitedEntryExporter;
import org.jabref.logic.openoffice.engine.MissingCitationLabelException;
import org.jabref.logic.openoffice.engine.MissingWriterStyleException;
import org.jabref.logic.openoffice.engine.RenderException;
import org.jabref.logic.openoffice.engine.StyleCapabilities;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.UnoCursor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.jabref.logic.l10n.Localization.lang;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class OpenOfficeCitationControllerTest {

    private final DialogService dialogs = mock(DialogService.class);
    private final OpenOfficePreferences preferences = mock(OpenOfficePreferences.class);
    private final OpenOfficeDocumentConnection connection = mock(OpenOfficeDocumentConnection.class);
    private final XComponentContext context = mock(XComponentContext.class);
    private final XTextDocument document = mock(XTextDocument.class);
    private final OOStyle style = mock(CitationStyle.class);
    private final BibEntryTypesManager entryTypes = mock(BibEntryTypesManager.class);

    @BeforeEach
    void connectToDocument() {
        when(connection.getComponentContext()).thenReturn(context);
        when(connection.getXTextDocument()).thenReturn(OOResult.ok(document));
        when(connection.isConnectedToDocument()).thenReturn(true);
        when(connection.selectDocument(true)).thenReturn(OOVoidResult.ok());
        when(connection.getCurrentDocumentTitle()).thenReturn(Optional.of("document"));
        when(preferences.getCurrentStyle()).thenReturn(style);
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void insertionUsesSyncWhenCitingPreferenceInCanonicalAndZoteroModes(boolean zoteroMode, boolean syncWhenCiting) {
        when(preferences.getZoteroCompatibilityMode()).thenReturn(zoteroMode);
        when(preferences.getSyncWhenCiting()).thenReturn(syncWhenCiting);
        BibEntry entry = new BibEntry().withCitationKey("alpha");
        BibDatabase active = new BibDatabase(List.of(entry));
        List<BibDatabase> databases = List.of(active, new BibDatabase());
        XTextViewCursor cursor = mock(XTextViewCursor.class);
        try (MockedConstruction<CitationEngine> engines = mockEngines();
             MockedStatic<UnoCursor> cursors = mockStatic(UnoCursor.class)) {
            cursors.when(() -> UnoCursor.getViewCursor(document)).thenReturn(Optional.of(cursor));
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();
            when(engine.insert(List.of(entry), databases, style, CitationType.AUTHORYEAR_INTEXT, "p. 5", cursor, syncWhenCiting))
                    .thenReturn(OOResult.ok(List.of()));

            citationController.insertCitation(List.of(entry), databases, style, CitationType.AUTHORYEAR_INTEXT, "p. 5");

            verify(engine).requiresFormatConversion(style);
            verify(engine).insert(List.of(entry), databases, style, CitationType.AUTHORYEAR_INTEXT, "p. 5", cursor, syncWhenCiting);
            verifyNoInteractions(dialogs);
            verify(document, never()).lockControllers();
            verify(document, never()).unlockControllers();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
        // [utest->req~openoffice.insertion-unresolved-warning~1]
    void insertionWarnsWhenExistingCitationKeysAreUnresolved(boolean syncWhenCiting) {
        when(preferences.getSyncWhenCiting()).thenReturn(syncWhenCiting);
        BibEntry entry = new BibEntry().withCitationKey("new");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        XTextViewCursor cursor = mock(XTextViewCursor.class);
        try (MockedConstruction<CitationEngine> engines = mockEngines();
             MockedStatic<UnoCursor> cursors = mockStatic(UnoCursor.class)) {
            cursors.when(() -> UnoCursor.getViewCursor(document)).thenReturn(Optional.of(cursor));
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();
            when(engine.insert(List.of(entry), databases, style, CitationType.AUTHORYEAR_PAR, "", cursor, syncWhenCiting))
                    .thenReturn(OOResult.ok(List.of("old", "other")));

            citationController.insertCitation(List.of(entry), databases, style, CitationType.AUTHORYEAR_PAR, "");

            InOrder order = inOrder(engine, dialogs);
            order.verify(engine).insert(List.of(entry), databases, style, CitationType.AUTHORYEAR_PAR, "", cursor, syncWhenCiting);
            order.verify(dialogs).showWarningDialogAndWait(lang("Unresolved citations"),
                    lang("The citation was inserted, but some existing citations could not be found in the selected libraries: %0. Open the corresponding libraries and refresh the document.", "old, other"));
            verify(dialogs, never()).showErrorDialogAndWait(anyString(), anyString());
        }
    }

    @Test
    void insertionFailureShowsAnErrorWithoutAnUnresolvedCitationWarning() {
        BibEntry entry = new BibEntry().withCitationKey("new");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        XTextViewCursor cursor = mock(XTextViewCursor.class);
        try (MockedConstruction<CitationEngine> engines = mockEngines();
             MockedStatic<UnoCursor> cursors = mockStatic(UnoCursor.class)) {
            cursors.when(() -> UnoCursor.getViewCursor(document)).thenReturn(Optional.of(cursor));
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();
            when(engine.insert(List.of(entry), databases, style, CitationType.AUTHORYEAR_PAR, "", cursor, false))
                    .thenReturn(OOResult.error(new JabRefException("render failed")));

            citationController.insertCitation(List.of(entry), databases, style, CitationType.AUTHORYEAR_PAR, "");

            verify(dialogs).showErrorDialogAndWait(lang("Could not insert citation"), "render failed");
            verify(dialogs, never()).showWarningDialogAndWait(anyString(), anyString());
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {CitationStyle.class, JStyle.class, BstStyle.class})
    void insertionWithoutSyncStillUsesEngineWithEmptyPageInfo(Class<? extends OOStyle> styleClass) {
        when(preferences.getSyncWhenCiting()).thenReturn(false);
        OOStyle selectedStyle = mock(styleClass);
        BibEntry entry = new BibEntry().withCitationKey("alpha");
        BibDatabase database = new BibDatabase(List.of(entry));
        XTextViewCursor cursor = mock(XTextViewCursor.class);
        try (MockedConstruction<CitationEngine> engines = mockEngines();
             MockedStatic<UnoCursor> cursors = mockStatic(UnoCursor.class)) {
            cursors.when(() -> UnoCursor.getViewCursor(document)).thenReturn(Optional.of(cursor));
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();
            when(engine.insert(List.of(entry), List.of(database), selectedStyle, CitationType.AUTHORYEAR_PAR, "", cursor, false))
                    .thenReturn(OOResult.ok(List.of()));

            citationController.insertCitation(List.of(entry), List.of(database), selectedStyle, CitationType.AUTHORYEAR_PAR, "");

            verify(engine).requiresFormatConversion(selectedStyle);
            verify(engine).insert(List.of(entry), List.of(database), selectedStyle, CitationType.AUTHORYEAR_PAR, "", cursor, false);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void insertionWarnsBeforeMutationWhenFormatConversionIsRequired(boolean zoteroMode) {
        when(preferences.getZoteroCompatibilityMode()).thenReturn(zoteroMode);
        BibEntry entry = new BibEntry().withCitationKey("alpha");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        XTextViewCursor cursor = mock(XTextViewCursor.class);
        try (MockedConstruction<CitationEngine> engines = mockEngines();
             MockedStatic<UnoCursor> cursors = mockStatic(UnoCursor.class)) {
            cursors.when(() -> UnoCursor.getViewCursor(document)).thenReturn(Optional.of(cursor));
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();
            when(engine.requiresFormatConversion(style)).thenReturn(OOResult.ok(true));
            when(engine.insert(List.of(entry), databases, style, CitationType.AUTHORYEAR_PAR, "", cursor, false))
                    .thenReturn(OOResult.ok(List.of()));

            citationController.insertCitation(List.of(entry), databases, style, CitationType.AUTHORYEAR_PAR, "");

            InOrder order = inOrder(cursor, engine, dialogs);
            order.verify(cursor).getStart();
            order.verify(engine).requiresFormatConversion(style);
            order.verify(dialogs).showWarningDialogAndWait(lang("Reference mark format"), lang("Converting references to selected format"));
            order.verify(engine).insert(List.of(entry), databases, style, CitationType.AUTHORYEAR_PAR, "", cursor, false);
        }
    }

    @Test
    void insertionStopsWhenFormatConversionQueryFails() {
        BibEntry entry = new BibEntry().withCitationKey("alpha");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        XTextViewCursor cursor = mock(XTextViewCursor.class);
        try (MockedConstruction<CitationEngine> engines = mockEngines();
             MockedStatic<UnoCursor> cursors = mockStatic(UnoCursor.class)) {
            cursors.when(() -> UnoCursor.getViewCursor(document)).thenReturn(Optional.of(cursor));
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();
            when(engine.requiresFormatConversion(style)).thenReturn(OOResult.error(new JabRefException("conversion query failed")));

            citationController.insertCitation(List.of(entry), databases, style, CitationType.AUTHORYEAR_PAR, "");

            verify(dialogs).showErrorDialogAndWait(lang("Could not insert citation"), "conversion query failed");
            verify(dialogs, never()).showWarningDialogAndWait(anyString(), anyString());
            verify(engine, never()).insert(anyList(), anyList(), any(), any(), anyString(), any(), anyBoolean());
        }
    }

    @Test
    void insertionWithoutEntriesDoesNotQueryFormatConversion() {
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();

            citationController.insertCitation(List.of(), List.of(new BibDatabase()), style, CitationType.AUTHORYEAR_PAR, "");

            verify(engine, never()).requiresFormatConversion(any());
            verify(engine, never()).insert(anyList(), anyList(), any(), any(), anyString(), any(), anyBoolean());
            verify(dialogs, never()).showWarningDialogAndWait(anyString(), anyString());
        }
    }

    @Test
    void insertionWithoutCursorDoesNotQueryFormatConversion() {
        BibEntry entry = new BibEntry().withCitationKey("alpha");
        try (MockedConstruction<CitationEngine> engines = mockEngines();
             MockedStatic<UnoCursor> cursors = mockStatic(UnoCursor.class)) {
            cursors.when(() -> UnoCursor.getViewCursor(document)).thenReturn(Optional.empty());
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();

            citationController.insertCitation(List.of(entry), List.of(new BibDatabase(List.of(entry))), style, CitationType.AUTHORYEAR_PAR, "");

            verify(engine, never()).requiresFormatConversion(any());
            verify(engine, never()).insert(anyList(), anyList(), any(), any(), anyString(), any(), anyBoolean());
            verify(dialogs, never()).showWarningDialogAndWait(anyString(), anyString());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void refreshDoesNotQueryOrWarnAboutFormatConversion(boolean zoteroMode) {
        when(preferences.getZoteroCompatibilityMode()).thenReturn(zoteroMode);
        List<BibDatabase> databases = List.of(new BibDatabase());
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();
            when(engine.requiresFormatConversion(style)).thenReturn(OOResult.ok(true));

            citationController.refresh(databases, style);

            verify(engine).refresh(databases, style);
            verify(engine, never()).requiresFormatConversion(any());
            verifyNoInteractions(dialogs);
        }
    }

    @Test
    void refreshWithoutCitedEntriesDoesNotLockControllersInGui() {
        List<BibDatabase> databases = List.of(new BibDatabase());
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.refresh(databases, style);

            verify(engines.constructed().getFirst()).refresh(databases, style);
            verify(document, never()).lockControllers();
            verify(document, never()).unlockControllers();
            verifyNoInteractions(dialogs);
        }
    }

    @Test
    void refreshFailureIsPresentedWithoutGuiDocumentMutation() {
        List<BibDatabase> databases = List.of(new BibDatabase());
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            when(engines.constructed().getFirst().refresh(databases, style)).thenReturn(OOResult.error(new JabRefException("refresh failed")));

            citationController.refresh(databases, style);

            verify(dialogs).showErrorDialogAndWait(lang("Unable to synchronize bibliography"), "refresh failed");
            verify(document, never()).lockControllers();
            verify(document, never()).unlockControllers();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"paragraph", "character"})
    void refreshDialogPreservesMissingStyleNameAndCorrectiveAction(String family) {
        String missingStyle = "Required research " + family + " style";
        RenderException cause = "character".equals(family)
                                ? MissingWriterStyleException.character(missingStyle) : MissingWriterStyleException.paragraph(missingStyle);
        String expected = "character".equals(family)
                          ? lang("You must create the following character style in LibreOffice: %0", missingStyle)
                          : lang("You must create the following paragraph style in LibreOffice: %0", missingStyle);
        List<BibDatabase> databases = List.of(new BibDatabase());
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            when(engines.constructed().getFirst().refresh(databases, style))
                    .thenReturn(OOResult.error(new JabRefException("Writer citation operation failed", cause.localizedMessage().orElseThrow(), cause)));

            citationController.refresh(databases, style);

            verify(dialogs).showErrorDialogAndWait(lang("Unable to synchronize bibliography"), expected);
            verify(document, never()).lockControllers();
            verify(document, never()).unlockControllers();
        }
    }

    @Test
    void refreshDialogPreservesBstCitationFormatCorrectiveAction() {
        MissingCitationLabelException cause = new MissingCitationLabelException("alpha");
        List<BibDatabase> databases = List.of(new BibDatabase());
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            when(engines.constructed().getFirst().refresh(databases, style))
                    .thenReturn(OOResult.error(new JabRefException("Writer citation operation failed", cause.localizedMessage().orElseThrow(), cause)));

            citationController.refresh(databases, style);

            verify(dialogs).showErrorDialogAndWait(lang("Unable to synchronize bibliography"),
                    lang("The selected BST style does not define any citation format for style-defined citations.\nPlease choose either numeric or author-year."));
        }
    }

    @Test
    void explicitRefreshReportsEveryUnresolvedKey() {
        List<BibDatabase> databases = List.of(new BibDatabase());
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            when(engines.constructed().getFirst().refresh(databases, style)).thenReturn(OOResult.ok(List.of("missing-alpha", "missing-beta")));

            citationController.refresh(databases, style);

            verify(dialogs).showErrorDialogAndWait(lang("Unable to synchronize bibliography"),
                    lang("Your OpenOffice/LibreOffice document references at least %0 citation keys which could not be found in your current library. Some of these are %1.",
                            "2", "missing-alpha, missing-beta"));
        }
    }

    @Test
    void groupEditsDelegateWithoutStyleFamilyChecks() {
        List<BibDatabase> databases = List.of(new BibDatabase());
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.mergeCitations(databases, style);
            citationController.separateCitations(databases, style);

            CitationEngine engine = engines.constructed().getFirst();
            verify(engine).merge(databases, style);
            verify(engine).separate(databases, style);
            verify(document, never()).lockControllers();
            verify(document, never()).unlockControllers();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void exportUsesEngineAndRetainsPartialResultPolicy(boolean returnPartial) {
        BibEntry entry = new BibEntry().withCitationKey("known");
        List<BibDatabase> databases = List.of(new BibDatabase(List.of(entry)));
        CitedEntryExporter.ExportResult exported = CitedEntryExporter.export(List.of("known", "missing"), databases);
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            when(engines.constructed().getFirst().exportCited(databases)).thenReturn(OOResult.ok(exported));

            Optional<BibDatabase> result = citationController.exportCited(databases, returnPartial);

            assertEquals(returnPartial ? Optional.of(exported.database()) : Optional.empty(), result);
            verify(engines.constructed().getFirst()).exportCited(databases);
            verify(dialogs).showErrorDialogAndWait(anyString(), anyString());
        }
    }

    @Test
    void manageCitationsUsesEngineForBothReadAndApply() {
        CitationEntry entry = new CitationEntry("canonical", "context", Optional.of("p. 5"));
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();
            when(engine.getCitationEntries(style)).thenReturn(OOResult.ok(List.of(entry)));
            when(engine.applyCitationEntries(List.of(entry), style)).thenReturn(OOVoidResult.ok());

            assertEquals(Optional.of(List.of(entry)), citationController.getCitationEntries());
            citationController.applyCitationEntries(List.of(entry));

            verify(engine).getCitationEntries(style);
            verify(engine).applyCitationEntries(List.of(entry), style);
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {CitationStyle.class, JStyle.class, BstStyle.class})
    void documentSelectionRestoresTheStoredStyleInsteadOfTheGlobalPreference(Class<? extends OOStyle> styleClass) throws WrappedTargetException {
        OOStyle storedStyle = mock(styleClass);
        OpenOfficeStyleCatalog catalog = mock(OpenOfficeStyleCatalog.class);
        when(catalog.restore(document, false)).thenReturn(Optional.of(storedStyle));

        assertEquals(Optional.of(storedStyle), controller().restoreStyleFromDocument(catalog).get());
        verify(preferences).setCurrentStyle(storedStyle);
        verify(preferences, never()).setCurrentStyle(style);
    }

    @Test
    void zoteroDocumentSelectionRestoresTheNativeStyle() throws WrappedTargetException {
        when(preferences.getZoteroCompatibilityMode()).thenReturn(true);
        CitationStyle storedStyle = mock(CitationStyle.class);
        OpenOfficeStyleCatalog catalog = mock(OpenOfficeStyleCatalog.class);
        when(catalog.restore(document, true)).thenReturn(Optional.of(storedStyle));

        assertEquals(Optional.of(storedStyle), controller().restoreStyleFromDocument(catalog).get());
        verify(preferences).setCurrentStyle(storedStyle);
    }

    @Test
    void cslInferenceSelectsOnlyTheCatalogCslResult() throws WrappedTargetException {
        CitationStyle inferred = mock(CitationStyle.class);
        OpenOfficeStyleCatalog catalog = mock(OpenOfficeStyleCatalog.class);
        when(catalog.inferCslStyle(document)).thenReturn(Optional.of(inferred));

        assertEquals(Optional.of(inferred), controller().inferCslStyleFromDocument(catalog).get());
        verify(preferences).setCurrentStyle(inferred);
        verify(catalog, never()).restore(any(), anyBoolean());
    }

    @Test
    void missingCslInferenceLeavesTheCurrentStyleUnchanged() throws WrappedTargetException {
        OpenOfficeStyleCatalog catalog = mock(OpenOfficeStyleCatalog.class);
        when(catalog.inferCslStyle(document)).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), controller().inferCslStyleFromDocument(catalog).get());
        verify(preferences, never()).setCurrentStyle(any());
    }

    @Test
    void cslInferenceFailureLeavesTheCurrentStyleUnchanged() throws WrappedTargetException {
        OpenOfficeStyleCatalog catalog = mock(OpenOfficeStyleCatalog.class);
        when(catalog.inferCslStyle(document)).thenThrow(new WrappedTargetException("document metadata unavailable"));

        assertEquals("document metadata unavailable", controller().inferCslStyleFromDocument(catalog).getError().getLocalizedMessage());
        verify(preferences, never()).setCurrentStyle(any());
    }

    @Test
    void documentSelectionDoesNotConstructEngineBeforeDocumentStyleRestoration() throws Exception {
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            controller().selectDocument(true);

            assertEquals(List.of(), engines.constructed());
            verify(dialogs).notify(lang("Connected to document: %0", "document"));
        }
    }

    @Test
    void switchingDocumentsClosesThePreviousEngineAndDisposeIsIdempotent() {
        XTextDocument nextDocument = mock(XTextDocument.class);
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            when(connection.getXTextDocument()).thenReturn(OOResult.ok(nextDocument));
            citationController.capabilities(style);
            citationController.dispose();
            citationController.dispose();

            assertEquals(2, engines.constructed().size());
            engines.constructed().forEach(engine -> verify(engine).close());
        }
    }

    @Test
    void connectionLossClosesTheEngineAndDoesNotRunAnotherOperation() {
        try (MockedConstruction<CitationEngine> engines = mockEngines()) {
            OpenOfficeCitationController citationController = controller();
            citationController.capabilities(style);
            CitationEngine engine = engines.constructed().getFirst();
            when(connection.getXTextDocument()).thenReturn(OOResult.error(OOError.from(new NoDocumentException())));

            citationController.refresh(List.of(new BibDatabase()), style);

            verify(engine).close();
            verify(engine, never()).refresh(anyList(), any());
            assertEquals(1, engines.constructed().size());
        }
    }

    private OpenOfficeCitationController controller() {
        return new OpenOfficeCitationController(connection, dialogs, preferences, entryTypes);
    }

    private MockedConstruction<CitationEngine> mockEngines() {
        return mockConstruction(CitationEngine.class, (engine, _) -> {
            when(engine.capabilities(any())).thenReturn(new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR), true, false, false));
            when(engine.requiresFormatConversion(any())).thenReturn(OOResult.ok(false));
            when(engine.refresh(anyList(), any())).thenReturn(OOResult.ok(List.of()));
            when(engine.merge(anyList(), any())).thenReturn(OOResult.ok(List.of()));
            when(engine.separate(anyList(), any())).thenReturn(OOResult.ok(List.of()));
        });
    }
}
