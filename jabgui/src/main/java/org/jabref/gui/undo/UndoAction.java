package org.jabref.gui.undo;

import java.util.function.Supplier;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.l10n.Localization;

import static org.jabref.gui.actions.ActionHelper.needsDatabase;

/// Undoes the last change made to the library the user is looking at.
///
/// The journal is taken from the active library rather than held, because which journal this
/// acts on is a property of the moment the user presses the button, not of the moment the button
/// was built: one instance of this action serves every library the session opens.
public class UndoAction extends SimpleCommand {
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;

    public UndoAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;

        BooleanExpression activeLibraryHasUndo = BooleanExpression.booleanExpression(
                stateManager.activeTabProperty().flatMap(
                        optionalTab -> optionalTab
                                .map(libraryTab -> libraryTab.getGuiUndoManager().undoableProperty())
                                .orElse(new SimpleBooleanProperty(false))));

        this.executable.bind(needsDatabase(stateManager).and(activeLibraryHasUndo));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = tabSupplier.get();
        GuiUndoManager undoManager = libraryTab.getGuiUndoManager();

        if (undoManager.canUndo()) {
            undoManager.undo();
            dialogService.notify(Localization.lang("Undo"));
        } else {
            dialogService.notify(Localization.lang("Nothing to undo") + '.');
        }
        libraryTab.markChangedOrUnChanged();
    }
}
