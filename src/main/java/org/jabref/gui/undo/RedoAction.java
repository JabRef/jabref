package org.jabref.gui.undo;

import java.util.Optional;
import java.util.function.Supplier;

import javax.swing.undo.CannotRedoException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
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

        ChangeListener<Optional<LibraryTab>> listener = (observable, oldValue, activeLibraryTab) -> {
            activeLibraryTab.ifPresent(libraryTab ->
                    this.executable.bind(libraryTab.getUndoManager().getRedoableProperty()));

            oldValue.ifPresent(libraryTab -> this.executable.unbind());
        };

        WeakChangeListener<Optional<LibraryTab>> weakListener = new WeakChangeListener<>(listener);
        stateManager.activeTabProperty().addListener(weakListener);
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
