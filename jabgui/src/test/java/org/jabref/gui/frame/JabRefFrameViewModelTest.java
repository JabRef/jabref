package org.jabref.gui.frame;

import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.importer.actions.OpenDatabaseAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JabRefFrameViewModelTest extends ApplicationTest {

    private JabRefFrameViewModel viewModel;
    private LibraryTabContainer tabContainer;
    private DialogService dialogService;
    private StateManager stateManager;
    private GuiPreferences preferences;
    private TaskExecutor taskExecutor;

    @BeforeEach
    void setUp() {
        preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        AiService aiService = mock(AiService.class);
        stateManager = mock(StateManager.class);
        dialogService = mock(DialogService.class);
        tabContainer = mock(LibraryTabContainer.class);
        @SuppressWarnings("unchecked")
        Supplier<OpenDatabaseAction> openDatabaseAction = mock(Supplier.class);
        BibEntryTypesManager entryTypesManager = mock(BibEntryTypesManager.class);
        FileUpdateMonitor fileUpdateMonitor = mock(FileUpdateMonitor.class);
        UndoManager undoManager = mock(UndoManager.class);
        ClipBoardManager clipBoardManager = mock(ClipBoardManager.class);
        taskExecutor = mock(TaskExecutor.class);

        Injector.setModelOrService(TaskExecutor.class, taskExecutor);
        Injector.setModelOrService(DialogService.class, dialogService);
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(AiService.class, aiService);
        Injector.setModelOrService(ClipBoardManager.class, clipBoardManager);
        Injector.setModelOrService(FileUpdateMonitor.class, fileUpdateMonitor);
        Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);
        Injector.setModelOrService(UndoManager.class, undoManager);

        when(stateManager.getOpenDatabases()).thenReturn(FXCollections.observableArrayList());
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());

        viewModel = new JabRefFrameViewModel(
                preferences,
                aiService,
                stateManager,
                dialogService,
                tabContainer,
                openDatabaseAction,
                entryTypesManager,
                fileUpdateMonitor,
                undoManager,
                clipBoardManager,
                taskExecutor
        );
    }

    @Test
    void addParserResultCreatesNewTabIfNoneOpen() {
        // Given
        BibDatabaseContext context = new BibDatabaseContext();
        ParserResult parserResult = new ParserResult(context.getDatabase());

        LibraryTab libraryTab = mock(LibraryTab.class);
        when(libraryTab.getBibDatabaseContext()).thenReturn(context);

        when(tabContainer.getCurrentLibraryTab())
                .thenReturn(null)        // First call in addParserResult
                .thenReturn(libraryTab); // Second call after tabContainer.addTab

        when(stateManager.getOpenDatabases()).thenReturn(FXCollections.observableArrayList(context));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(context));

        // When
        interact(() -> viewModel.addParserResult(parserResult));

        // Then
        verify(tabContainer).addTab(any(BibDatabaseContext.class), eq(true));
        verify(dialogService).showCustomDialogAndWait(any());
    }

    @Test
    void addParserResultUsesExistingTabIfOpen() {
        // Given
        BibDatabaseContext context = new BibDatabaseContext();
        ParserResult parserResult = new ParserResult(context.getDatabase());

        LibraryTab libraryTab = mock(LibraryTab.class);
        when(libraryTab.getBibDatabaseContext()).thenReturn(context);
        when(tabContainer.getCurrentLibraryTab()).thenReturn(libraryTab);

        when(stateManager.getOpenDatabases()).thenReturn(FXCollections.observableArrayList(context));
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(context));

        // When
        interact(() -> viewModel.addParserResult(parserResult));

        // Then
        verify(dialogService).showCustomDialogAndWait(any());
    }
}
