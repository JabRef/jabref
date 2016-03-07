package net.sf.jabref.gui.dbproperties;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.util.component.JTextFieldWithUnfocusedText;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;

public class FieldFormatterCleanupsPanel extends JPanel {

    private final JCheckBox enabled;

    private FieldFormatterCleanups fieldFormatterCleanups;

    private JList actionsList;

    private JTextFieldWithUnfocusedText keyField;

    private JComboBox<String> formatters;

    private JButton addButton;

    private JButton deleteButton;

    private JButton resetButton;
    private FieldFormatterCleanups defaultFormatters;

    public FieldFormatterCleanupsPanel(String description, FieldFormatterCleanups defaultFormatters) {
        this.defaultFormatters = Objects.requireNonNull(defaultFormatters);

        enabled = new JCheckBox(description);
    }

    public void setValues(MetaData metaData) {
        Objects.requireNonNull(metaData);

        List<String> saveActionsMetaList = metaData.getData(MetaData.SAVE_ACTIONS);

        setValues(FieldFormatterCleanups.parseFromString(saveActionsMetaList));
    }

    public void setValues(FieldFormatterCleanups formatterCleanups) {
        fieldFormatterCleanups = formatterCleanups;

        // first clear existing content
        this.removeAll();

        List<FieldFormatterCleanup> configuredActions = fieldFormatterCleanups.getConfiguredActions();

        FormBuilder builder = FormBuilder.create().layout(
                new FormLayout("left:pref, 13dlu, left:pref, 4dlu, left:pref, 4dlu, pref:grow",
                        "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref,"));
        builder.add(enabled).xyw(1, 1, 5);
        builder.add(getSelectorPanel()).xyw(3, 3, 5);

        List<FieldFormatterCleanup> actionsToDisplay = new ArrayList<>(configuredActions.size());
        for (FieldFormatterCleanup action : configuredActions) {
            actionsToDisplay.add(action);
        }

        actionsList = new JList(new SaveActionsListModel(actionsToDisplay));
        actionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        builder.add(actionsList).xyw(3, 5, 5);

        deleteButton = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getSmallIcon());
        deleteButton.addActionListener(new DeleteButtonListener());
        builder.add(deleteButton).xy(3, 7);

        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> ((SaveActionsListModel) actionsList.getModel()).reset(defaultFormatters));
        builder.add(resetButton).xy(5, 7);

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.WEST);

        // make sure the layout is set according to the checkbox
        enabled.addActionListener(new EnablementStatusListener(fieldFormatterCleanups.isEnabled()));
        enabled.setSelected(fieldFormatterCleanups.isEnabled());
    }

    private JPanel getSelectorPanel() {
        FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, pref:grow",
                "pref, 2dlu,"));

        keyField = new JTextFieldWithUnfocusedText(Localization.lang("Enter field name (e.g., title, author)"));
        keyField.setColumns(25);
        builder.add(keyField).xy(1, 1);

        List<String> formatterNames = fieldFormatterCleanups.getAvailableFormatters().stream().map(formatter -> formatter.getKey()).collect(Collectors.toList());
        formatters = new JComboBox(formatterNames.toArray());
        builder.add(formatters).xy(3, 1);

        addButton = new JButton(IconTheme.JabRefIcon.ADD_NOBOX.getSmallIcon());
        addButton.addActionListener(new AddButtonListener());
        builder.add(addButton).xy(5, 1);

        return builder.getPanel();
    }

    public void storeSettings(MetaData metaData) {
        Objects.requireNonNull(metaData);

        FieldFormatterCleanups formatterCleanups = getFormatterCleanups();

        // if all actions have been removed, remove the save actions from the MetaData
        if (formatterCleanups.getConfiguredActions().isEmpty()) {
            metaData.remove(MetaData.SAVE_ACTIONS);
            return;
        }

        metaData.putData(MetaData.SAVE_ACTIONS, formatterCleanups.convertToString());
    }

    public FieldFormatterCleanups getFormatterCleanups() {
        List<FieldFormatterCleanup> actions = ((SaveActionsListModel) actionsList.getModel()).getAllActions();
        return new FieldFormatterCleanups(enabled.isSelected(), actions);
    }

    public boolean hasChanged() {
        return !fieldFormatterCleanups.equals(getFormatterCleanups());
    }

    public boolean isDefaultSaveActions() {
        return FieldFormatterCleanups.DEFAULT_SAVE_ACTIONS.equals(getFormatterCleanups());
    }

    class AddButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Formatter selectedFormatter = null;
            String selectedFormatterKey = formatters.getSelectedItem().toString();
            for (Formatter formatter : fieldFormatterCleanups.getAvailableFormatters()) {
                if (formatter.getKey().equals(selectedFormatterKey)) {
                    selectedFormatter = formatter;
                    break;
                }
            }

            String fieldKey = keyField.getText();
            if ((fieldKey == null) || fieldKey.isEmpty()) {
                return;
            }

            FieldFormatterCleanup newAction = new FieldFormatterCleanup(fieldKey, selectedFormatter);

            ((SaveActionsListModel) actionsList.getModel()).addSaveAction(newAction);
            keyField.setText("");
        }
    }

    class DeleteButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            ((SaveActionsListModel) actionsList.getModel()).removeAtIndex(actionsList.getSelectedIndex());
        }
    }

    class EnablementStatusListener implements ActionListener {

        public EnablementStatusListener(boolean initialStatus) {
         setStatus(initialStatus);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean enablementStatus = enabled.isSelected();
            setStatus(enablementStatus);

        }

        private void setStatus(boolean status) {
            actionsList.setEnabled(status);
            keyField.setEnabled(status);
            formatters.setEnabled(status);
            addButton.setEnabled(status);
            deleteButton.setEnabled(status);
            resetButton.setEnabled(status);
        }
    }


}
