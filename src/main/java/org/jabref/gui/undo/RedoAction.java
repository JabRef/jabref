package org.jabref.gui.undo;

import java.util.function.Supplier;

import javax.swing.undo.CannotUndoException;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.logic.l10n.Localization;

public class RedoAction extends UndoRedoAction {
    public RedoAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, StateManager stateManager) {
        super(tabSupplier, dialogService, stateManager);
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = this.tabSupplier.get();
        try {
            libraryTab.getUndoManager().redo();
            dialogService.notify(Localization.lang("Redo"));
        } catch (CannotUndoException ex) {
            dialogService.notify(Localization.lang("Nothing to redo") + '.');
        }
        libraryTab.markChangedOrUnChanged();
    }
}
