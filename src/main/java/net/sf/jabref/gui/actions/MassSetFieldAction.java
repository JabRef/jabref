/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui.actions;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.undo.UndoableEdit;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

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
    private JComboBox<String> field;
    private JTextField text;
    private JTextField renameTo;
    private boolean canceled = true;
    private JCheckBox overwrite;


    public MassSetFieldAction(JabRefFrame frame) {
        putValue(Action.NAME, Localization.menuTitle("Set/clear/rename fields") + "...");
        this.frame = frame;
    }

    private void createDialog() {
        diag = new JDialog(frame, Localization.lang("Set/clear/rename fields"), true);

        field = new JComboBox<>();
        field.setEditable(true);
        text = new JTextField();
        text.setEnabled(false);
        renameTo = new JTextField();
        renameTo.setEnabled(false);

        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));

        all = new JRadioButton(Localization.lang("All entries"));
        selected = new JRadioButton(Localization.lang("Selected entries"));
        clear = new JRadioButton(Localization.lang("Clear fields"));
        set = new JRadioButton(Localization.lang("Set fields"));
        rename = new JRadioButton(Localization.lang("Rename field to") + ":");
        rename.setToolTipText(Localization.lang("Move contents of a field into a field with a different name"));

        Set<String> allFields = frame.getCurrentBasePanel().getDatabase().getAllVisibleFields();

        for (String f : allFields) {
            field.addItem(f);
        }

        set.addChangeListener(e ->
        // Entering a text is only relevant if we are setting, not clearing:
        text.setEnabled(set.isSelected()));

        clear.addChangeListener(e ->
        // Overwrite protection makes no sense if we are clearing the field:
        overwrite.setEnabled(!clear.isSelected()));

        rename.addChangeListener(e ->
        // Entering a text is only relevant if we are renaming
        renameTo.setEnabled(rename.isSelected()));

        overwrite = new JCheckBox(Localization.lang("Overwrite existing field values"), true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(all);
        bg.add(selected);
        bg = new ButtonGroup();
        bg.add(clear);
        bg.add(set);
        bg.add(rename);
        FormBuilder builder = FormBuilder.create().layout(new FormLayout(
                "left:pref, 4dlu, fill:100dlu:grow", "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref"));
        builder.addSeparator(Localization.lang("Field name")).xyw(1, 1, 3);
        builder.add(Localization.lang("Field name")).xy(1, 3);
        builder.add(field).xy(3, 3);
        builder.addSeparator(Localization.lang("Include entries")).xyw(1, 5, 3);
        builder.add(all).xyw(1, 7, 3);
        builder.add(selected).xyw(1, 9, 3);
        builder.addSeparator(Localization.lang("New field value")).xyw(1, 11, 3);
        builder.add(set).xy(1, 13);
        builder.add(text).xy(3, 13);
        builder.add(clear).xyw(1, 15, 3);
        builder.add(rename).xy(1, 17);
        builder.add(renameTo).xy(3, 17);
        builder.add(overwrite).xyw(1, 19, 3);

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

        ok.addActionListener(e -> {
            // Check if the user tries to rename multiple fields:
            if (rename.isSelected()) {
                String[] fields = getFieldNames((String) field.getSelectedItem());
                if (fields.length > 1) {
                    JOptionPane.showMessageDialog(diag, Localization.lang("You can only rename one field at a time"),
                            "", JOptionPane.ERROR_MESSAGE);
                    return; // Do not close the dialog.
                }
            }
            canceled = false;
            diag.dispose();
        });

        Action cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                canceled = true;
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelAction);

        // Key bindings:
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
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
        BasePanel bp = frame.getCurrentBasePanel();
        if (bp == null) {
            return;
        }
        List<BibEntry> entries = bp.getSelectedEntries();
        // Lazy creation of the dialog:
        if (diag == null) {
            createDialog();
        }
        canceled = true;
        prepareDialog(!entries.isEmpty());
        if (diag != null) {
            diag.setLocationRelativeTo(frame);
            diag.setVisible(true);
        }
        if (canceled) {
            return;
        }

        Collection<BibEntry> entryList;
        // If all entries should be treated, change the entries array:
        if (all.isSelected()) {
            entryList = bp.getDatabase().getEntries();
        } else {
            entryList = entries;
        }
        String toSet = text.getText();
        if (toSet.isEmpty()) {
            toSet = null;
        }
        String[] fields = getFieldNames(((String) field.getSelectedItem()).trim().toLowerCase());
        NamedCompound ce = new NamedCompound(Localization.lang("Set field"));
        if (rename.isSelected()) {
            if (fields.length > 1) {
                JOptionPane.showMessageDialog(diag, Localization.lang("You can only rename one field at a time"), "",
                        JOptionPane.ERROR_MESSAGE);
                return; // Do not close the dialog.
            } else {
                ce.addEdit(MassSetFieldAction.massRenameField(entryList, fields[0], renameTo.getText(),
                        overwrite.isSelected()));
            }
        } else {
            for (String field1 : fields) {
                ce.addEdit(MassSetFieldAction.massSetField(entryList, field1,
                        set.isSelected() ? toSet : null,
                                overwrite.isSelected()));
            }
        }
        ce.end();
        bp.undoManager.addEdit(ce);
        bp.markBaseChanged();
    }

    /**
     * Set a given field to a given value for all entries in a Collection. This method DOES NOT update any UndoManager,
     * but returns a relevant CompoundEdit that should be registered by the caller.
     *
     * @param entries         The entries to set the field for.
     * @param field           The name of the field to set.
     * @param text            The value to set. This value can be null, indicating that the field should be cleared.
     * @param overwriteValues Indicate whether the value should be set even if an entry already has the field set.
     * @return A CompoundEdit for the entire operation.
     */
    private static UndoableEdit massSetField(Collection<BibEntry> entries, String field, String text,
            boolean overwriteValues) {

        NamedCompound ce = new NamedCompound(Localization.lang("Set field"));
        for (BibEntry entry : entries) {
            String oldVal = entry.getField(field);
            // If we are not allowed to overwrite values, check if there is a
            // nonempty
            // value already for this entry:
            if (!overwriteValues && (oldVal != null) && !oldVal.isEmpty()) {
                continue;
            }
            if (text == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, text);
            }
            ce.addEdit(new UndoableFieldChange(entry, field, oldVal, text));
        }
        ce.end();
        return ce;
    }

    /**
     * Move contents from one field to another for a Collection of entries.
     *
     * @param entries         The entries to do this operation for.
     * @param field           The field to move contents from.
     * @param newField        The field to move contents into.
     * @param overwriteValues If true, overwrites any existing values in the new field. If false, makes no change for
     *                        entries with existing value in the new field.
     * @return A CompoundEdit for the entire operation.
     */
    private static UndoableEdit massRenameField(Collection<BibEntry> entries, String field, String newField,
            boolean overwriteValues) {
        NamedCompound ce = new NamedCompound(Localization.lang("Rename field"));
        for (BibEntry entry : entries) {
            String valToMove = entry.getField(field);
            // If there is no value, do nothing:
            if ((valToMove == null) || valToMove.isEmpty()) {
                continue;
            }
            // If we are not allowed to overwrite values, check if there is a
            // non-empty value already for this entry for the new field:
            String valInNewField = entry.getField(newField);
            if (!overwriteValues && (valInNewField != null) && !valInNewField.isEmpty()) {
                continue;
            }

            entry.setField(newField, valToMove);
            ce.addEdit(new UndoableFieldChange(entry, newField, valInNewField, valToMove));
            entry.clearField(field);
            ce.addEdit(new UndoableFieldChange(entry, field, valToMove, null));
        }
        ce.end();
        return ce;
    }

    private static String[] getFieldNames(String s) {
        return s.split("[\\s;,]");
    }
}
