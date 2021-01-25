package org.jabref.gui;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.SimplePreferencesDialog;
import org.jabref.gui.preferences.entryeditortabs.CustomizeGeneralFieldsTabView;

import com.airhacks.afterburner.injection.Injector;

public class SetupGeneralFieldsAction extends SimpleCommand {

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialogAndWait(new SimplePreferencesDialog(new CustomizeGeneralFieldsTabView()));
    }
}
