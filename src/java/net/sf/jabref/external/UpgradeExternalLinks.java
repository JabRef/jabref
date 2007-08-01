package net.sf.jabref.external;

import javax.swing.JOptionPane;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;

/**
 * Action for upgrading old-style (pre 2.3) PS/PDF links to the new "file" field.
 */
public class UpgradeExternalLinks extends BaseAction {

    private BasePanel panel;

    public UpgradeExternalLinks(BasePanel panel) {

        this.panel = panel;
    }

    public void action() throws Throwable {

        int answer = JOptionPane.showConfirmDialog(panel.frame(),
                Globals.lang("This will move all external links from the 'pdf' and 'ps' fields "
                    +"into the '%0' field. Proceed?", GUIGlobals.FILE_FIELD), Globals.lang("Upgrade external links"),
                JOptionPane.YES_NO_OPTION);
        if (answer !=  JOptionPane.YES_OPTION)
            return;
        NamedCompound ce = Util.upgradePdfPsToFile(panel.database(), new String[] {"pdf", "ps"});
        panel.undoManager.addEdit(ce);
        panel.markBaseChanged();
        panel.output(Globals.lang("Upgraded links."));
    }
}
