package org.jabref.gui.newentry;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewEntryViewModelTest {

    private final GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final LibraryTab libraryTab = mock(LibraryTab.class);
    private final DialogService dialogService = mock(DialogService.class);
    private final StateManager stateManager = mock(StateManager.class);
    private final UiTaskExecutor taskExecutor = mock(UiTaskExecutor.class);
    private final FileUpdateMonitor fileUpdateMonitor = mock(FileUpdateMonitor.class);
    private final AiService aiService = mock(AiService.class);

    private NewEntryViewModel viewModel;

    @BeforeEach
    void setUp() {
        when(preferences.getImportFormatPreferences()).thenReturn(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));
        when(preferences.getImporterPreferences()).thenReturn(mock(ImporterPreferences.class));
        viewModel = new NewEntryViewModel(preferences, libraryTab, dialogService, stateManager, taskExecutor, fileUpdateMonitor, aiService);
    }

    @Test
    void executingPropertyInitiallyFalse() {
        assertFalse(viewModel.executingProperty().get());
    }

    @Test
    void searchBoxBindingTracksExecutingState() {
        BooleanProperty visibleProperty = new SimpleBooleanProperty();
        BooleanProperty managedProperty = new SimpleBooleanProperty();

        visibleProperty.bind(viewModel.executingProperty());
        managedProperty.bind(viewModel.executingProperty());

        assertFalse(visibleProperty.get());
        assertFalse(managedProperty.get());
    }
}
