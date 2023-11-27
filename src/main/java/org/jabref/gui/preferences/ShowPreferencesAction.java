package org.jabref.gui.preferences;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.SimpleCommand;

import com.airhacks.afterburner.injection.Injector;

public class ShowPreferencesAction extends SimpleCommand {

    private final JabRefFrame jabRefFrame;
    private final Class<? extends PreferencesTab> preferencesTabToSelectClass;

    public ShowPreferencesAction(JabRefFrame jabRefFrame) {
        this(jabRefFrame, null);
    }

    public ShowPreferencesAction(JabRefFrame jabRefFrame, Class<? extends PreferencesTab> preferencesTabToSelectClass) {
        this.jabRefFrame = jabRefFrame;
        this.preferencesTabToSelectClass = preferencesTabToSelectClass;
    }

    @Override
    public void execute() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        dialogService.showCustomDialog(new PreferencesDialogView(preferencesTabToSelectClass));

        // Refresh frame and tables
        jabRefFrame.getGlobalSearchBar().updateHintVisibility();
        jabRefFrame.setupAllTables();
        jabRefFrame.getLibraryTabs().forEach(panel -> panel.getMainTable().getTableModel().refresh());
    }
}
