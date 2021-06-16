package org.jabref.gui.undo;

import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.logic.l10n.Localization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndoRedoAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoRedoAction.class);

    private final StandardActions action;
    private final JabRefFrame frame;
    private DialogService dialogService;

    public UndoRedoAction(StandardActions action, JabRefFrame frame, DialogService dialogService, StateManager stateManager) {
        this.action = action;
        this.frame = frame;
        this.dialogService = dialogService;

        // ToDo: Rework the UndoManager to something like the following, if it had a property.
        //  this.executable.bind(frame.getCurrentBasePanel().getUndoManager().canUndo())
        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = frame.getCurrentLibraryTab();
        if (action == StandardActions.UNDO) {
            try {
                libraryTab.getUndoManager().undo();
                libraryTab.markBaseChanged();
                dialogService.notify(Localization.lang("Undo"));
            } catch (CannotUndoException ex) {
                dialogService.notify(Localization.lang("Nothing to undo") + '.');
            }
            frame.getCurrentLibraryTab().markChangedOrUnChanged();
        } else if (action == StandardActions.REDO) {
            try {
                libraryTab.getUndoManager().redo();
                libraryTab.markBaseChanged();
                dialogService.notify(Localization.lang("Redo"));
            } catch (CannotRedoException ex) {
                dialogService.notify(Localization.lang("Nothing to redo") + '.');
            }

            libraryTab.markChangedOrUnChanged();
        } else {
            LOGGER.debug("No undo/redo action: " + action.name());
        }
    }
}
