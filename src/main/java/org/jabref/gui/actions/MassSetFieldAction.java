package org.jabref.gui.actions;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

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
    private JRadioButton append;
    private JRadioButton rename;
    private JComboBox<String> field;
    private JTextField textFieldSet;
    private JTextField textFieldAppend;
    private JTextField textFieldRename;
    private boolean canceled = true;
    private JCheckBox overwrite;


    public MassSetFieldAction(JabRefFrame frame) {
        putValue(Action.NAME, Localization.menuTitle("Set/clear/append/rename fields") + "...");
        this.frame = frame;
    }

    private void createDialog() {
        diag = new JDialog(frame, Localization.lang("Set/clear/append/rename fields"), true);

        field = new JComboBox<>();
        field.setEditable(true);
        textFieldSet = new JTextField();
        textFieldSet.setEnabled(false);
        textFieldAppend = new JTextField();
        textFieldAppend.setEnabled(false);
        textFieldRename = new JTextField();
        textFieldRename.setEnabled(false);

        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));

        all = new JRadioButton(Localization.lang("All entries"));
        selected = new JRadioButton(Localization.lang("Selected entries"));
        clear = new JRadioButton(Localization.lang("Clear fields"));
        set = new JRadioButton(Localization.lang("Set fields"));
        append = new JRadioButton(Localization.lang("Append to fields"));
        rename = new JRadioButton(Localization.lang("Rename field to") + ":");
        rename.setToolTipText(Localization.lang("Move contents of a field into a field with a different name"));

        Set<String> allFields = frame.getCurrentBasePanel().getDatabase().getAllVisibleFields();

        for (String f : allFields) {
            field.addItem(f);
        }

        set.addChangeListener(e ->
                // Entering a setText is only relevant if we are setting, not clearing:
                textFieldSet.setEnabled(set.isSelected()));

        append.addChangeListener(e -> {
            // Text to append is only required if we are appending:
            textFieldAppend.setEnabled(append.isSelected());
            // Overwrite protection makes no sense if we are appending to a field:
            overwrite.setEnabled(!clear.isSelected() && !append.isSelected());
        });

        clear.addChangeListener(e ->
                // Overwrite protection makes no sense if we are clearing the field:
                overwrite.setEnabled(!clear.isSelected() && !append.isSelected()));

        rename.addChangeListener(e ->
                // Entering a setText is only relevant if we are renaming
                textFieldRename.setEnabled(rename.isSelected()));

        overwrite = new JCheckBox(Localization.lang("Overwrite existing field values"), true);
        ButtonGroup bg = new ButtonGroup();
        bg.add(all);
        bg.add(selected);
        bg = new ButtonGroup();
        bg.add(clear);
        bg.add(set);
        bg.add(append);
        bg.add(rename);
        FormBuilder builder = FormBuilder.create().layout(new FormLayout(
                "left:pref, 4dlu, fill:100dlu:grow", "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref"));
        builder.addSeparator(Localization.lang("Field name")).xyw(1, 1, 3);
        builder.add(Localization.lang("Field name")).xy(1, 3);
        builder.add(field).xy(3, 3);
        builder.addSeparator(Localization.lang("Include entries")).xyw(1, 5, 3);
        builder.add(all).xyw(1, 7, 3);
        builder.add(selected).xyw(1, 9, 3);
        builder.addSeparator(Localization.lang("New field value")).xyw(1, 11, 3);
        builder.add(set).xy(1, 13);
        builder.add(textFieldSet).xy(3, 13);
        builder.add(clear).xyw(1, 15, 3);
        builder.add(append).xy(1, 17);
        builder.add(textFieldAppend).xy(3, 17);
        builder.add(rename).xy(1, 19);
        builder.add(textFieldRename).xy(3, 19);
        builder.add(overwrite).xyw(1, 21, 3);

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
            // Check that any field name is set
            String fieldText = (String) field.getSelectedItem();
            if ((fieldText == null) || fieldText.trim().isEmpty()) {
                JOptionPane.showMessageDialog(diag, Localization.lang("You must enter at least one field name"), "",
                        JOptionPane.ERROR_MESSAGE);
                return; // Do not close the dialog.
            }

            // Check if the user tries to rename multiple fields:
            if (rename.isSelected()) {
                String[] fields = getFieldNames(fieldText);
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

        String toSet = textFieldSet.getText();
        if (toSet.isEmpty()) {
            toSet = null;
        }

        String[] fields = getFieldNames(((String) field.getSelectedItem()).trim().toLowerCase(Locale.ROOT));
        NamedCompound compoundEdit = new NamedCompound(Localization.lang("Set field"));
        if (rename.isSelected()) {
            if (fields.length > 1) {
                JOptionPane.showMessageDialog(diag, Localization.lang("You can only rename one field at a time"), "",
                        JOptionPane.ERROR_MESSAGE);
                return; // Do not close the dialog.
            } else {
                compoundEdit.addEdit(MassSetFieldAction.massRenameField(entryList, fields[0], textFieldRename.getText(),
                        overwrite.isSelected()));
            }
        } else if (append.isSelected()) {
            for (String field : fields) {
                compoundEdit.addEdit(MassSetFieldAction.massAppendField(entryList, field, textFieldAppend.getText()));
            }
        } else {
            for (String field : fields) {
                compoundEdit.addEdit(MassSetFieldAction.massSetField(entryList, field,
                        set.isSelected() ? toSet : null,
                                overwrite.isSelected()));
            }
        }
        compoundEdit.end();
        bp.getUndoManager().addEdit(compoundEdit);
        bp.markBaseChanged();
    }

    /**
     * Set a given field to a given value for all entries in a Collection. This method DOES NOT update any UndoManager,
     * but returns a relevant CompoundEdit that should be registered by the caller.
     *
     * @param entries         The entries to set the field for.
     * @param field           The name of the field to set.
     * @param textToSet            The value to set. This value can be null, indicating that the field should be cleared.
     * @param overwriteValues Indicate whether the value should be set even if an entry already has the field set.
     * @return A CompoundEdit for the entire operation.
     */
    private static UndoableEdit massSetField(Collection<BibEntry> entries, String field, String textToSet,
            boolean overwriteValues) {

        NamedCompound compoundEdit = new NamedCompound(Localization.lang("Set field"));
        for (BibEntry entry : entries) {
            Optional<String> oldValue = entry.getField(field);
            // If we are not allowed to overwrite values, check if there is a
            // nonempty
            // value already for this entry:
            if (!overwriteValues && (oldValue.isPresent()) && !oldValue.get().isEmpty()) {
                continue;
            }
            if (textToSet == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, textToSet);
            }
            compoundEdit.addEdit(new UndoableFieldChange(entry, field, oldValue.orElse(null), textToSet));
        }
        compoundEdit.end();
        return compoundEdit;
    }

    /**
     * Append a given value to a given field for all entries in a Collection. This method DOES NOT update any UndoManager,
     * but returns a relevant CompoundEdit that should be registered by the caller.
     *
     * @param entries         The entries to process the operation for.
     * @param field           The name of the field to append to.
     * @param textToAppend            The value to set. A null in this case will simply preserve the current field state.
     * @return A CompoundEdit for the entire operation.
     */
    private static UndoableEdit massAppendField(Collection<BibEntry> entries, String field, String textToAppend) {

        String newValue = "";

        if (textToAppend != null) {
            newValue = textToAppend;
        }

        NamedCompound compoundEdit = new NamedCompound(Localization.lang("Append field"));
        for (BibEntry entry : entries) {
            Optional<String> oldValue = entry.getField(field);
            entry.setField(field, oldValue.orElse("") + newValue);
            compoundEdit.addEdit(new UndoableFieldChange(entry, field, oldValue.orElse(null), newValue));
        }
        compoundEdit.end();
        return compoundEdit;
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
        NamedCompound compoundEdit = new NamedCompound(Localization.lang("Rename field"));
        for (BibEntry entry : entries) {
            Optional<String> valToMove = entry.getField(field);
            // If there is no value, do nothing:
            if ((!valToMove.isPresent()) || valToMove.get().isEmpty()) {
                continue;
            }
            // If we are not allowed to overwrite values, check if there is a
            // non-empty value already for this entry for the new field:
            Optional<String> valInNewField = entry.getField(newField);
            if (!overwriteValues && (valInNewField.isPresent()) && !valInNewField.get().isEmpty()) {
                continue;
            }

            entry.setField(newField, valToMove.get());
            compoundEdit.addEdit(new UndoableFieldChange(entry, newField, valInNewField.orElse(null), valToMove.get()));
            entry.clearField(field);
            compoundEdit.addEdit(new UndoableFieldChange(entry, field, valToMove.get(), null));
        }
        compoundEdit.end();
        return compoundEdit;
    }

    private static String[] getFieldNames(String s) {
        return s.split("[\\s;,]");
    }
}
