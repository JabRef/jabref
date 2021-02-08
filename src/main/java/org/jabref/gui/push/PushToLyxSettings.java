package org.jabref.gui.push;

import javafx.beans.property.ObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.PushToApplicationPreferences;

public class PushToLyxSettings extends PushToApplicationSettings {

    public PushToLyxSettings(PushToApplication application,
                             DialogService dialogService,
                             PreferencesService preferencesService,
                             ObjectProperty<PushToApplicationPreferences> preferences) {
        super(application, dialogService, preferencesService, preferences);

        commandLabel.setText(Localization.lang("Path to LyX pipe") + ":");
    }
}
