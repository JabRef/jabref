package net.sf.jabref.gui.importer.actions;

import java.util.Arrays;
import java.util.Iterator;

import javax.swing.JOptionPane;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.preferences.JabRefPreferences;

/**
 * This action checks whether any new custom entry types were loaded from this
 * BIB file. If so, an offer to remember these entry types is given.
 */
public class CheckForNewEntryTypesAction implements PostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult pr) {
        Defaults defaults = new Defaults(BibDatabaseMode.fromPreference(Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)));
        BibDatabaseMode mode = new BibDatabaseContext(pr.getDatabase(), pr.getMetaData(), defaults).getMode();
        // See if any custom entry types were imported, but disregard those we already know:
        for (Iterator<String> i = pr.getEntryTypes().keySet().iterator(); i.hasNext();) {
            String typeName = i.next().toLowerCase();
            if (EntryTypes.getType(typeName, mode).isPresent()) {
                i.remove();
            }
        }
        return !pr.getEntryTypes().isEmpty();
    }

    @Override
    public void performAction(BasePanel panel, ParserResult pr) {

        StringBuilder sb = new StringBuilder();
        sb.append(Localization.lang("Custom entry types found in file")).append(": ");
        Object[] types = pr.getEntryTypes().keySet().toArray();
        Arrays.sort(types);
        for (Object type : types) {
            sb.append(type).append(", ");
        }
        String s = sb.toString();
        int answer = JOptionPane.showConfirmDialog(panel.frame(),
                s.substring(0, s.length() - 2) + ".\n"
                        + Localization.lang("Remember these entry types?"),
                Localization.lang("Custom entry types"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (answer == JOptionPane.YES_OPTION) {
            // Import
            for (EntryType typ : pr.getEntryTypes().values()) {
                EntryTypes.addOrModifyCustomEntryType((CustomEntryType) typ);
            }
        }
    }
}
