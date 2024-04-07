package org.jabref.gui;

import java.util.List;

import javafx.scene.control.TabPane;

import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class CopyEntryToLibraryAction extends SimpleCommand {
    private final DialogService dialogService;
    private final StateManager stateManager;
    private LibraryTab source;
    private LibraryTab destination;
    private final TabPane tabPane;

    public CopyEntryToLibraryAction(DialogService dialogService, StateManager stateManager, LibraryTab source, LibraryTab destination, TabPane tabPane) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.source = source;
        this.destination = destination;
        this.tabPane = tabPane;

        this.executable.bind(ActionHelper.needsEntriesSelected(1, stateManager));
    }

    @Override
    public void execute() {
        BibEntry selectedEntry = stateManager.getSelectedEntries().getFirst();
        System.out.println(selectedEntry.getField(StandardField.EDITORA));
        dialogService.showCustomDialogAndWait(new CopyEntryToDialogView());

        // tabPane.getSelectionModel().select(destination);
        // destination.dropEntry(List.of((BibEntry) selectedEntry.clone()));
        // destination.dropEntry();
    }
}
