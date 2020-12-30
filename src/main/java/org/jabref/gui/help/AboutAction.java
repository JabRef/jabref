package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

public class AboutAction extends SimpleCommand {
    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        AboutDialogView aboutDialogView = new AboutDialogView();
        dialogService.show(aboutDialogView);
    }
}
