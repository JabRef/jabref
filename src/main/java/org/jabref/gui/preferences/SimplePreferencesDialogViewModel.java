package org.jabref.gui.preferences;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

public class SimplePreferencesDialogViewModel {
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final PreferencesTab preferencesTab;

    public SimplePreferencesDialogViewModel(DialogService dialogService,
                                            PreferencesService preferencesService,
                                            PreferencesTab preferencesTab) {
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.preferencesTab = preferencesTab;
    }

    public boolean validSettings() {
        return preferencesTab.validateSettings();
    }

    public void storeSettings() {
        if (!validSettings()) {
            return;
        }

        preferencesTab.storeSettings();
        List<String> restartWarnings = preferencesTab.getRestartWarnings();

        preferencesService.flush();

        if (!restartWarnings.isEmpty()) {
            dialogService.showWarningDialogAndWait(Localization.lang("Restart required"),
                    String.join(",\n", restartWarnings)
                            + "\n\n"
                            + Localization.lang("You must restart JabRef for this to come into effect."));
        }
    }

    public void setValues() {
        preferencesTab.setValues();
    }
}
