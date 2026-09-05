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

/// Re-applies the last change undone in the library the user is looking at.
///
/// Takes the journal from the active library for the same reason as [UndoAction].
public class RedoAction extends SimpleCommand {
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;

    public RedoAction(Supplier<LibraryTab> tabSupplier, DialogService dialogService, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;

        BooleanExpression activeLibraryHasRedo = BooleanExpression.booleanExpression(
                stateManager.activeTabProperty().flatMap(
                        optionalTab -> optionalTab
                                .map(libraryTab -> libraryTab.getGuiUndoManager().redoableProperty())
                                .orElse(new SimpleBooleanProperty(false))));

        this.executable.bind(needsDatabase(stateManager).and(activeLibraryHasRedo));
    }

    @Override
    public void execute() {
        LibraryTab libraryTab = tabSupplier.get();
        GuiUndoManager undoManager = libraryTab.getGuiUndoManager();

        if (undoManager.canRedo()) {
            undoManager.redo();
            dialogService.notify(Localization.lang("Redo"));
        } else {
            dialogService.notify(Localization.lang("Nothing to redo") + '.');
        }
        libraryTab.markChangedOrUnChanged();
    }
}
