package org.jabref.gui.search;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGuiStateManager;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.MainTable;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.TransferMode;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;

import static org.jabref.gui.testutils.JavaFxExtension.invokeAndWait;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class GlobalSearchResultDialogTest {

    private GuiPreferences preferences;
    private StateManager stateManager;
    private DialogService dialogService;
    private TaskExecutor taskExecutor;
    private BibEntryTypesManager entryTypesManager;
    private ClipBoardManager clipBoardManager;
    private KeyBindingRepository keyBindingRepository;
    private LibraryTabContainer libraryTabContainer;

    @BeforeEach
    void setUp() {
        preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SearchPreferences searchPreferences = mock(SearchPreferences.class);
        when(searchPreferences.getSearchFlags()).thenReturn(EnumSet.noneOf(SearchFlags.class));
        when(searchPreferences.getObservableSearchFlags()).thenReturn(FXCollections.observableSet());
        when(searchPreferences.keepSearchStringProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(searchPreferences.searchDisplayModeProperty()).thenReturn(new SimpleObjectProperty<>(SearchDisplayMode.FLOAT));
        when(searchPreferences.getSearchWindowHeight()).thenReturn(400.0);
        when(searchPreferences.getSearchWindowWidth()).thenReturn(600.0);
        when(searchPreferences.getSearchWindowDividerPosition()).thenReturn(0.5);
        when(preferences.getSearchPreferences()).thenReturn(searchPreferences);

        PreviewPreferences previewPreferences = preferences.getPreviewPreferences();
        org.mockito.Mockito.doReturn(new org.jabref.logic.preview.TextBasedPreviewLayout("", mock(org.jabref.logic.layout.LayoutFormatterPreferences.class), mock(org.jabref.logic.journals.JournalAbbreviationRepository.class)))
                .when(previewPreferences).getSelectedPreviewLayout();

        keyBindingRepository = mock(KeyBindingRepository.class);
        when(preferences.getKeyBindingRepository()).thenReturn(keyBindingRepository);

        stateManager = new JabRefGuiStateManager();
        dialogService = mock(DialogService.class);
        taskExecutor = new CurrentThreadTaskExecutor();
        entryTypesManager = new BibEntryTypesManager();
        clipBoardManager = mock(ClipBoardManager.class);

        libraryTabContainer = mock(LibraryTabContainer.class);
        LibraryTab libraryTab = mock(LibraryTab.class);
        when(libraryTab.getMainTable()).thenReturn(mock(MainTable.class));
        when(libraryTabContainer.getCurrentLibraryTab()).thenReturn(libraryTab);

        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(DialogService.class, dialogService);
        Injector.setModelOrService(TaskExecutor.class, taskExecutor);
        Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);
        Injector.setModelOrService(ClipBoardManager.class, clipBoardManager);
        Injector.setModelOrService(KeyBindingRepository.class, keyBindingRepository);
    }

    @AfterEach
    void tearDown() {
        Injector.forgetAll();
    }

    private void registerSearchContext(BibDatabaseContext context) {
        org.jabref.logic.search.SearchContext searchContext = new org.jabref.logic.search.SearchContext(
                new SimpleBooleanProperty(false),
                org.jabref.logic.search.NoOpSearchBackend::new,
                () -> new org.jabref.logic.search.inmemory.InMemorySearchBackend(context, new org.jabref.model.entry.BibEntryPreferences(','))
        );
        stateManager.setSearchContext(context, searchContext);
    }

    @Test
    void dialogInitializesCorrectly() {
        invokeAndWait(() -> {
            GlobalSearchResultDialog dialog = new GlobalSearchResultDialog(libraryTabContainer);
            assertNotNull(dialog);
        });
    }

    @Test
    void copySelectedEntriesToClipboard() throws IOException {
        BibDatabaseContext context = new BibDatabaseContext();
        registerSearchContext(context);
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Alice");
        context.getDatabase().insertEntry(entry);
        stateManager.getOpenDatabases().add(context);
        stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH).set(Optional.of(new org.jabref.model.search.query.SearchQuery("Alice")));

        when(keyBindingRepository.mapToKeyBinding(any(KeyEvent.class))).thenReturn(Optional.of(KeyBinding.COPY));

        invokeAndWait(() -> {
            GlobalSearchResultDialog dialog = new GlobalSearchResultDialog(libraryTabContainer);
            javafx.scene.control.SplitPane container = (javafx.scene.control.SplitPane) dialog.getDialogPane().lookup("#container");
            assertNotNull(container);
            SearchResultsTable resultsTable = (SearchResultsTable) container.getItems().getFirst();
            assertNotNull(resultsTable);

            resultsTable.getSelectionModel().select(0);

            KeyEvent copyKeyEvent = new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.C, false, true, false, false
            );
            resultsTable.fireEvent(copyKeyEvent);
        });

        verify(clipBoardManager).setContent(eq(TransferMode.COPY), any(), any(), any(), any());
    }

    @Test
    void copyDoesNothingWhenNoSelection() throws IOException {
        BibDatabaseContext context = new BibDatabaseContext();
        registerSearchContext(context);
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Alice");
        context.getDatabase().insertEntry(entry);
        stateManager.getOpenDatabases().add(context);
        stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH).set(Optional.of(new org.jabref.model.search.query.SearchQuery("Alice")));

        when(keyBindingRepository.mapToKeyBinding(any(KeyEvent.class))).thenReturn(Optional.of(KeyBinding.COPY));

        invokeAndWait(() -> {
            GlobalSearchResultDialog dialog = new GlobalSearchResultDialog(libraryTabContainer);
            javafx.scene.control.SplitPane container = (javafx.scene.control.SplitPane) dialog.getDialogPane().lookup("#container");
            assertNotNull(container);
            SearchResultsTable resultsTable = (SearchResultsTable) container.getItems().getFirst();
            assertNotNull(resultsTable);

            resultsTable.getSelectionModel().clearSelection();

            KeyEvent copyKeyEvent = new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.C, false, true, false, false
            );
            resultsTable.fireEvent(copyKeyEvent);
        });

        org.mockito.Mockito.verifyNoInteractions(clipBoardManager);
    }

    @Test
    void copyMultipleEntriesAcrossDifferentLibraries() throws IOException {
        BibDatabaseContext context1 = new BibDatabaseContext();
        registerSearchContext(context1);
        BibEntry entry1 = new BibEntry().withField(StandardField.AUTHOR, "Alice");
        context1.getDatabase().insertEntry(entry1);

        BibDatabaseContext context2 = new BibDatabaseContext();
        registerSearchContext(context2);
        BibEntry entry2 = new BibEntry().withField(StandardField.AUTHOR, "Alice");
        context2.getDatabase().insertEntry(entry2);

        stateManager.getOpenDatabases().addAll(List.of(context1, context2));
        stateManager.activeSearchQuery(SearchType.GLOBAL_SEARCH).set(Optional.of(new org.jabref.model.search.query.SearchQuery("Alice")));

        when(keyBindingRepository.mapToKeyBinding(any(KeyEvent.class))).thenReturn(Optional.of(KeyBinding.COPY));

        invokeAndWait(() -> {
            GlobalSearchResultDialog dialog = new GlobalSearchResultDialog(libraryTabContainer);
            javafx.scene.control.SplitPane container = (javafx.scene.control.SplitPane) dialog.getDialogPane().lookup("#container");
            SearchResultsTable resultsTable = (SearchResultsTable) container.getItems().getFirst();

            resultsTable.getSelectionModel().selectAll();

            KeyEvent copyKeyEvent = new KeyEvent(
                    KeyEvent.KEY_PRESSED, "", "", KeyCode.C, false, true, false, false
            );
            resultsTable.fireEvent(copyKeyEvent);
        });

        // Notice: clipBoardManager receives context1 as the single BibDatabaseContext for all entries,
        // even though entry2 belongs to context2!
        verify(clipBoardManager).setContent(eq(TransferMode.COPY), eq(context1), any(), any(), any());
    }
}
