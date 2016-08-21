package net.sf.jabref.gui.importer.actions;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.actions.Actions;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;

/**
 * PostOpenAction that checks whether there are warnings about duplicate BibTeX keys, and
 * if so, offers to start the duplicate resolving process.
 */
public class HandleDuplicateWarnings implements PostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult pr) {
        return pr.hasDuplicateKeys();
    }

    @Override
    public void performAction(BasePanel panel, ParserResult pr) {
        int answer = JOptionPane.showConfirmDialog(null,
                "<html><p>" + Localization.lang("This database contains one or more duplicated BibTeX keys.")
                        + "</p><p>" + Localization.lang("Do you want to resolve duplicate keys now?"),
                Localization.lang("Duplicate BibTeX key"), JOptionPane.YES_NO_OPTION);
        if (answer == JOptionPane.YES_OPTION) {
            panel.runCommand(Actions.RESOLVE_DUPLICATE_KEYS);
        }
    }
}
