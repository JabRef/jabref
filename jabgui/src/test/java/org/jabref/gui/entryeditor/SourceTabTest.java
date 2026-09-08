package org.jabref.gui.entryeditor;

import java.util.List;
import java.util.Optional;

import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.search.SearchType;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.support.DisabledOnCIServer;

import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import jfx.incubator.scene.control.richtext.CodeArea;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
class SourceTabTest {

    private Stage stage;
    private Scene scene;
    private CodeArea area;
    private TabPane pane;
    private SourceTab sourceTab;
    private FieldPreferences fieldPreferences;
    private BibEntryTypesManager entryTypesManager;
    private KeyBindingRepository keyBindingRepository;
    private OptionalObjectProperty<BibDatabaseContext> activeDatabase;
    private StateManager stateManager;

    @BeforeEach
    void setUp() {
        JavaFxExtension.invokeAndWait(() -> {
            Stage stage = new Stage();
            area = new CodeArea();
            area.appendText("some example\n text to go here\n across a couple of \n lines....");
            stateManager = mock(StateManager.class);
            when(stateManager.activeSearchQuery(SearchType.NORMAL_SEARCH)).thenReturn(OptionalObjectProperty.empty());
            when(stateManager.searchQueryProperty()).thenReturn(mock(StringProperty.class));
            activeDatabase = OptionalObjectProperty.empty();
            when(stateManager.activeDatabaseProperty()).thenReturn(activeDatabase);
            when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
            keyBindingRepository = new KeyBindingRepository(List.of(), List.of());
            keyBindingRepository.put(KeyBinding.SAVE_LIBRARY, "Ctrl+S");
            ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
            when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
            fieldPreferences = mock(FieldPreferences.class);
            when(fieldPreferences.getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
            entryTypesManager = mock(BibEntryTypesManager.class);

            sourceTab = new SourceTab(

                    fieldPreferences,
                    importFormatPreferences,
                    new DummyFileUpdateMonitor(),
                    mock(DialogService.class),
                    entryTypesManager,
                    keyBindingRepository,
                    stateManager,
                    new BibTeXSyntaxHighlighter()
            );
            pane = new TabPane(
                    new Tab("main area", area),
                    new Tab("other tab", new Label("some text")),
                    sourceTab
            );
            scene = new Scene(pane);
            this.stage = stage;

            stage.setScene(scene);
            stage.setWidth(400);
            stage.setHeight(400);
            stage.show();

            // select the area's tab
            pane.getSelectionModel().select(0);
        });
    }

    @ParameterizedTest
    @EnumSource(BibDatabaseMode.class)
    void sourceLabelUpdatesWhenStartupDatabaseBecomesAvailable(BibDatabaseMode mode) {
        BibDatabaseContext database = new BibDatabaseContext();
        database.setMode(mode);

        JavaFxExtension.invokeAndWait(() -> {
            activeDatabase.set(Optional.of(database));

            assertEquals(Localization.lang("%0 source", mode.getFormattedName()), sourceTab.getText());
            assertEquals(Localization.lang("Show/edit %0 source", mode.getFormattedName()), sourceTab.getTooltip().getText());
        });
    }

    @Test
    void sourceLabelResetsWhenDatabaseCloses() {
        JavaFxExtension.invokeAndWait(() -> {
            activeDatabase.set(Optional.of(new BibDatabaseContext()));
            activeDatabase.set(Optional.empty());

            assertEquals(Localization.lang("Source"), sourceTab.getText());
            assertEquals(Localization.lang("Show/edit source"), sourceTab.getTooltip().getText());
        });
    }

    @Test
    void switchingFromSourceTabDoesNotThrowException() {
        BibEntry entry = new BibEntry();
        entry.setField(new UnknownField("test"), "testvalue");

        // Update source editor. In production currentEntry is bound to the view model; here we drive the
        // property directly, since notifyAboutFocus no longer sets it.
        JavaFxExtension.invokeAndWait(() -> pane.getSelectionModel().select(2));
        JavaFxExtension.invokeAndWait(() -> {
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        JavaFxExtension.awaitEvents();

        // Switch to different tab & update entry
        JavaFxExtension.invokeAndWait(() -> pane.getSelectionModel().select(1));
        JavaFxExtension.invokeAndWait(() -> stage.setWidth(600));
        JavaFxExtension.invokeAndWait(() -> entry.setField(new UnknownField("test"), "new value"));

        // No exception should be thrown
        JavaFxExtension.awaitEvents();
    }

    @Test
    void replacingLongSourceWithShortSourceDoesNotThrowException() {
        BibEntry longEntry = new BibEntry()
                .withField(new UnknownField("author"), "Author")
                .withField(new UnknownField("title"), "Title")
                .withField(new UnknownField("year"), "2026")
                .withField(new UnknownField("publisher"), "Publisher");
        BibEntry shortEntry = new BibEntry().withField(new UnknownField("title"), "Short title");

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(longEntry);
            sourceTab.notifyAboutFocus(longEntry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            sourceTab.currentEntryProperty().set(shortEntry);
            sourceTab.notifyAboutFocus(shortEntry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea sourceArea = (CodeArea) sourceTab.getContent();
            assertTrue(sourceArea.getText().contains("title = {Short title}"));
            assertFalse(sourceArea.getText().contains("publisher = {Publisher}"));
        });
    }

    @Test
    void updatingPreviouslyBoundEntryDoesNotResetCurrentSource() {
        BibEntry firstEntry = new BibEntry().withField(new UnknownField("title"), "First entry");
        BibEntry secondEntry = new BibEntry().withField(new UnknownField("title"), "Second entry");

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(firstEntry);
            sourceTab.notifyAboutFocus(firstEntry);

            sourceTab.currentEntryProperty().set(secondEntry);
            sourceTab.notifyAboutFocus(secondEntry);

            CodeArea sourceArea = (CodeArea) sourceTab.getContent();
            sourceArea.clear();
            sourceArea.appendText("Unsaved source for the second entry");

            firstEntry.setField(new UnknownField("author"), "Author");
            assertEquals("Unsaved source for the second entry", sourceArea.getText());
        });
    }

    @Test
    @DisabledOnCIServer("Depends on https://bugs.openjdk.org/browse/JDK-8391883 ")
    void saveKeybindingWritesBackToRenderedEntryInsteadOfCurrentSelection() {
        BibEntry firstEntry = new BibEntry().withField(StandardField.TITLE, "First entry");
        BibEntry secondEntry = new BibEntry().withField(StandardField.TITLE, "Second entry");

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(firstEntry);
            sourceTab.notifyAboutFocus(firstEntry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea sourceArea = (CodeArea) sourceTab.getContent();
            String editedSource = sourceArea.getText().replace("First entry", "Edited first entry");
            sourceArea.clear();
            sourceArea.appendText(editedSource);

            // Simulate the selection model having advanced before the deferred tab refresh runs.
            sourceTab.currentEntryProperty().set(secondEntry);
            Event.fireEvent(sourceArea, new KeyEvent(KeyEvent.KEY_PRESSED, "s", "S", KeyCode.S, false, true, false, false));

            assertEquals("Edited first entry", firstEntry.getField(StandardField.TITLE).orElseThrow());
            assertEquals("Second entry", secondEntry.getField(StandardField.TITLE).orElseThrow());
        });
    }

    @Test
    void switchingToEqualContentEntryRebindsByIdentity() {
        BibEntry firstEntry = new BibEntry().withField(StandardField.TITLE, "Same title");
        BibEntry secondEntry = new BibEntry().withField(StandardField.TITLE, "Same title");

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(firstEntry);
            sourceTab.notifyAboutFocus(firstEntry);

            sourceTab.currentEntryProperty().set(secondEntry);
            sourceTab.notifyAboutFocus(secondEntry);
            secondEntry.setField(StandardField.AUTHOR, "Author");

            CodeArea sourceArea = (CodeArea) sourceTab.getContent();
            assertTrue(sourceArea.getText().contains("author = {Author}"));
            assertTrue(sourceArea.getText().contains("title  = {Same title}"));
        });
    }
}
