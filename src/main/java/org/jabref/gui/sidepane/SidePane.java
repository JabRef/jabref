package org.jabref.gui.sidepane;

import java.util.HashMap;
import java.util.Map;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class SidePane extends VBox {
    private final SidePaneViewModel viewModel;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;

    // These bindings need to be stored, otherwise they are garbage collected
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final Map<SidePaneType, BooleanBinding> visibleBindings = new HashMap<>();

    public SidePane(LibraryTabContainer tabContainer,
                    PreferencesService preferencesService,
                    JournalAbbreviationRepository abbreviationRepository,
                    TaskExecutor taskExecutor,
                    DialogService dialogService,
                    StateManager stateManager,
                    FileUpdateMonitor fileUpdateMonitor,
                    BibEntryTypesManager entryTypesManager,
                    UndoManager undoManager) {
        this.stateManager = stateManager;
        this.preferencesService = preferencesService;
        this.viewModel = new SidePaneViewModel(
                tabContainer,
                preferencesService,
                abbreviationRepository,
                stateManager,
                taskExecutor,
                dialogService,
                fileUpdateMonitor,
                entryTypesManager,
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
        return new TogglePaneAction(stateManager, sidePane, preferencesService.getSidePanePreferences());
    }

    public SidePaneComponent getSidePaneComponent(SidePaneType type) {
        return viewModel.getSidePaneComponent(type);
    }
}
