package org.jabref.gui.undo;

import java.util.function.Supplier;

import javafx.beans.binding.Bindings;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.undo.UndoManager;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

public class RedoAction extends SimpleCommand {
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final UndoManager undoManager;
    /// Held as a field so the listener it registers on the manager stays reachable.
    private final GuiUndoManager guiUndoManager;

    public RedoAction(Supplier<LibraryTab> tabSupplier, UndoManager undoManager, DialogService dialogService, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.undoManager = undoManager;
        this.guiUndoManager = new GuiUndoManager(undoManager);

        this.executable.bind(Bindings.and(needsDatabase(stateManager), guiUndoManager.redoableProperty()));
    }

    @Override
    public void execute() {
        if (undoManager.canRedo()) {
            undoManager.redo();
            dialogService.notify(Localization.lang("Redo"));
        } else {
            dialogService.notify(Localization.lang("Nothing to redo") + '.');
        }
        tabSupplier.get().markChangedOrUnChanged();
    }
}
