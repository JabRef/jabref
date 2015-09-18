package net.sf.jabref.gui.actions;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.*;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.util.Util;
import net.sf.jabref.wizard.auximport.gui.FromAuxDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * The action concerned with generate a new (sub-)database from latex aux file.
 */
public class NewSubDatabaseAction extends MnemonicAwareAction {

    private JabRefFrame jabRefFrame;

    public NewSubDatabaseAction(JabRefFrame jabRefFrame) {
        super(IconTheme.getImage("new"));
        this.jabRefFrame = jabRefFrame;
        putValue(Action.NAME, "New subdatabase based on AUX file");
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("New BibTeX subdatabase"));
        //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Create a new, empty, database.

        FromAuxDialog dialog = new FromAuxDialog(jabRefFrame, "", true, jabRefFrame.tabbedPane);

        Util.placeDialog(dialog, jabRefFrame);
        dialog.setVisible(true);

        if (dialog.generatePressed()) {
            BasePanel bp = new BasePanel(jabRefFrame,
                    dialog.getGenerateDB(), // database
                    null, // file
                    new MetaData(), Globals.prefs.get(JabRefPreferences.DEFAULT_ENCODING)); // meta data
            jabRefFrame.tabbedPane.add(Localization.lang(GUIGlobals.untitledTitle), bp);
            jabRefFrame.tabbedPane.setSelectedComponent(bp);
            jabRefFrame.output(Localization.lang("New database created."));
        }
    }
}
