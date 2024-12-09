package org.jabref.gui.undo;

import java.util.function.Supplier;

import javax.swing.undo.CannotUndoException;

import javafx.beans.binding.Bindings;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/**
 * @implNote See also {@link RedoAction}
 */
public class UndoAction extends SimpleCommand {
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;
    private final CountingUndoManager undoManager;

    public UndoAction(Supplier<LibraryTab> tabSupplier, CountingUndoManager undoManager, DialogService dialogService, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.undoManager = undoManager;

        this.executable.bind(Bindings.and(needsDatabase(stateManager), undoManager.getUndoableProperty()));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = this.tabSupplier.get();
        try {
            if (undoManager.canUndo()) {
                undoManager.undo();
                dialogService.notify(Localization.lang("Undo"));
            } else {
                throw new CannotUndoException();
            }
        } catch (CannotUndoException ex) {
            dialogService.notify(Localization.lang("Nothing to undo") + '.');
        }
        libraryTab.markChangedOrUnChanged();
    }
}
