/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.util;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.*;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.undo.NamedCompound;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibtexEntry;

/**
 * An Action for launching mass field.
 *
 * Functionality:
 * * Defaults to selected entries, or all entries if none are selected.
 * * Input field name
 * * Either set field, or clear field.
 */
public class MassSetFieldAction extends MnemonicAwareAction {

    private final JabRefFrame frame;
    private JDialog diag;
    private JRadioButton all;
    private JRadioButton selected;
    private JRadioButton clear;
    private JRadioButton set;
    private JRadioButton rename;
    private JTextField field;
    private JTextField text;
    private JTextField renameTo;
    private boolean cancelled = true;
    private JCheckBox overwrite;


    public MassSetFieldAction(JabRefFrame frame) {
        putValue(Action.NAME, Localization.menuTitle("Set/clear/rename fields"));
        this.frame = frame;
    }

    private void createDialog() {
        diag = new JDialog(frame, Localization.lang("Set/clear/rename fields"), true);

        field = new JTextField();
        text = new JTextField();
        text.setEnabled(false);
        renameTo = new JTextField();
        renameTo.setEnabled(false);

        JButton ok = new JButton(Localization.lang("Ok"));
        JButton cancel = new JButton(Localization.lang("Cancel"));

        all = new JRadioButton(Localization.lang("All entries"));
        selected = new JRadioButton(Localization.lang("Selected entries"));
        clear = new JRadioButton(Localization.lang("Clear fields"));
        set = new JRadioButton(Localization.lang("Set fields"));
        rename = new JRadioButton(Localization.lang("Rename field to:"));
        rename.setToolTipText(Localization.lang("Move contents of a field into a field with a different name"));
        set.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                // Entering a text is only relevant if we are setting, not clearing:
                text.setEnabled(set.isSelected());
            }
        });
        clear.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent event) {
                // Overwrite protection makes no sense if we are clearing the field:
                overwrite.setEnabled(!clear.isSelected());
            }
        });
        rename.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                // Entering a text is only relevant if we are renaming
                renameTo.setEnabled(rename.isSelected());
            }
        });
        overwrite = new JCheckBox(Localization.lang("Overwrite existing field values"), true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(all);
        bg.add(selected);
        bg = new ButtonGroup();
        bg.add(clear);
        bg.add(set);
        bg.add(rename);
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout(
                "left:pref, 4dlu, fill:100dlu", ""));
        builder.appendSeparator(Localization.lang("Field name"));
        builder.append(Localization.lang("Field name"));
        builder.append(field);
        builder.nextLine();
        builder.appendSeparator(Localization.lang("Include entries"));
        builder.append(all, 3);
        builder.nextLine();
        builder.append(selected, 3);
        builder.nextLine();
        builder.appendSeparator(Localization.lang("New field value"));
        builder.append(set);
        builder.append(text);
        builder.nextLine();
        builder.append(clear);
        builder.nextLine();
        builder.append(rename);
        builder.append(renameTo);
        builder.nextLine();
        builder.append(overwrite, 3);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();

        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Check if the user tries to rename multiple fields:
                if (rename.isSelected()) {
                    String[] fields = getFieldNames(field.getText());
                    if (fields.length > 1) {
                        JOptionPane.showMessageDialog(diag, Localization.lang("You can only rename one field at a time"),
                                "", JOptionPane.ERROR_MESSAGE);
                        return; // Do not close the dialog.
                    }
                }
                cancelled = false;
                diag.dispose();
            }
        });

        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                cancelled = true;
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelAction);

        // Key bindings:
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", cancelAction);
    }

    private void prepareDialog(boolean selection) {
        selected.setEnabled(selection);
        if (selection) {
            selected.setSelected(true);
        } else {
            all.setSelected(true);
        }
        // Make sure one of the following ones is selected:
        if (!set.isSelected() && !clear.isSelected() && !rename.isSelected()) {
            set.setSelected(true);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        BasePanel bp = frame.basePanel();
        if (bp == null) {
            return;
        }
        BibtexEntry[] entries = bp.getSelectedEntries();
        // Lazy creation of the dialog:
        if (diag == null) {
            createDialog();
        }
        cancelled = true;
        prepareDialog(entries.length > 0);
        Util.placeDialog(diag, frame);
        diag.setVisible(true);
        if (cancelled) {
            return;
        }

        Collection<BibtexEntry> entryList;
        // If all entries should be treated, change the entries array:
        if (all.isSelected()) {
            entryList = bp.database().getEntries();
        } else {
            entryList = Arrays.asList(entries);
        }
        String toSet = text.getText();
        if (toSet.isEmpty()) {
            toSet = null;
        }
        String[] fields = getFieldNames(field.getText().trim().toLowerCase());
        NamedCompound ce = new NamedCompound(Localization.lang("Set field"));
        if (rename.isSelected()) {
            if (fields.length > 1) {
                // TODO: message: can only rename a single field
            }
            else {
                ce.addEdit(Util.massRenameField(entryList, fields[0], renameTo.getText(),
                        overwrite.isSelected()));
            }
        } else {
            for (String field1 : fields) {
                ce.addEdit(Util.massSetField(entryList, field1,
                        set.isSelected() ? toSet : null,
                        overwrite.isSelected()));
            }
        }
        ce.end();
        bp.undoManager.addEdit(ce);
        bp.markBaseChanged();
    }

    private String[] getFieldNames(String s) {
        return s.split("[\\s;,]");
    }
}
