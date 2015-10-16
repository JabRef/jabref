package net.sf.jabref.gui.actions;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.util.Util;
import net.sf.jabref.wizard.integrity.gui.IntegrityWizard;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * The action should test the database and report errors/warnings
 */
public class IntegrityCheckAction extends AbstractAction {

    private JabRefFrame jabRefFrame;

    public IntegrityCheckAction(JabRefFrame jabRefFrame) {
        super(Localization.menuTitle("Integrity check"),
                IconTheme.JabRefIcon.INTEGRITY_CHECK.getIcon());
        this.jabRefFrame = jabRefFrame;
        //putValue( SHORT_DESCRIPTION, "integrity" ) ;  //Globals.lang( "integrity" ) ) ;
        //putValue(MNEMONIC_KEY, GUIGlobals.newKeyCode);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object selComp = jabRefFrame.tabbedPane.getSelectedComponent();
        if (selComp != null) {
            BasePanel bp = (BasePanel) selComp;
            BibtexDatabase refBase = bp.getDatabase();
            if (refBase != null) {
                IntegrityWizard wizard = new IntegrityWizard(jabRefFrame, jabRefFrame.basePanel());
                Util.placeDialog(wizard, jabRefFrame);
                wizard.setVisible(true);

            }
        }
    }
}
