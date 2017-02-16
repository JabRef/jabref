package org.jabref.gui.renderer;

import org.jabref.Globals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

public class IncompleteRenderer extends GeneralRenderer {

    public IncompleteRenderer() {
        super(Globals.prefs.getColor(JabRefPreferences.INCOMPLETE_ENTRY_BACKGROUND));
    }

    public void setNumber(int number) {
        super.setValue(String.valueOf(number + 1));
        setToolTipText(Localization.lang("This entry is incomplete"));
    }

    @Override
    protected void setValue(Object value) {
        // do not support normal behaviour
    }
}
