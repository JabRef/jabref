/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.labelpattern;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.labelpattern.AbstractLabelPattern;
import net.sf.jabref.logic.labelpattern.DatabaseLabelPattern;
import net.sf.jabref.logic.labelpattern.GlobalLabelPattern;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.EntryType;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class LabelPatternPanel extends JPanel {

    // used by both LabelPatternPanel and TabLabelPAttern
    protected final GridBagLayout gbl = new GridBagLayout();
    protected final GridBagConstraints con = new GridBagConstraints();

    private final HelpAction help;

    // default pattern
    protected final JTextField defaultPat = new JTextField();

    // one field for each type
    private final Map<String, JTextField> textFields = new HashMap<>();
    private final BasePanel panel;


    public LabelPatternPanel(BasePanel panel) {
        this.panel = panel;
        help = new HelpAction(Localization.lang("Help on key patterns"), HelpFiles.labelPatternHelp);
        buildGUI();
    }

    private void buildGUI() {
        JPanel pan = new JPanel();
        JScrollPane sp = new JScrollPane(pan);
        sp.setPreferredSize(new Dimension(100, 100));
        sp.setBorder(BorderFactory.createEmptyBorder());
        pan.setLayout(gbl);
        setLayout(gbl);
        // The header - can be removed
        JLabel lblEntryType = new JLabel(Localization.lang("Entry type"));
        Font f = new Font("plain", Font.BOLD, 12);
        lblEntryType.setFont(f);
        con.gridx = 0;
        con.gridy = 0;
        con.gridwidth = 1;
        con.gridheight = 1;
        con.fill = GridBagConstraints.VERTICAL;
        con.anchor = GridBagConstraints.WEST;
        con.insets = new Insets(5, 5, 10, 0);
        gbl.setConstraints(lblEntryType, con);
        pan.add(lblEntryType);

        JLabel lblKeyPattern = new JLabel(Localization.lang("Key pattern"));
        lblKeyPattern.setFont(f);
        con.gridx = 1;
        con.gridy = 0;
        con.gridheight = 1;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.anchor = GridBagConstraints.WEST;
        con.insets = new Insets(5, 5, 10, 5);
        gbl.setConstraints(lblKeyPattern, con);
        pan.add(lblKeyPattern);

        con.gridy = 1;
        con.gridx = 0;
        JLabel lab = new JLabel(Localization.lang("Default pattern"));
        gbl.setConstraints(lab, con);
        pan.add(lab);
        con.gridx = 1;
        gbl.setConstraints(defaultPat, con);
        pan.add(defaultPat);
        con.insets = new Insets(5, 5, 10, 5);
        JButton btnDefault = new JButton(Localization.lang("Default"));
        btnDefault.addActionListener(
                e -> defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_LABEL_PATTERN)));
        con.gridx = 2;
        int y = 2;
        gbl.setConstraints(btnDefault, con);
        pan.add(btnDefault);

        BibDatabaseMode mode;
        // check mode of currently used DB
        if (panel != null) {
            mode = panel.getBibDatabaseContext().getMode();
        } else { // use preferences value if no DB is open
            if (Globals.prefs.getBoolean(JabRefPreferences.BIBLATEX_DEFAULT_MODE)) {
                mode = BibDatabaseMode.BIBLATEX;
            } else {
                mode = BibDatabaseMode.BIBTEX;
            }
        }
        for (EntryType type : EntryTypes.getAllValues(mode)) {
            textFields.put(type.getName().toLowerCase(), addEntryType(pan, type, y));
            y++;
        }

        con.fill = GridBagConstraints.BOTH;
        con.gridx = 0;
        con.gridy = 1;
        con.gridwidth = 3;
        con.weightx = 1;
        con.weighty = 1;
        gbl.setConstraints(sp, con);
        add(sp);

        // A help button
        con.gridwidth = 1;
        con.gridx = 1;
        con.gridy = 2;
        con.fill = GridBagConstraints.HORIZONTAL;
        //
        con.weightx = 0;
        con.weighty = 0;
        con.anchor = GridBagConstraints.SOUTHEAST;
        con.insets = new Insets(0, 5, 0, 5);
        JButton hlb = new JButton(IconTheme.JabRefIcon.HELP.getSmallIcon());
        hlb.setToolTipText(Localization.lang("Help on key patterns"));
        gbl.setConstraints(hlb, con);
        add(hlb);
        hlb.addActionListener(help);

        // And finally a button to reset everything
        JButton btnDefaultAll = new JButton(Localization.lang("Reset all"));
        con.gridx = 2;
        con.gridy = 2;

        con.weightx = 1;
        con.weighty = 0;
        con.anchor = GridBagConstraints.SOUTHEAST;
        con.insets = new Insets(20, 5, 0, 5);
        gbl.setConstraints(btnDefaultAll, con);
        btnDefaultAll.addActionListener(e -> {
            // reset all fields
            for (JTextField field : textFields.values()) {
                field.setText("");
            }

            // also reset the default pattern
            defaultPat.setText((String) Globals.prefs.defaults.get(JabRefPreferences.DEFAULT_LABEL_PATTERN));
        });
        add(btnDefaultAll);
    }

    private JTextField addEntryType(Container c, EntryType type, int y) {

        JLabel lab = new JLabel(type.getName());
        con.gridx = 0;
        con.gridy = y;
        con.fill = GridBagConstraints.BOTH;
        con.weightx = 0;
        con.weighty = 0;
        con.anchor = GridBagConstraints.WEST;
        con.insets = new Insets(0, 5, 0, 5);
        gbl.setConstraints(lab, con);
        c.add(lab);

        JTextField tf = new JTextField();
        tf.setColumns(15);
        con.gridx = 1;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.weightx = 1;
        con.weighty = 0;
        con.anchor = GridBagConstraints.CENTER;
        con.insets = new Insets(0, 5, 0, 5);
        gbl.setConstraints(tf, con);
        c.add(tf);

        JButton but = new JButton(Localization.lang("Default"));
        con.gridx = 2;
        con.fill = GridBagConstraints.BOTH;
        con.weightx = 0;
        con.weighty = 0;
        con.anchor = GridBagConstraints.CENTER;
        con.insets = new Insets(0, 5, 0, 5);
        gbl.setConstraints(but, con);
        but.setActionCommand(type.getName().toLowerCase());
        but.addActionListener(e -> {
            JTextField tField = textFields.get(e.getActionCommand());
            tField.setText("");
        });
        c.add(but);

        return tf;
    }

    /**
     * fill the given LabelPattern by values generated from the text fields
     */
    private void fillPatternUsingPanelData(AbstractLabelPattern keypatterns) {
        // each entry type
        for (Map.Entry<String, JTextField> entry : textFields.entrySet()) {
            String text = entry.getValue().getText();
            if (!text.trim().isEmpty()) {
                keypatterns.addLabelPattern(entry.getKey(), text);
            }
        }

        // default value
        String text = defaultPat.getText();
        if (!text.trim().isEmpty()) { // we do not trim the value at the assignment to enable users to have spaces at the beginning and at the end of the pattern
            keypatterns.setDefaultValue(text);
        }
    }

    protected GlobalLabelPattern getLabelPatternAsGlobalLabelPattern() {
        GlobalLabelPattern res = new GlobalLabelPattern();
        fillPatternUsingPanelData(res);
        return res;
    }

    public DatabaseLabelPattern getLabelPatternAsDatabaseLabelPattern() {
        DatabaseLabelPattern res = new DatabaseLabelPattern();
        fillPatternUsingPanelData(res);
        return res;
    }

    /**
     * Fills the current values to the text fields
     *
     * @param keypatterns the LabelPattern to use as initial value
     */
    public void setValues(AbstractLabelPattern keypatterns) {
        for (Map.Entry<String, JTextField> entry : textFields.entrySet()) {
            setValue(entry.getValue(), entry.getKey(), keypatterns);
        }

        if (keypatterns.getDefaultValue() == null) {
            defaultPat.setText("");
        } else {
            defaultPat.setText(keypatterns.getDefaultValue().get(0));
        }
    }

    private static void setValue(JTextField tf, String fieldName, AbstractLabelPattern keypatterns) {
        if (keypatterns.isDefaultValue(fieldName)) {
            tf.setText("");
        } else {
            tf.setText(keypatterns.getValue(fieldName).get(0));
        }
    }

}
