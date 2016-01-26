package net.sf.jabref.gui.actions;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseType;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class SwitchBibtexModeAction extends AbstractAction {
    public SwitchBibtexModeAction() {
        putValue(Action.NAME, Localization.menuTitle("Switch to %0 mode", getOppositeMode()));
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
        if(isBiblatexMode()) {
            // to BibteX
            Globals.prefs.putBoolean(JabRefPreferences.BIBLATEX_MODE, false);
        } else {
            // to Biblatex
            Globals.prefs.putBoolean(JabRefPreferences.BIBLATEX_MODE, true);
        }
        // update menu label
        putValue(Action.NAME, Localization.menuTitle("Switch to %0 mode", getOppositeMode()));
    }

    private boolean isBiblatexMode() {
        return Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE);
    }

    private String getOppositeMode() {
        return isBiblatexMode() ? "BibTeX" : "BibLaTeX";
    }

    private String getMode() {
        return isBiblatexMode() ? "BibLaTeX" : "BibTeX";
    }
}
