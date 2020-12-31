package org.jabref.gui.importer;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

public class ManageCustomImportsAction extends SimpleCommand {

    public ManageCustomImportsAction() {
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialog(new ImportCustomizationDialog());
    }
}
