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
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.engine.StyleCapabilities;
import org.jabref.logic.openoffice.style.BstStyleLoader;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.JStyleLoader;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;

import static org.jabref.logic.l10n.Localization.lang;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        private final OpenOfficePreferences preferences = mock(OpenOfficePreferences.class);
        private final OpenOfficeStyleCatalog styles = mock(OpenOfficeStyleCatalog.class);
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
            when(styles.reload(any())).thenAnswer(invocation -> invocation.getArgument(0, OOStyle.class));
            when(stateManager.getActiveDatabase()).thenReturn(Optional.of(new BibDatabaseContext(database)));
            when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList(entry));
            when(citationController.capabilities(any())).thenReturn(new StyleCapabilities(EnumSet.allOf(CitationType.class), true, true, true));
            when(citationController.restoreStyleFromDocument(styles)).thenReturn(OOResult.ok(Optional.empty()));

            for (Field field : OpenOfficePanel.class.getDeclaredFields()) {
                if (field.getType() == Button.class) {
                    field.setAccessible(true);
                    field.set(panel, new Button());
                }
            }
            setField("dialogService", dialogs);
            setField("openOfficePreferences", preferences);
            setField("styleCatalog", styles);
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
        void stylePreparationReturnsTrueAndUsesTheReloadedStyle() throws ReflectiveOperationException, IOException {
            JStyle reloaded = mock(JStyle.class);
            when(preferences.getCurrentStyle()).thenReturn(selectedStyle);
            when(styles.reload(selectedStyle)).thenReturn(reloaded);

            JavaFxExtension.invokeAndWait(() -> assertEquals(Boolean.TRUE,
                    assertDoesNotThrow(() -> invoke("getOrUpdateTheStyle", "Prepare style"))));

            assertEquals(reloaded, field("currentStyle"));
            verify(preferences).setCurrentStyle(reloaded);
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

        @Test
        void inferenceButtonSelectsOnlyTheCslResult() throws ReflectiveOperationException {
            CitationStyle inferred = mock(CitationStyle.class);
            when(inferred.getName()).thenReturn("External CSL style");
            when(citationController.inferCslStyleFromDocument(styles)).thenReturn(OOResult.ok(Optional.of(inferred)));
            Button infer = button("inferCslStyle");

            JavaFxExtension.invokeAndWait(infer::fire);

            assertEquals(lang("Infer CSL style from document"), infer.getTooltip().getText());
            assertEquals(inferred, field("currentStyle"));
            verify(citationController).inferCslStyleFromDocument(styles);
            verify(citationController, never()).restoreStyleFromDocument(any());
            verify(dialogs).notify(lang("Inferred CSL style: '%0'", "External CSL style"));
        }

        @Test
        void missingCslInferenceKeepsTheSelectedStyle() throws ReflectiveOperationException {
            when(citationController.inferCslStyleFromDocument(styles)).thenReturn(OOResult.ok(Optional.empty()));
            Button infer = button("inferCslStyle");

            JavaFxExtension.invokeAndWait(infer::fire);

            assertEquals(previousStyle, field("currentStyle"));
            verify(dialogs).notify(lang("No CSL style could be inferred from the document."));
        }

        @ParameterizedTest
        @CsvSource({"false, false, true", "false, true, true", "true, false, false", "true, true, true"})
        void documentSelectionRestoresStyleSeparatelyAndRespectsNativeInferencePreference(boolean zoteroMode, boolean inferOnConnect, boolean restores) throws ReflectiveOperationException {
            when(preferences.getZoteroCompatibilityMode()).thenReturn(zoteroMode);
            when(preferences.shouldInferCslStyleFromDocument()).thenReturn(inferOnConnect);
            Button selectDocument = button("selectDocument");

            JavaFxExtension.invokeAndWait(selectDocument::fire);

            if (restores) {
                verify(citationController).restoreStyleFromDocument(styles);
            } else {
                verify(citationController, never()).restoreStyleFromDocument(any());
            }
            verify(citationController, never()).inferCslStyleFromDocument(any());
        }

        private void prepareScenario(String scenario) throws IOException {
            if (!"missing".equals(scenario)) {
                when(preferences.getCurrentStyle()).thenReturn(selectedStyle);
            }
            if ("reload-failed".equals(scenario)) {
                when(styles.reload(selectedStyle)).thenThrow(new IOException("unavailable"));
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

        private void initializePanel() throws ReflectiveOperationException {
            Method method = OpenOfficePanel.class.getDeclaredMethod("initPanel");
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
