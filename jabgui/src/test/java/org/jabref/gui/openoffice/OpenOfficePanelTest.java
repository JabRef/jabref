package org.jabref.gui.openoffice;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.PreferencesDialogView;
import org.jabref.gui.preferences.openoffice.OpenOfficeTab;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.bst.BstStyle;
import org.jabref.logic.openoffice.bst.BstStyleLoader;
import org.jabref.logic.openoffice.engine.StyleCapabilities;
import org.jabref.logic.openoffice.jstyle.JStyle;
import org.jabref.logic.openoffice.jstyle.JStyleLoader;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;

import static org.jabref.logic.l10n.Localization.lang;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
class OpenOfficePanelTest {

    @ParameterizedTest
    @MethodSource
    void buttonsFollowEngineCapabilities(StyleCapabilities capabilities, OpenOfficeActionAvailability expected) {
        assertEquals(expected, OpenOfficeActionAvailability.forCapabilities(true, true, capabilities));
    }

    static Stream<Arguments> buttonsFollowEngineCapabilities() {
        return Stream.of(
                Arguments.of(
                        new StyleCapabilities(EnumSet.allOf(CitationType.class), true, true, true),
                        new OpenOfficeActionAvailability(true, true, true, true, true, true, true, true)),
                Arguments.of(
                        new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.INVISIBLE_CIT), true, true, true),
                        new OpenOfficeActionAvailability(true, true, true, false, true, true, true, true)),
                Arguments.of(
                        new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.INVISIBLE_CIT,
                                CitationType.AUTHOR_ONLY, CitationType.YEAR_ONLY), true, false, false),
                        new OpenOfficeActionAvailability(true, true, true, true, true, true, false, false)),
                Arguments.of(
                        new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.INVISIBLE_CIT), true, false, false),
                        new OpenOfficeActionAvailability(true, true, true, false, true, true, false, false)),
                Arguments.of(
                        new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT), true, false, false),
                        new OpenOfficeActionAvailability(true, true, false, false, true, true, false, false)),
                Arguments.of(
                        new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR), true, false, false),
                        new OpenOfficeActionAvailability(true, true, false, false, true, true, false, false)),
                Arguments.of(
                        new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.INVISIBLE_CIT), false, false, false),
                        new OpenOfficeActionAvailability(true, true, true, false, true, false, false, false)),
                Arguments.of(
                        new StyleCapabilities(Set.of(), false, false, false),
                        new OpenOfficeActionAvailability(false, false, false, false, false, false, false, false)));
    }

    @ParameterizedTest
    @EnumSource(value = CitationType.class, names = {"AUTHOR_ONLY", "YEAR_ONLY"})
    void eitherPartialCitationEnablesCiteSpecialWithoutPageInfo(CitationType citationType) {
        StyleCapabilities capabilities = new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR, citationType), true, false, false);

        assertEquals(new OpenOfficeActionAvailability(true, true, false, true, true, true, false, false),
                OpenOfficeActionAvailability.forCapabilities(true, true, capabilities));
    }

    @Test
    void disconnectedDocumentDisablesEveryDocumentAction() {
        StyleCapabilities capabilities = new StyleCapabilities(EnumSet.allOf(CitationType.class), true, true, true);

        assertEquals(new OpenOfficeActionAvailability(false, false, false, false, false, false, false, false),
                OpenOfficeActionAvailability.forCapabilities(false, true, capabilities));
    }

    @Test
    void missingLibraryDisablesCitingButPreservesSupportedGroupEditing() {
        StyleCapabilities capabilities = new StyleCapabilities(EnumSet.allOf(CitationType.class), true, true, true);

        assertEquals(new OpenOfficeActionAvailability(false, false, false, false, false, false, true, true),
                OpenOfficeActionAvailability.forCapabilities(true, false, capabilities));
    }

    @Nested
    @NullMarked
    @ExtendWith(JavaFxExtension.class)
    @ResourceLock("Localization.lang")
    class DocumentActions {

        private final OpenOfficePanel panel = mock(OpenOfficePanel.class, CALLS_REAL_METHODS);
        private final DialogService dialogs = mock(DialogService.class);
        private final LibraryTabContainer tabContainer = mock(LibraryTabContainer.class);
        private final LibraryTab targetTab = mock(LibraryTab.class);
        private final OpenOfficePreferences preferences = mock(OpenOfficePreferences.class);
        private final CslStyleResolver resolver = mock(CslStyleResolver.class);
        private final OpenOfficeCitationController citationController = mock(OpenOfficeCitationController.class);
        private final StateManager stateManager = mock(StateManager.class);
        private final JStyle previousStyle = mock(JStyle.class);
        private final JStyle selectedStyle = mock(JStyle.class);
        private final BibEntry entry = new BibEntry().withCitationKey("alpha");
        private final BibDatabase database = new BibDatabase(List.of(entry));

        @BeforeEach
        void installRealActionHandlersWithMockedServices() throws ReflectiveOperationException, IOException {
            when(preferences.zoteroCompatibilityModeProperty()).thenReturn(new SimpleBooleanProperty());
            when(selectedStyle.getPath()).thenReturn("selected.jstyle");
            when(selectedStyle.getName()).thenReturn("Selected style");
            when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext(database)));
            when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entry));
            when(citationController.capabilities(any())).thenReturn(new StyleCapabilities(EnumSet.allOf(CitationType.class), true, true, true));

            for (Field field : OpenOfficePanel.class.getDeclaredFields()) {
                if (field.getType() == Button.class) {
                    field.setAccessible(true);
                    field.set(panel, new Button());
                }
            }
            setField("dialogService", dialogs);
            setField("tabContainer", tabContainer);
            setField("openOfficePreferences", preferences);
            setField("cslStyleResolver", resolver);
            setField("citationController", citationController);
            setField("stateManager", stateManager);
            setField("currentStyle", previousStyle);
            setField("vbox", new VBox());
            setField("pageInfoSupported", new SimpleBooleanProperty());
            setField("jStyleLoader", mock(JStyleLoader.class));
            setField("cslStyleLoader", mock(CSLStyleLoader.class));
            setField("bstStyleLoader", mock(BstStyleLoader.class));
            setField("journalAbbreviationRepository", mock(JournalAbbreviationRepository.class));
            JavaFxExtension.invokeAndWait(() -> assertDoesNotThrow(this::initializePanel));
        }

        @Test
        void importWithoutLibraryStopsBeforeReadingTheDocument() {
            JavaFxExtension.invokeAndWait(() -> assertDoesNotThrow(() -> invoke("importEntries")));

            verify(dialogs).notify(lang("No library open"));
            verify(citationController, never()).importCited();
            verifyNoInteractions(targetTab);
        }

        @Test
        void failedCitationCollectionDoesNotStartLibraryImport() {
            when(tabContainer.getCurrentLibraryTab()).thenReturn(targetTab);
            when(citationController.importCited()).thenReturn(Optional.empty());

            JavaFxExtension.invokeAndWait(() -> assertDoesNotThrow(() -> invoke("importEntries")));

            verify(citationController).importCited();
            verifyNoInteractions(targetTab);
            verify(dialogs, never()).notify(anyString());
        }

        @Test
        void emptyCitationCollectionReportsNothingToImport() {
            when(tabContainer.getCurrentLibraryTab()).thenReturn(targetTab);
            when(citationController.importCited()).thenReturn(Optional.of(List.of()));

            JavaFxExtension.invokeAndWait(() -> assertDoesNotThrow(() -> invoke("importEntries")));

            verify(dialogs).notify(lang("No new cited entries to import"));
            verifyNoInteractions(targetTab);
        }

        @Test
        void importActionForwardsCitationsToTheCapturedLibraryWithoutFilteringByKey() throws ReflectiveOperationException {
            setField("currentStyle", mock(CitationStyle.class));
            when(preferences.getZoteroCompatibilityMode()).thenReturn(true);
            when(tabContainer.getCurrentLibraryTab()).thenReturn(targetTab);
            when(citationController.importCited()).thenReturn(Optional.of(List.of(entry)));
            Button importCitations = button("importCitations");

            JavaFxExtension.invokeAndWait(() -> {
                assertDoesNotThrow(this::refreshActionAvailability);
                assertFalse(importCitations.isDisabled());
                importCitations.fire();
            });

            verify(targetTab).importCitedEntriesWithDuplicateCheck(List.of(entry));
            verify(targetTab, never()).getDatabase();
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void importActionRequiresZoteroCompatibilityMode(boolean zoteroMode) throws ReflectiveOperationException {
            setField("currentStyle", mock(CitationStyle.class));
            when(preferences.getZoteroCompatibilityMode()).thenReturn(zoteroMode);
            Button importCitations = button("importCitations");

            JavaFxExtension.invokeAndWait(() -> {
                assertDoesNotThrow(this::refreshActionAvailability);
                assertEquals(!zoteroMode, importCitations.isDisabled());
            });
        }

        @ParameterizedTest
        @ValueSource(classes = {JStyle.class, BstStyle.class})
        void importActionIsDisabledForNonCslStylesWithoutClearingTheSavedMode(Class<? extends OOStyle> styleClass) throws ReflectiveOperationException {
            setField("currentStyle", mock(styleClass));
            when(preferences.getZoteroCompatibilityMode()).thenReturn(true);
            Button importCitations = button("importCitations");

            JavaFxExtension.invokeAndWait(() -> {
                assertDoesNotThrow(this::refreshActionAvailability);
                assertTrue(importCitations.isDisabled());
            });

            verify(preferences, never()).setZoteroCompatibilityMode(anyBoolean());
        }

        @Test
        void importActionIsDisabledWithoutDocumentConnection() throws ReflectiveOperationException {
            when(preferences.getZoteroCompatibilityMode()).thenReturn(true);
            when(citationController.isDocumentConnectionMissing()).thenReturn(true);
            Button importCitations = button("importCitations");

            JavaFxExtension.invokeAndWait(() -> {
                assertDoesNotThrow(this::refreshActionAvailability);
                assertTrue(importCitations.isDisabled());
                importCitations.fire();
            });

            verify(citationController, never()).importCited();
        }

        @Test
            // [utest->req~openoffice.preferences-shortcut~1]
        void settingsMenuOpensLibreOfficePreferencesWithoutAWriterConnection() throws ReflectiveOperationException {
            when(citationController.isDocumentConnectionMissing()).thenReturn(true);
            Button settings = button("settingsB");
            Method updateAvailability = OpenOfficePanel.class.getDeclaredMethod("updateButtonAvailability");
            updateAvailability.setAccessible(true);

            JavaFxExtension.invokeAndWait(() -> {
                assertDoesNotThrow(() -> updateAvailability.invoke(panel));
                assertFalse(settings.isDisabled());
                try (MockedConstruction<PreferencesDialogView> views = mockConstruction(PreferencesDialogView.class, (_, context) ->
                        assertEquals(List.of(OpenOfficeTab.class), context.arguments()))) {
                    MenuItem moreSettings = settings.getContextMenu().getItems().stream()
                                                    .filter(item -> lang("More settings").equals(item.getText())).findFirst().orElseThrow();
                    assertFalse(moreSettings.isDisable());

                    moreSettings.fire();

                    assertEquals(1, views.constructed().size());
                    verify(dialogs).showCustomDialogAndWait(views.constructed().getFirst());
                }
            });
            verify(tabContainer).refresh();
        }

        @Test
        void stylePreparationRefreshesTheSelectedJstyleWithoutReplacingIt() throws ReflectiveOperationException, IOException {
            when(preferences.getCurrentStyle()).thenReturn(selectedStyle);

            JavaFxExtension.invokeAndWait(() -> assertEquals(Boolean.TRUE,
                    assertDoesNotThrow(() -> invoke("getOrUpdateTheStyle", "Prepare style"))));

            assertSame(selectedStyle, field("currentStyle"));
            verify(selectedStyle).ensureUpToDate();
            verify(preferences, never()).setCurrentStyle(any());
            verify(previousStyle, never()).ensureUpToDate();
            verifyNoInteractions(resolver, field("jStyleLoader"));
        }

        @ParameterizedTest
        @ValueSource(classes = {CitationStyle.class, BstStyle.class})
        void stylePreparationKeepsCslAndBstStylesWithoutJstyleReloading(Class<? extends OOStyle> styleClass) throws ReflectiveOperationException, IOException {
            OOStyle selected = mock(styleClass);
            when(preferences.getCurrentStyle()).thenReturn(selected);

            JavaFxExtension.invokeAndWait(() -> assertEquals(Boolean.TRUE,
                    assertDoesNotThrow(() -> invoke("getOrUpdateTheStyle", "Prepare style"))));

            assertSame(selected, field("currentStyle"));
            verify(previousStyle, never()).ensureUpToDate();
            verify(selectedStyle, never()).ensureUpToDate();
            verify(preferences, never()).setCurrentStyle(any());
            verifyNoInteractions(resolver, field("jStyleLoader"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"missing", "reload-failed"})
        void stylePreparationReturnsFalseAndKeepsTheCurrentStyleOnFailure(String failure) throws ReflectiveOperationException, IOException {
            prepareScenario(failure);

            JavaFxExtension.invokeAndWait(() -> assertEquals(Boolean.FALSE,
                    assertDoesNotThrow(() -> invoke("getOrUpdateTheStyle", "Prepare style"))));

            assertEquals(previousStyle, field("currentStyle"));
            verify(dialogs).showErrorDialogAndWait(anyString(), anyString());
        }

        @ParameterizedTest
        @ValueSource(booleans = {false, true})
        void styleSelectionNotifiesOnlyAfterSuccessfulPreparation(boolean succeeds) throws ReflectiveOperationException, IOException {
            prepareScenario(succeeds ? "ready" : "reload-failed");
            when(dialogs.<OOStyle>showCustomDialogAndWait(any())).thenReturn(Optional.of(selectedStyle));
            Button selectStyle = button("setStyleFile");

            JavaFxExtension.invokeAndWait(() -> {
                try (MockedConstruction<StyleSelectDialogView> views = mockConstruction(StyleSelectDialogView.class)) {
                    selectStyle.fire();
                    assertEquals(1, views.constructed().size());
                }
            });

            verify(preferences).setCurrentStyle(selectedStyle);
            if (succeeds) {
                verify(dialogs).notify(lang("Currently selected style: '%0'", "Selected style"));
                assertEquals(selectedStyle, field("currentStyle"));
            } else {
                verify(dialogs, never()).notify(anyString());
                verify(dialogs).showErrorDialogAndWait(lang("Problem modifying citation"),
                        lang("Unable to reload style file '%0'. %1", "selected.jstyle", "unavailable"));
                assertEquals(previousStyle, field("currentStyle"));
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"ready", "missing", "reload-failed"})
        void refreshRunsOnlyAfterSuccessfulStylePreparation(String scenario) throws ReflectiveOperationException, IOException {
            prepareScenario(scenario);
            Button refresh = button("updateBibliography");

            JavaFxExtension.invokeAndWait(refresh::fire);

            if ("ready".equals(scenario)) {
                verify(citationController).refresh(List.of(database), selectedStyle);
                assertEquals(selectedStyle, field("currentStyle"));
            } else {
                verify(citationController, never()).refresh(anyList(), any());
                verify(dialogs).showErrorDialogAndWait(anyString(), anyString());
                assertEquals(previousStyle, field("currentStyle"));
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {"ready", "missing", "reload-failed"})
        void citationRunsOnlyAfterSuccessfulStylePreparation(String scenario) throws ReflectiveOperationException, IOException {
            prepareScenario(scenario);
            Button cite = button("cite");

            JavaFxExtension.invokeAndWait(cite::fire);

            if ("ready".equals(scenario)) {
                verify(citationController).insertCitation(List.of(entry), List.of(database), selectedStyle, CitationType.AUTHORYEAR_PAR, "");
                assertEquals(selectedStyle, field("currentStyle"));
            } else {
                verify(citationController, never()).insertCitation(anyList(), anyList(), any(), any(), anyString());
                verify(dialogs).showErrorDialogAndWait(anyString(), anyString());
                assertEquals(previousStyle, field("currentStyle"));
            }
        }

        @ParameterizedTest
        @EnumSource(value = CitationType.class, names = {"AUTHOR_ONLY", "YEAR_ONLY"})
        void citeSpecialUsesCurrentCapabilitiesAndInsertsSelectedPartialType(CitationType citationType) throws ReflectiveOperationException, IOException {
            prepareScenario("ready");
            StyleCapabilities capabilities = new StyleCapabilities(
                    Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT, CitationType.INVISIBLE_CIT,
                            CitationType.AUTHOR_ONLY, CitationType.YEAR_ONLY), true, false, false);
            when(citationController.capabilities(selectedStyle)).thenReturn(capabilities);
            when(preferences.getCiteSpecialCitationType()).thenReturn(CitationType.AUTHORYEAR_NOPAR);
            when(dialogs.showCustomDialogAndWait(any(CiteSpecialDialogView.class))).thenAnswer(invocation -> {
                CiteSpecialDialogView dialog = invocation.getArgument(0);
                RadioButton inPar = JavaFxExtension.lookup(dialog.getDialogPane(), "#inPar", RadioButton.class);
                RadioButton noPar = JavaFxExtension.lookup(dialog.getDialogPane(), "#noPar", RadioButton.class);
                assertTrue(inPar.isSelected());
                assertTrue(noPar.isDisabled());
                RadioButton option = JavaFxExtension.lookup(dialog.getDialogPane(), ".radio-button", RadioButton.class,
                        button -> button.getUserData() == citationType);
                option.fire();
                return Optional.of(dialog.getResultConverter().call(ButtonType.OK));
            });
            Button selectDocument = button("selectDocument");
            Button citeSpecial = button("citeSpecial");

            JavaFxExtension.invokeAndWait(() -> {
                selectDocument.fire();
                assertFalse(citeSpecial.isDisabled());
                citeSpecial.fire();
            });

            verify(citationController).insertCitation(List.of(entry), List.of(database), selectedStyle, citationType, "");
            verify(preferences).setCiteSpecialCitationType(citationType);
        }

        @Test
        void citeSpecialAndEmptyButtonsRemainDisabledForNativeCapabilities() throws ReflectiveOperationException {
            when(preferences.getCurrentStyle()).thenReturn(selectedStyle);
            when(citationController.capabilities(selectedStyle)).thenReturn(new StyleCapabilities(
                    Set.of(CitationType.AUTHORYEAR_PAR, CitationType.AUTHORYEAR_INTEXT), true, false, false));
            Button selectDocument = button("selectDocument");
            Button citeSpecial = button("citeSpecial");
            Button citeEmpty = button("citeEmpty");

            JavaFxExtension.invokeAndWait(() -> {
                selectDocument.fire();
                assertTrue(citeSpecial.isDisabled());
                assertTrue(citeEmpty.isDisabled());
                citeSpecial.fire();
                citeEmpty.fire();
            });

            verify(dialogs, never()).showCustomDialogAndWait(any(CiteSpecialDialogView.class));
            verify(citationController, never()).insertCitation(anyList(), anyList(), any(), any(), anyString());
        }

        @ParameterizedTest
        @ValueSource(classes = {JStyle.class, BstStyle.class})
        void inferenceButtonIsDisabledForNonCslStyles(Class<? extends OOStyle> styleClass) throws ReflectiveOperationException {
            setField("currentStyle", mock(styleClass));
            Button infer = button("inferCslStyle");

            JavaFxExtension.invokeAndWait(() -> {
                assertDoesNotThrow(this::refreshActionAvailability);
                assertTrue(infer.isDisabled());
                infer.fire();
            });

            verify(citationController, never()).inferCslStyleFromDocument(any());
        }

        @Test
        void inferenceButtonIsDisabledWithoutAWriterConnection() throws ReflectiveOperationException {
            setField("currentStyle", mock(CitationStyle.class));
            when(citationController.isDocumentConnectionMissing()).thenReturn(true);
            Button infer = button("inferCslStyle");

            JavaFxExtension.invokeAndWait(() -> {
                assertDoesNotThrow(this::refreshActionAvailability);
                assertTrue(infer.isDisabled());
                infer.fire();
            });

            verify(citationController, never()).inferCslStyleFromDocument(any());
        }

        @Test
        void inferenceButtonSelectsOnlyTheCslResult() throws ReflectiveOperationException {
            setField("currentStyle", mock(CitationStyle.class));
            CitationStyle inferred = mock(CitationStyle.class);
            when(inferred.getName()).thenReturn("External CSL style");
            when(citationController.inferCslStyleFromDocument(resolver)).thenReturn(OOResult.ok(Optional.of(inferred)));
            Button infer = button("inferCslStyle");

            JavaFxExtension.invokeAndWait(() -> {
                assertDoesNotThrow(this::refreshActionAvailability);
                assertFalse(infer.isDisabled());
                infer.fire();
            });

            assertEquals(lang("Infer CSL style from document"), infer.getTooltip().getText());
            assertEquals(inferred, field("currentStyle"));
            verify(citationController).inferCslStyleFromDocument(resolver);
            verify(dialogs).notify(lang("Inferred CSL style: '%0'", "External CSL style"));
        }

        @Test
        void missingCslInferenceKeepsTheSelectedStyle() throws ReflectiveOperationException {
            CitationStyle selectedCslStyle = mock(CitationStyle.class);
            setField("currentStyle", selectedCslStyle);
            when(citationController.inferCslStyleFromDocument(resolver)).thenReturn(OOResult.ok(Optional.empty()));
            Button infer = button("inferCslStyle");

            JavaFxExtension.invokeAndWait(() -> {
                assertDoesNotThrow(this::refreshActionAvailability);
                assertFalse(infer.isDisabled());
                infer.fire();
            });

            assertEquals(selectedCslStyle, field("currentStyle"));
            verify(dialogs).notify(lang("No CSL style could be inferred from the document."));
        }

        @ParameterizedTest
        @ValueSource(classes = {CitationStyle.class, JStyle.class, BstStyle.class})
        void documentSelectionKeepsTheLastSelectedStyleWithoutReadingDocumentStyle(Class<? extends OOStyle> styleClass) throws ReflectiveOperationException {
            OOStyle selected = mock(styleClass);
            when(preferences.getCurrentStyle()).thenReturn(selected);
            Button selectDocument = button("selectDocument");

            JavaFxExtension.invokeAndWait(() -> {
                selectDocument.fire();
                selectDocument.fire();
            });

            assertEquals(selected, field("currentStyle"));
            verify(preferences, never()).setCurrentStyle(any());
            verify(citationController, never()).inferCslStyleFromDocument(any());
            verifyNoInteractions(resolver);
        }

        @Test
        void documentSelectionWithoutAStyleRemainsSafeAndAllowsSelectingOne() throws ReflectiveOperationException {
            Button selectDocument = button("selectDocument");
            Button selectStyle = button("setStyleFile");
            List<Button> styleActions = List.of(button("cite"), button("citeInText"), button("citeEmpty"), button("citeSpecial"),
                    button("updateBibliography"), button("merge"), button("unmerge"), button("manageCitations"), button("modifyBibliographyProperties"));
            clearInvocations(citationController);

            JavaFxExtension.invokeAndWait(() -> {
                selectDocument.fire();
                assertFalse(selectStyle.isDisabled());
                styleActions.forEach(action -> assertTrue(action.isDisabled()));
            });

            assertNull(field("currentStyle"));
            verify(preferences, never()).setCurrentStyle(any());
            verify(citationController, never()).capabilities(any());
            verify(citationController, never()).inferCslStyleFromDocument(any());
            verifyNoInteractions(resolver);
        }

        private void prepareScenario(String scenario) throws IOException {
            if (!"missing".equals(scenario)) {
                when(preferences.getCurrentStyle()).thenReturn(selectedStyle);
            }
            if ("reload-failed".equals(scenario)) {
                doThrow(new IOException("unavailable")).when(selectedStyle).ensureUpToDate();
            }
        }

        private void setField(String name, Object value) throws ReflectiveOperationException {
            Field field = OpenOfficePanel.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(panel, value);
        }

        private Object field(String name) throws ReflectiveOperationException {
            Field field = OpenOfficePanel.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(panel);
        }

        private Button button(String name) throws ReflectiveOperationException {
            return (Button) field(name);
        }

        private void refreshActionAvailability() throws ReflectiveOperationException {
            Method method = OpenOfficePanel.class.getDeclaredMethod("updateButtonAvailability");
            method.setAccessible(true);
            method.invoke(panel);
        }

        private void initializePanel() throws ReflectiveOperationException {
            Method method = OpenOfficePanel.class.getDeclaredMethod("initPanel");
            method.setAccessible(true);
            method.invoke(panel);
        }

        private void invoke(String name) throws ReflectiveOperationException {
            Method method = OpenOfficePanel.class.getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(panel);
        }

        private Object invoke(String name, String title) throws ReflectiveOperationException {
            Method method = OpenOfficePanel.class.getDeclaredMethod(name, String.class);
            method.setAccessible(true);
            return method.invoke(panel, title);
        }
    }
}
