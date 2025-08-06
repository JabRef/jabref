package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

public class AboutAction extends SimpleCommand {

    private final AboutDialogView aboutDialogView;
    private final DialogService dialogService;

    public AboutAction() {
        this.aboutDialogView = new AboutDialogView();
        this.dialogService = Injector.instantiateModelOrService(DialogService.class);
    }

    @Override
    public void execute() {
        dialogService.showCustomDialog(aboutDialogView);
    }
}
