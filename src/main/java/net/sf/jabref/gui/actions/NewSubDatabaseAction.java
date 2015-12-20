package net.sf.jabref.gui.actions;

import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.util.PositionWindow;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.wizard.auximport.gui.FromAuxDialog;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * The action concerned with generate a new (sub-)database from latex aux file.
 */
public class NewSubDatabaseAction extends MnemonicAwareAction {

    private final JabRefFrame jabRefFrame;

    public NewSubDatabaseAction(JabRefFrame jabRefFrame) {
        super(IconTheme.JabRefIcon.NEW.getIcon());
        this.jabRefFrame = jabRefFrame;
        putValue(Action.NAME, Localization.menuTitle("New subdatabase based on AUX file"));
        putValue(Action.SHORT_DESCRIPTION, Localization.lang("New BibTeX subdatabase"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Create a new, empty, database.

        FromAuxDialog dialog = new FromAuxDialog(jabRefFrame, "", true, jabRefFrame.tabbedPane);

        PositionWindow.placeDialog(dialog, jabRefFrame);
        dialog.setVisible(true);

        if (dialog.generatePressed()) {
            BasePanel bp = new BasePanel(jabRefFrame,
                    dialog.getGenerateDB(), // database
                    null, // file
                    new MetaData(), Globals.prefs.getDefaultEncoding()); // meta data
            jabRefFrame.tabbedPane.add(GUIGlobals.untitledTitle, bp);
            jabRefFrame.tabbedPane.setSelectedComponent(bp);
            jabRefFrame.output(Localization.lang("New database created."));
        }
    }
}
