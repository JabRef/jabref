package org.jabref.gui.entryeditor;

import java.util.function.Supplier;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

public class OpenEntryEditorAction extends SimpleCommand {

    private final Supplier<LibraryTab> tabSupplier;
    private final StateManager stateManager;

    public OpenEntryEditorAction(Supplier<LibraryTab> tabSupplier, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    public void execute() {
        if (!stateManager.getSelectedEntries().isEmpty()) {
            tabSupplier.get().showAndEdit(stateManager.getSelectedEntries().getFirst());
        }
    }
}
