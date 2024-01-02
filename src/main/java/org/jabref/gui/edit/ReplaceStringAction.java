package org.jabref.gui.edit;

import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

public class ReplaceStringAction extends SimpleCommand {
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;

    public ReplaceStringAction(Supplier<LibraryTab> tabSupplier, StateManager stateManager, DialogService dialogService) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;

        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new ReplaceStringView(tabSupplier.get()));
    }
}
