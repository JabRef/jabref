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
import org.jabref.gui.ai.chatting.chathistory.ChatHistoryService;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.logic.preferences.Preferences;

public class SidePane extends VBox {
    private final SidePaneViewModel viewModel;
    private final Preferences preferences;
    private final StateManager stateManager;

    // These bindings need to be stored, otherwise they are garbage collected
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<SidePaneType, BooleanBinding> visibleBindings = new HashMap<>();

    public SidePane(LibraryTabContainer tabContainer,
                    Preferences preferences,
                    ChatHistoryService chatHistoryService,
                    JournalAbbreviationRepository abbreviationRepository,
                    TaskExecutor taskExecutor,
                    DialogService dialogService,
                    StateManager stateManager,
                    FileUpdateMonitor fileUpdateMonitor,
                    BibEntryTypesManager entryTypesManager,
                    ClipBoardManager clipBoardManager,
                    UndoManager undoManager) {
        this.stateManager = stateManager;
        this.preferences = preferences;
        this.viewModel = new SidePaneViewModel(
                tabContainer,
                preferences,
                chatHistoryService,
                abbreviationRepository,
                stateManager,
                taskExecutor,
                dialogService,
                fileUpdateMonitor,
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
