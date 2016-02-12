package net.sf.jabref.gui.databaseProperties;

import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.SaveAction;
import net.sf.jabref.exporter.SaveActions;
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

    private List<SaveAction> displayedActions;

    private JList<String> actionsList;

    private JTextField keyField;

    private JComboBox<String> formatters;

    private DatabasePropertiesDialog parent;

    public SaveActionsPanel(DatabasePropertiesDialog parent) {

        enabled = new JCheckBox(Localization.lang("Enable save actions"));
        displayedActions = new ArrayList<>();
        this.parent = parent;
    }

    public void setValues(MetaData metaData) {
        // first clear existing content
        this.removeAll();

        saveActions = new SaveActions(metaData);
        List<SaveAction> configuredActions = saveActions.getConfiguredActions();

        enabled.setSelected(saveActions.isEnabled());

        this.setLayout(new GridLayout(2 + configuredActions.size(), 1));
        this.add(enabled);
        this.add(getSelectorPanel());

        String[] data = new String[configuredActions.size()];
        for (int i = 0; i < configuredActions.size(); i++) {
            data[i] = configuredActions.get(i).toString();
        }
        actionsList = new JList<>(data);
        this.add(actionsList);
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
        addButton.addActionListener(new AddButtonListener(parent));
        panel.add(addButton);

        return panel;
    }

    public boolean storeSettings(MetaData metaData) {
        java.util.List<String> actions = new ArrayList<>();

        if (enabled.isSelected()) {
            actions.add("enabled;");
        } else {
            actions.add("disabled;");
        }

        metaData.putData(SaveActions.META_KEY, actions);

        boolean hasChanged = saveActions.equals(new SaveActions((metaData)));

        return hasChanged;
    }

    class AddButtonListener implements ActionListener {

        private DatabasePropertiesDialog container;

        public AddButtonListener(DatabasePropertiesDialog container) {
            this.container = container;
        }

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

            displayedActions.add(new SaveAction(keyField.getText(), selectedFormatter));
            keyField.setText("");
            container.repaint();
        }
    }

}
