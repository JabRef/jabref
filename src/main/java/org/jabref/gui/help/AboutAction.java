package org.jabref.gui.help;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

public class AboutAction extends SimpleCommand {

    private final AboutDialogView aboutDialogView;

    public AboutAction() {
        this.aboutDialogView = new AboutDialogView();
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialog(aboutDialogView);
    }

    public AboutDialogView getAboutDialogView() {
        return aboutDialogView;
    }
}
