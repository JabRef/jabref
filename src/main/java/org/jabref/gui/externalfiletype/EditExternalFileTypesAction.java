package org.jabref.gui.externalfiletype;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

public class EditExternalFileTypesAction extends SimpleCommand {

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new CustomizeExternalFileTypesDialog());
    }
}
