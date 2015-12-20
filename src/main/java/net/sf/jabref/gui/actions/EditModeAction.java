package net.sf.jabref.gui.actions;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class EditModeAction extends AbstractAction {
    public EditModeAction() {
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
        // TODO: enable this change per file and without GUI restart
        JOptionPane.showMessageDialog(null,
                    Localization.lang("You have toggled the %0 mode.", getMode()).concat(" ")
                            .concat("You must restart JabRef for this change to come into effect."),
                    Localization.lang("%0 mode", getMode()), JOptionPane.WARNING_MESSAGE);
    }

    private boolean isBiblatexMode() {
        return Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_MODE);
    }

    private String getOppositeMode() {
        String mode = isBiblatexMode() ? "BibTeX" : "BibLaTeX";
        return mode;
    }

    private String getMode() {
        String mode = isBiblatexMode() ? "BibLaTeX" : "BibTeX";
        return mode;
    }
}