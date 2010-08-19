package net.sf.jabref.label;

import net.sf.jabref.BasePanel;
import net.sf.jabref.Globals;
import net.sf.jabref.imports.ParserResult;
import net.sf.jabref.imports.PostOpenAction;
import net.sf.jabref.labelPattern.SearchFixDuplicateLabels;

import javax.swing.*;

/**
 * PostOpenAction that checks whether there are warnings about duplicate BibTeX keys, and
 * if so, offers to start the duplicate resolving process.
 */
public class HandleDuplicateWarnings implements PostOpenAction {


    public boolean isActionNecessary(ParserResult pr) {
        return pr.hasDuplicateKeys();
    }

    public void performAction(BasePanel panel, ParserResult pr) {
        int answer = JOptionPane.showConfirmDialog(null,
                "<html><p>"+Globals.lang("This database contains one or more duplicated BibTeX keys.")
                +"</p><p>"+Globals.lang("Do you want to resolve duplicate keys now?"),
                Globals.lang("Duplicate BibTeX key"), JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            panel.runCommand("resolveDuplicateKeys");
        }
    }
}
