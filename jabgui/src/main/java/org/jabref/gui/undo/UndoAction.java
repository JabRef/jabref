package org.jabref.gui.undo;

import java.util.function.Supplier;

import javafx.beans.binding.Bindings;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class UndoAction extends SimpleCommand {
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    /// Held as a field so the listener it registers on the manager stays reachable.
    private final GuiUndoManager guiUndoManager;

    public UndoAction(Supplier<LibraryTab> tabSupplier, UndoManager undoManager, DialogService dialogService, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.guiUndoManager = new GuiUndoManager(undoManager);

        this.executable.bind(Bindings.and(needsDatabase(stateManager), guiUndoManager.undoableProperty()));
    }

    @Override
    public void execute() {
        if (undoManager.canUndo()) {
            undoManager.undo();
            dialogService.notify(Localization.lang("Undo"));
        } else {
            dialogService.notify(Localization.lang("Nothing to undo") + '.');
        }
        tabSupplier.get().markChangedOrUnChanged();
    }
}
