package org.jabref.gui.sidepane;

import java.util.HashMap;
import java.util.Map;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.VBox;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.entryeditor.AdaptVisibleTabs;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.DirectoryUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

public class SidePane extends VBox {
    private final SidePaneViewModel viewModel;
    private final GuiPreferences preferences;
    private final StateManager stateManager;

    // These bindings need to be stored, otherwise they are garbage collected
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<SidePaneType, BooleanBinding> visibleBindings = new HashMap<>();

    public SidePane(LibraryTabContainer tabContainer,
                    GuiPreferences preferences,
                    JournalAbbreviationRepository abbreviationRepository,
                    TaskExecutor taskExecutor,
                    DialogService dialogService,
                    AiService aiService,
                    StateManager stateManager,
                    AdaptVisibleTabs adaptVisibleTabs,
                    FileUpdateMonitor fileUpdateMonitor,
                    DirectoryUpdateMonitor directoryUpdateMonitor,
                    BibEntryTypesManager entryTypesManager,
                    ClipBoardManager clipBoardManager,
                    UndoManager undoManager) {
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.viewModel = new SidePaneViewModel(
                tabContainer,
                preferences,
                abbreviationRepository,
                stateManager,
                taskExecutor,
                adaptVisibleTabs,
                dialogService,
                aiService,
                fileUpdateMonitor,
                directoryUpdateMonitor,
                entryTypesManager,
                clipBoardManager,
                undoManager);

        stateManager.getVisibleSidePaneComponents().addListener((ListChangeListener<SidePaneType>) c -> updateView());
        updateView();
    }

    private void updateView() {
        getChildren().clear();
        for (SidePaneType type : stateManager.getVisibleSidePaneComponents()) {
            SidePaneComponent view = viewModel.getSidePaneComponent(type);
            getChildren().add(view);
        }
    }

    public BooleanBinding paneVisibleBinding(SidePaneType pane) {
        BooleanBinding visibility = Bindings.createBooleanBinding(
                () -> stateManager.getVisibleSidePaneComponents().contains(pane),
                stateManager.getVisibleSidePaneComponents());
        visibleBindings.put(pane, visibility);
        return visibility;
    }

    public SimpleCommand getToggleCommandFor(SidePaneType sidePane) {
        return new TogglePaneAction(stateManager, sidePane, preferences.getSidePanePreferences());
    }

    public SidePaneComponent getSidePaneComponent(SidePaneType type) {
        return viewModel.getSidePaneComponent(type);
    }
}
