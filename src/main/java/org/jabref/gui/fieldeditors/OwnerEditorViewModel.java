package org.jabref.gui.fieldeditors;

import org.jabref.preferences.JabRefPreferences;

public class OwnerEditorViewModel extends AbstractEditorViewModel {
    private final JabRefPreferences preferences;

    public OwnerEditorViewModel(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public void setOwner() {
        text.set(preferences.get(JabRefPreferences.DEFAULT_OWNER));
    }
}
