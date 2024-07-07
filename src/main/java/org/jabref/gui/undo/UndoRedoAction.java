package org.jabref.gui.undo;

import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UndoRedoAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(UndoRedoAction.class);

    protected final Supplier<LibraryTab> tabSupplier;
    protected final DialogService dialogService;

    public UndoRedoAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;

        // TODO: Rework the UndoManager to something like the following, if it had a property.
        //  this.executable.bind(frame.getCurrentBasePanel().getUndoManager().canUndo())
        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }
}
