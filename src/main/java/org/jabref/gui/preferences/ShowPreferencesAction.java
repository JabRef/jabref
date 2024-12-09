package org.jabref.gui.preferences;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.actions.SimpleCommand;

public class ShowPreferencesAction extends SimpleCommand {

    private final LibraryTabContainer tabContainer;
    private final Class<? extends PreferencesTab> preferencesTabToSelectClass;

    private final DialogService dialogService;

    public ShowPreferencesAction(LibraryTabContainer tabContainer, DialogService dialogService) {
        this(tabContainer, null, dialogService);
    }

    public ShowPreferencesAction(LibraryTabContainer tabContainer, Class<? extends PreferencesTab> preferencesTabToSelectClass, DialogService dialogService) {
        this.tabContainer = tabContainer;
        this.preferencesTabToSelectClass = preferencesTabToSelectClass;
        this.dialogService = dialogService;
    }

    @Override
    public void execute() {
        dialogService.showCustomDialogAndWait(new PreferencesDialogView(preferencesTabToSelectClass));
        tabContainer.refresh();
    }
}
