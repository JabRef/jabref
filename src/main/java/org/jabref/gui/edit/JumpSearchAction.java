package org.jabref.gui.edit;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.function.Supplier;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

public class JumpSearchAction extends SimpleCommand {
    private final Supplier<LibraryTab> tabSupplier;
    private final DialogService dialogService;

    public JumpSearchAction(Supplier<LibraryTab> tabSupplier, StateManager stateManager, DialogService dialogService) {
        this.tabSupplier = tabSupplier;
        this.dialogService = dialogService;
        this.executable.bind(ActionHelper.needsDatabase(stateManager));
    }

    private static final Logger logger = Logger.getLogger(JumpSearchAction.class.getName());

    public static void main(String[] args) {
        // Log a simple INFO message
        logger.info("Application has started.");

    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new JumpSearchView(tabSupplier.get()));
    }
}
