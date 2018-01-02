package org.jabref.gui.customentrytypes;

import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jabref.Globals;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;
import org.jabref.preferences.JabRefPreferences;

/**
 * This class extends FieldSetComponent to provide some required functionality for the
 * list of entry types in EntryCustomizationDialog.
 * @author alver
 */
public class EntryTypeList extends FieldSetComponent implements ListSelectionListener {

    private final JButton def = new JButton(Localization.lang("Default"));
    private final BibDatabaseMode mode;

    /** Creates a new instance of EntryTypeList */
    public EntryTypeList(List<String> fields, BibDatabaseMode mode) {
        super(Localization.lang("Entry types"), fields, false, true);
        this.mode = mode;

        con.gridx = 0;
        con.gridy = 2;
        con.fill = GridBagConstraints.VERTICAL;
        con.anchor = GridBagConstraints.EAST;
        gbl.setConstraints(def, con);
        add(def);
        list.addListSelectionListener(this);
        def.addActionListener(e -> def.setEnabled(false));
        def.setEnabled(false);
        remove.setEnabled(false);
    }

    @Override
    protected void addField(String str) {
        String s = str.trim();
        if (forceLowerCase) {
            s = s.toLowerCase(Locale.ROOT);
        }
        if ("".equals(s) || listModel.contains(s)) {
            return;
        }

        String testString = BibtexKeyGenerator.cleanKey(s,
                Globals.prefs.getBoolean(JabRefPreferences.ENFORCE_LEGAL_BIBTEX_KEY));
        if (!testString.equals(s) || (s.indexOf('&') >= 0)) {
            // Report error and exit.
            JOptionPane.showMessageDialog(this, Localization.lang("Entry type names are not allowed to contain white space or the following "
                            + "characters") + ": # { } ~ , ^ &",
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        else if ("comment".equalsIgnoreCase(s)) {
            // Report error and exit.
            JOptionPane.showMessageDialog(this, Localization.lang("The name 'comment' cannot be used as an entry type name."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        addFieldUncritically(s);
    }

    @Override
    protected void removeSelected() {
        //super.removeSelected();
        int[] selected = list.getSelectedIndices();
        if (selected.length > 0) {
            changesMade = true;
        }
        for (int i = 0; i < selected.length; i++) {
            String typeName = listModel.get(selected[selected.length - 1 - i]);
            Optional<EntryType> type = EntryTypes.getType(typeName, this.mode);

            // If it is a custom entry type, we can remove it. If type == null, it means
            // the user must have added it and not yet applied it, so we can remove it
            // in this case as well. If it is a standard type it cannot be removed.
            if (type.isPresent() && (type.get() instanceof CustomEntryType)) {
                listModel.removeElementAt(selected[selected.length - 1 - i]);
            } else {
                // This shouldn't happen, since the Remove button should be disabled.
                JOptionPane.showMessageDialog(null, Localization.lang("This entry type cannot be removed."),
                        Localization.lang("Remove entry type"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        // Do nothing
    }

    public void enable(String typeName, boolean isChanged) {
        //String s = (String)list.getSelectedValue();

        if (EntryTypes.getStandardType(typeName, mode).isPresent()) {
            Optional<EntryType> entryType = EntryTypes.getType(typeName, mode);
            if (isChanged || (entryType.isPresent() && (entryType.get() instanceof CustomEntryType))) {
                def.setEnabled(true);
            } else {
                def.setEnabled(false);
            }

            remove.setEnabled(false);
        } else {
            def.setEnabled(false);
            remove.setEnabled(true);
        }
    }

    public void addDefaultActionListener(ActionListener l) {
        def.addActionListener(l);
    }

    @Override
    public void setEnabled(boolean en) {
        super.setEnabled(en);
        def.setEnabled(en);
    }
}
