package net.sf.jabref.gui.renderer;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;

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
