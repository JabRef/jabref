package org.jabref.gui.undo;

import java.util.function.Supplier;

import javax.swing.undo.CannotRedoException;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

/**
 * @implNote See also {@link UndoAction}
 */
public class RedoAction extends SimpleCommand {
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;

    public RedoAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;

        // TODO: Rework the UndoManager to something like the following, if it had a property.
        //  this.executable.bind(frame.getCurrentBasePanel().getUndoManager().canUndo())
        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = this.tabSupplier.get();
        try {
            libraryTab.getUndoManager().redo();
            dialogService.notify(Localization.lang("Redo"));
        } catch (CannotRedoException ex) {
            dialogService.notify(Localization.lang("Nothing to redo") + '.');
        }
        libraryTab.markChangedOrUnChanged();
    }
}
