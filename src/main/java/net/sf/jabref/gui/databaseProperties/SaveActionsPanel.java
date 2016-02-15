package net.sf.jabref.gui.databaseProperties;

import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.SaveAction;
import net.sf.jabref.exporter.SaveActions;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.List;

public class SaveActionsPanel extends JPanel {

    private JCheckBox enabled;

    private SaveActions saveActions;

    private JList actionsList;

    private JTextField keyField;

    private JComboBox<String> formatters;

    public SaveActionsPanel() {

        enabled = new JCheckBox(Localization.lang("Enable save actions"));
    }

    public void setValues(MetaData metaData) {
        // first clear existing content
        this.removeAll();

        saveActions = new SaveActions(metaData);
        List<FieldFormatterCleanup> configuredActions = saveActions.getConfiguredActions();

        enabled.setSelected(saveActions.isEnabled());

        this.setLayout(new GridLayout(2 + configuredActions.size(), 1));
        this.add(enabled);
        this.add(getSelectorPanel());

        List<FieldFormatterCleanup> actionsToDisplay = new ArrayList<>(configuredActions.size());
        for (FieldFormatterCleanup action : configuredActions) {
            actionsToDisplay.add(action);
        }

        actionsList = new JList(new SaveActionsListModel<>(actionsToDisplay));
        actionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.add(actionsList);

        JButton deleteButton = new JButton(Localization.lang("Delete"));
        deleteButton.addActionListener(new DeleteButtonListener());
        this.add(deleteButton);
    }

    private JPanel getSelectorPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, 3));

        keyField = new JTextField(20);
        panel.add(keyField);

        List<String> formatterNames = saveActions.getAvailableFormatters().stream().map(formatter -> formatter.getKey()).collect(Collectors.toList());
        formatters = new JComboBox(formatterNames.toArray());
        panel.add(formatters);

        JButton addButton = new JButton(Localization.lang("Add"));
        addButton.addActionListener(new AddButtonListener());
        panel.add(addButton);

        return panel;
    }

    public boolean storeSettings(MetaData metaData) {
        java.util.List<String> actions = new ArrayList<>();

        if (enabled.isSelected()) {
            actions.add("enabled");
        } else {
            actions.add("disabled");
        }

        List<FieldFormatterCleanup> newActions = ((SaveActionsListModel) actionsList.getModel()).getAllActions();
        for (FieldFormatterCleanup action : newActions) {
            actions.add(action.getField());
            actions.add(action.getFormatter().getKey());
        }

        metaData.putData(SaveActions.META_KEY, actions);

        boolean hasChanged = saveActions.equals(new SaveActions((metaData)));

        return hasChanged;
    }

    class AddButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            Formatter selectedFormatter = null;
            String selectedFormatterKey = formatters.getSelectedItem().toString();
            for (Formatter formatter : saveActions.getAvailableFormatters()) {
                if (formatter.getKey().equals(selectedFormatterKey)) {
                    selectedFormatter = formatter;
                    break;
                }
            }

            String fieldKey = keyField.getText();
            if (fieldKey == null || fieldKey.equals("")) {
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


}
