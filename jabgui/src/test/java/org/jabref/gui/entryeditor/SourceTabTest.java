package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
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
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.support.DisabledOnCIServer;

import io.github.kusoroadeolu.veneer.BibTeXSyntaxHighlighter;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
    private DialogService dialogService;

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
            dialogService = mock(DialogService.class);

            sourceTab = new SourceTab(

                    fieldPreferences,
                    importFormatPreferences,
                    new DummyFileUpdateMonitor(),
                    dialogService,
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

    @Test
    void invalidSourceErrorIsDeferredUntilNestedEventLoopIsAllowed() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("baseline")
                .withField(StandardField.TITLE, "Original title")
                .withField(StandardField.AUTHOR, "Original author")
                .withField(StandardField.YEAR, "2000");

        var originalType = entry.getType();
        var originalCitationKey = entry.getCitationKey();
        var originalFields = Map.copyOf(entry.getFieldMap());

        ConcurrentLinkedQueue<Boolean> dialogContexts = new ConcurrentLinkedQueue<>();
        CountDownLatch pulseCompleted = new CountDownLatch(1);
        CompletableFuture<Void> pulseOutcome = new CompletableFuture<>();

        doAnswer(_ -> {
            dialogContexts.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea sourceArea = (CodeArea) sourceTab.getContent();

            sourceArea.requestFocus();
            assertTrue(sourceArea.isFocused());

            sourceArea.clear();
            sourceArea.appendText("This is not a BibTeX entry");

            assertEquals("This is not a BibTeX entry", sourceArea.getText());
            assertTrue(sourceArea.isFocused());
        });

        Runnable listener = new Runnable() {
            @Override
            public void run() {
                try {
                    scene.removePostLayoutPulseListener(this);
                    assertFalse(Platform.canStartNestedEventLoop());

                    CodeArea sourceArea = (CodeArea) sourceTab.getContent();
                    assertTrue(sourceArea.isFocused());
                    pane.requestFocus();
                    assertFalse(sourceArea.isFocused());

                    assertEquals(originalType, entry.getType());
                    assertEquals(originalCitationKey, entry.getCitationKey());
                    assertEquals(originalFields, entry.getFieldMap());
                    pulseOutcome.complete(null);
                } catch (RuntimeException | AssertionError failure) {
                    pulseOutcome.completeExceptionally(failure);
                } finally {
                    pulseCompleted.countDown();
                }
            }
        };

        try {
            JavaFxExtension.invokeAndWait(() -> {
                scene.addPostLayoutPulseListener(listener);
                Platform.requestNextPulse();
            });

            assertTrue(pulseCompleted.await(10, TimeUnit.SECONDS), "Post-layout pulse did not complete");
            pulseOutcome.get(10, TimeUnit.SECONDS);
            JavaFxExtension.awaitEvents();
            assertEquals(List.of(true), List.copyOf(dialogContexts));
        } finally {
            JavaFxExtension.invokeAndWait(() -> scene.removePostLayoutPulseListener(listener));
        }
    }

    @Test
    void sourceWriteBackWithRemovedFieldsDoesNotReenterEditorRefresh() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("original")
                .withField(StandardField.TITLE, "Original title")
                .withField(StandardField.AUTHOR, "Original author")
                .withField(StandardField.YEAR, "2000")
                .withField(StandardField.JOURNAL, "Original journal")
                .withField(StandardField.PUBLISHER, "Original publisher")
                .withField(StandardField.PAGES, "1--10")
                .withField(StandardField.DOI, "10.1000/original");
        BibEntry expected = new BibEntry(StandardEntryType.Book)
                .withCitationKey("short")
                .withField(StandardField.TITLE, "Short title")
                .withField(StandardField.AUTHOR, "Original author");
        String editedSource = """
                @Book{short,
                  author = {Original author},
                  title = {Short title}
                }
                """;

        showSourceForEntry(entry);

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea sourceArea = (CodeArea) sourceTab.getContent();
            sourceArea.requestFocus();
            assertTrue(sourceArea.isFocused());
            sourceArea.clear();
            sourceArea.appendText(editedSource);

            String editedDocument = sourceArea.getText();
            List<String> documentChanges = new ArrayList<>();
            StyledTextModel.Listener documentListener = change -> {
                if (change.isEdit()) {
                    documentChanges.add(sourceArea.getText());
                }
            };
            InvalidationListener rebindOnTypeChange = _ -> sourceTab.notifyAboutFocus(entry);
            sourceArea.getModel().addListener(documentListener);
            entry.typeProperty().addListener(rebindOnTypeChange);
            try {
                pane.requestFocus();
                assertFalse(sourceArea.isFocused());
                assertEquals(expected.getType(), entry.getType());
                assertEquals(expected.getCitationKey(), entry.getCitationKey());
                assertEquals(expected.getFieldMap(), entry.getFieldMap());
                assertEquals(List.of(), documentChanges);
                assertEquals(editedDocument, sourceArea.getText());

                entry.setField(StandardField.TITLE, "External title");
                assertTrue(sourceArea.getText().contains("External title"));
                assertEquals(List.of(sourceArea.getText()), documentChanges);
            } finally {
                entry.typeProperty().removeListener(rebindOnTypeChange);
                sourceArea.getModel().removeListener(documentListener);
            }
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea sourceArea = (CodeArea) sourceTab.getContent();
            sourceArea.requestFocus();
            assertTrue(sourceArea.isFocused());
            assertTrue(sourceArea.isEditable());
            sourceArea.appendText("\n");
            assertTrue(sourceArea.getText().endsWith("\n"));
            pane.requestFocus();
            assertFalse(sourceArea.isFocused());
        });
        JavaFxExtension.awaitEvents();

        expected.withField(StandardField.TITLE, "External title");
        assertEquals(expected.getType(), entry.getType());
        assertEquals(expected.getFieldMap(), entry.getFieldMap());
    }

    /// Editing the source and leaving the code area must write every kind of change back to the entry,
    /// so the field editors and the entry table show the edited data.
    /// See https://github.com/JabRef/jabref/issues/9219
    @Test
    void editedSourceIsWrittenBackToEntryOnFocusLoss() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("old")
                .withField(StandardField.TITLE, "Old title")
                .withField(StandardField.AUTHOR, "Old author")
                .withField(StandardField.YEAR, "2000");

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea sourceArea = (CodeArea) sourceTab.getContent();
            sourceArea.requestFocus();
            assertTrue(sourceArea.isFocused());
            sourceArea.clear();
            sourceArea.appendText("""
                    @Book{new,
                      author  = {Old author},
                      title   = {New title},
                      journal = {New journal},
                    }
                    """);
            // Focus loss stores the visible source
            pane.requestFocus();
            assertFalse(sourceArea.isFocused());
        });
        JavaFxExtension.awaitEvents();

        assertEquals(StandardEntryType.Book, entry.getType());
        assertEquals(Optional.of("new"), entry.getCitationKey());
        assertEquals(Optional.of("New title"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("New journal"), entry.getField(StandardField.JOURNAL));
        assertEquals(Optional.of("Old author"), entry.getField(StandardField.AUTHOR));
        assertEquals(Optional.empty(), entry.getField(StandardField.YEAR));
    }

    @Test
    void editedSourceIsWrittenBackWhenSwitchingToAnotherEntry() {
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

            sourceTab.currentEntryProperty().set(secondEntry);
            sourceTab.notifyAboutFocus(secondEntry);
        });
        JavaFxExtension.awaitEvents();

        assertEquals(Optional.of("Edited first entry"), firstEntry.getField(StandardField.TITLE));
        assertEquals(Optional.of("Second entry"), secondEntry.getField(StandardField.TITLE));
    }

    @Test
    void unchangedInvalidSourceDoesNotShowDuplicateErrors() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("baseline")
                .withField(StandardField.TITLE, "Original title");

        ConcurrentLinkedQueue<Boolean> dialogs = new ConcurrentLinkedQueue<>();
        doAnswer(_ -> {
            dialogs.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("This is not a BibTeX entry");

            pane.requestFocus();
            assertFalse(editor.isFocused());
        });
        JavaFxExtension.awaitEvents();

        assertEquals(List.of(true), List.copyOf(dialogs));

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            pane.requestFocus();
            assertFalse(editor.isFocused());
        });
        JavaFxExtension.awaitEvents();

        assertEquals(List.of(true), List.copyOf(dialogs));
    }

    @Test
    void changedInvalidSourceCanShowAnotherError() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("baseline")
                .withField(StandardField.TITLE, "Original title");

        ConcurrentLinkedQueue<Boolean> dialogs = new ConcurrentLinkedQueue<>();
        doAnswer(_ -> {
            dialogs.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("First invalid source");

            pane.requestFocus();
            assertFalse(editor.isFocused());
        });
        JavaFxExtension.awaitEvents();

        assertEquals(1, dialogs.size());

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("Second invalid source");

            pane.requestFocus();
            assertFalse(editor.isFocused());
        });
        JavaFxExtension.awaitEvents();

        assertEquals(List.of(true, true), List.copyOf(dialogs));
    }

    @Test
    void correctedSourceCancelsPendingError() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("baseline")
                .withField(StandardField.TITLE, "Original title");

        ConcurrentLinkedQueue<Boolean> dialogs = new ConcurrentLinkedQueue<>();
        doAnswer(_ -> {
            dialogs.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("Invalid source");

            pane.requestFocus();
            assertFalse(editor.isFocused());

            editor.clear();
            editor.appendText("@Article{baseline, title = {Corrected title}}\n");
        });
        JavaFxExtension.awaitEvents();

        assertTrue(dialogs.isEmpty());
    }

    @Test
    void switchingEntryCancelsPendingError() {
        BibEntry first = new BibEntry(StandardEntryType.Article)
                .withCitationKey("first")
                .withField(StandardField.TITLE, "First title");

        BibEntry second = new BibEntry(StandardEntryType.Article)
                .withCitationKey("second")
                .withField(StandardField.TITLE, "Second title");

        ConcurrentLinkedQueue<Boolean> dialogs = new ConcurrentLinkedQueue<>();
        doAnswer(_ -> {
            dialogs.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(first);
            sourceTab.notifyAboutFocus(first);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("Invalid source");

            pane.requestFocus();
            assertFalse(editor.isFocused());

            sourceTab.currentEntryProperty().set(second);
            sourceTab.notifyAboutFocus(second);
        });
        JavaFxExtension.awaitEvents();

        assertTrue(dialogs.isEmpty());
    }

    @Test
    void disposingTabCancelsPendingError() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("baseline")
                .withField(StandardField.TITLE, "Original title");

        ConcurrentLinkedQueue<Boolean> dialogs = new ConcurrentLinkedQueue<>();
        doAnswer(_ -> {
            dialogs.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("Invalid source");

            pane.requestFocus();
            assertFalse(editor.isFocused());

            sourceTab.dispose();
        });
        JavaFxExtension.awaitEvents();

        assertTrue(dialogs.isEmpty());
    }

    @Test
    void equalContentRoundTripCancelsPendingError() {
        BibEntry first = new BibEntry(StandardEntryType.Article)
                .withCitationKey("same")
                .withField(StandardField.TITLE, "Same title");

        BibEntry second = new BibEntry(StandardEntryType.Article)
                .withCitationKey("same")
                .withField(StandardField.TITLE, "Same title");

        assertNotSame(first, second);
        assertEquals(first, second);

        ConcurrentLinkedQueue<Boolean> dialogs = new ConcurrentLinkedQueue<>();
        doAnswer(_ -> {
            dialogs.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(first);
            sourceTab.notifyAboutFocus(first);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("Invalid source");

            pane.requestFocus();
            assertFalse(editor.isFocused());

            sourceTab.currentEntryProperty().set(second);
            sourceTab.currentEntryProperty().set(first);
        });
        JavaFxExtension.awaitEvents();

        assertTrue(dialogs.isEmpty(),
                "A -> B -> A should cancel the old queued error");
    }

    @Test
    void parsedEntryWarningIsDeliveredAfterWriteBack() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("old")
                .withField(StandardField.AUTHOR, "Original author")
                .withField(StandardField.TITLE, "Original title");

        ConcurrentLinkedQueue<Boolean> dialogContexts = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Optional<String>> keysAtDialog = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Optional<String>> authorsAtDialog = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Optional<String>> titlesAtDialog = new ConcurrentLinkedQueue<>();

        doAnswer(_ -> {
            dialogContexts.add(Platform.canStartNestedEventLoop());
            keysAtDialog.add(entry.getCitationKey());
            authorsAtDialog.add(entry.getField(StandardField.AUTHOR));
            titlesAtDialog.add(entry.getField(StandardField.TITLE));
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        JavaFxExtension.awaitEvents();

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());
            editor.clear();
            editor.appendText("@article{author={Ed von Test},title={Test}}");
            pane.requestFocus();
            assertFalse(editor.isFocused());
        });
        JavaFxExtension.awaitEvents();

        assertEquals(List.of(true), List.copyOf(dialogContexts));
        assertEquals(List.of(Optional.empty()), List.copyOf(keysAtDialog));
        assertEquals(List.of(Optional.of("Ed von Test")), List.copyOf(authorsAtDialog));
        assertEquals(List.of(Optional.of("Test")), List.copyOf(titlesAtDialog));

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            pane.requestFocus();
        });
        JavaFxExtension.awaitEvents();
        assertEquals(1, dialogContexts.size());
    }

    private BibEntry createParserWarningEntry() {
        return new BibEntry(StandardEntryType.Article)
                .withCitationKey("old")
                .withField(StandardField.AUTHOR, "Original author")
                .withField(StandardField.TITLE, "Original title");
    }

    private void showSourceForEntry(BibEntry entry) {
        JavaFxExtension.invokeAndWait(() -> {
            pane.getSelectionModel().select(sourceTab);
            sourceTab.currentEntryProperty().set(entry);
            sourceTab.notifyAboutFocus(entry);
        });
        JavaFxExtension.awaitEvents();
    }

    @Test
    void editingSourceCancelsPendingParserWarning() {
        BibEntry entry = createParserWarningEntry();
        ConcurrentLinkedQueue<Boolean> dialogs = new ConcurrentLinkedQueue<>();

        doAnswer(_ -> {
            dialogs.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        showSourceForEntry(entry);

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("@article{author={Ed von Test},title={Test}}");

            pane.requestFocus();
            assertFalse(editor.isFocused());

            editor.clear();
            editor.appendText("@article{corrected,author={Ed von Test},title={Corrected}}");
        });

        JavaFxExtension.awaitEvents();
        assertTrue(dialogs.isEmpty());
    }

    @Test
    void switchingEntryCancelsPendingParserWarning() {
        BibEntry first = createParserWarningEntry();
        BibEntry second = new BibEntry(StandardEntryType.Article)
                .withCitationKey("second")
                .withField(StandardField.TITLE, "Second title");

        ConcurrentLinkedQueue<Boolean> dialogs = new ConcurrentLinkedQueue<>();
        doAnswer(_ -> {
            dialogs.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        showSourceForEntry(first);

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("@article{author={Ed von Test},title={Test}}");

            pane.requestFocus();
            assertFalse(editor.isFocused());

            sourceTab.currentEntryProperty().set(second);
            sourceTab.notifyAboutFocus(second);
        });

        JavaFxExtension.awaitEvents();
        assertTrue(dialogs.isEmpty());
    }

    @Test
    void disposingTabCancelsPendingParserWarning() {
        BibEntry entry = createParserWarningEntry();
        ConcurrentLinkedQueue<Boolean> dialogs = new ConcurrentLinkedQueue<>();

        doAnswer(_ -> {
            dialogs.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showErrorDialogAndWait(anyString());

        showSourceForEntry(entry);

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("@article{author={Ed von Test},title={Test}}");

            pane.requestFocus();
            assertFalse(editor.isFocused());

            sourceTab.dispose();
        });

        JavaFxExtension.awaitEvents();
        assertTrue(dialogs.isEmpty());
    }

    @Test
    void multipleEntriesWarningIsDeferredUntilNestedEventLoopIsAllowed() throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("baseline")
                .withField(StandardField.TITLE, "Original title")
                .withField(StandardField.AUTHOR, "Original author")
                .withField(StandardField.YEAR, "2000");

        var originalType = entry.getType();
        var originalCitationKey = entry.getCitationKey();
        var originalFields = Map.copyOf(entry.getFieldMap());

        ConcurrentLinkedQueue<Boolean> dialogContexts = new ConcurrentLinkedQueue<>();
        CountDownLatch pulseCompleted = new CountDownLatch(1);
        CompletableFuture<Void> pulseOutcome = new CompletableFuture<>();

        doAnswer(_ -> {
            dialogContexts.add(Platform.canStartNestedEventLoop());
            return null;
        }).when(dialogService).showWarningDialogAndWait(anyString(), anyString());

        showSourceForEntry(entry);

        JavaFxExtension.invokeAndWait(() -> {
            CodeArea editor = (CodeArea) sourceTab.getContent();
            editor.requestFocus();
            assertTrue(editor.isFocused());

            editor.clear();
            editor.appendText("""
                    @article{first,
                      author = {First Author},
                      title = {First Title}
                    }
                    @article{second,
                      author = {Second Author},
                      title = {Second Title}
                    }
                    """);
        });

        Runnable listener = new Runnable() {
            @Override
            public void run() {
                try {
                    scene.removePostLayoutPulseListener(this);
                    assertFalse(Platform.canStartNestedEventLoop());

                    CodeArea editor = (CodeArea) sourceTab.getContent();
                    assertTrue(editor.isFocused());

                    pane.requestFocus();
                    assertFalse(editor.isFocused());

                    assertEquals(originalType, entry.getType());
                    assertEquals(originalCitationKey, entry.getCitationKey());
                    assertEquals(originalFields, entry.getFieldMap());

                    pulseOutcome.complete(null);
                } catch (RuntimeException | AssertionError failure) {
                    pulseOutcome.completeExceptionally(failure);
                } finally {
                    pulseCompleted.countDown();
                }
            }
        };

        try {
            JavaFxExtension.invokeAndWait(() -> {
                scene.addPostLayoutPulseListener(listener);
                Platform.requestNextPulse();
            });

            assertTrue(pulseCompleted.await(10, TimeUnit.SECONDS),
                    "Post-layout pulse did not complete");
            pulseOutcome.get(10, TimeUnit.SECONDS);
            JavaFxExtension.awaitEvents();

            assertEquals(List.of(true), List.copyOf(dialogContexts));
            assertEquals(originalType, entry.getType());
            assertEquals(originalCitationKey, entry.getCitationKey());
            assertEquals(originalFields, entry.getFieldMap());
        } finally {
            JavaFxExtension.invokeAndWait(() ->
                    scene.removePostLayoutPulseListener(listener));
        }
    }
}
