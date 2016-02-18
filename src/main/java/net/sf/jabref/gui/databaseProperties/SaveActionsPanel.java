package net.sf.jabref.gui.databaseProperties;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.SaveActions;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;

public class SaveActionsPanel extends JPanel {

    private JCheckBox enabled;

    private SaveActions saveActions;

    private JList actionsList;

    private JTextField keyField;

    private JComboBox<String> formatters;

    private JButton addButton;

    private JButton deleteButton;

    public SaveActionsPanel() {

        enabled = new JCheckBox(Localization.lang("Enable save actions"));
    }

    public void setValues(MetaData metaData) {
        Objects.requireNonNull(metaData);

        // first clear existing content
        this.removeAll();

        boolean enablementStatus = metaData.getData(SaveActions.META_KEY).get(0).equals("enabled");
        String formatterString = metaData.getData(SaveActions.META_KEY).get(1);

        saveActions = new SaveActions(enablementStatus, formatterString);
        List<FieldFormatterCleanup> configuredActions = saveActions.getConfiguredActions();

        enabled.setSelected(saveActions.isEnabled());
        enabled.addActionListener(new EnablementStatusListener());

        FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, pref:grow",
                "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref,"));
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        builder.add(enabled).xy(3, 1);
        builder.add(getSelectorPanel()).xy(3, 3);

        List<FieldFormatterCleanup> actionsToDisplay = new ArrayList<>(configuredActions.size());
        for (FieldFormatterCleanup action : configuredActions) {
            actionsToDisplay.add(action);
        }

        actionsList = new JList(new SaveActionsListModel<>(actionsToDisplay));
        actionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        builder.add(actionsList).xyw(3, 5, 2);

        deleteButton = new JButton(Localization.lang("Delete"));
        deleteButton.addActionListener(new DeleteButtonListener());
        builder.add(deleteButton).xy(3, 7);

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.WEST);
    }

    private JPanel getSelectorPanel() {
        FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, pref:grow",
                "pref, 2dlu,"));

        keyField = new JTextField(20);
        keyField.setText("field name");
        keyField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                keyField.setText("");
            }

            @Override
            public void focusLost(FocusEvent e) {

            }
        });
        builder.add(keyField).xy(1, 1);

        List<String> formatterNames = saveActions.getAvailableFormatters().stream().map(formatter -> formatter.getKey()).collect(Collectors.toList());
        formatters = new JComboBox(formatterNames.toArray());
        builder.add(formatters).xy(3, 1);

        addButton = new JButton(Localization.lang("Add"));
        addButton.addActionListener(new AddButtonListener());
        builder.add(addButton).xy(5, 1);

        return builder.getPanel();
    }

    public boolean storeSettings(MetaData metaData) {
        Objects.requireNonNull(metaData);

        java.util.List<String> actions = new ArrayList<>();

        if (enabled.isSelected()) {
            actions.add("enabled");
        } else {
            actions.add("disabled");
        }

        List<FieldFormatterCleanup> newActions = ((SaveActionsListModel) actionsList.getModel()).getAllActions();
        String formatterString = SaveActions.getMetaDataString(newActions);
        actions.add(formatterString);

        metaData.putData(SaveActions.META_KEY, actions);

        boolean hasChanged = saveActions.equals(new SaveActions(enabled.isSelected(), formatterString));

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

    class EnablementStatusListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            boolean enablementStatus = enabled.isSelected();
            actionsList.setEnabled(enablementStatus);
            keyField.setEnabled(enablementStatus);
            formatters.setEnabled(enablementStatus);
            addButton.setEnabled(enablementStatus);
            deleteButton.setEnabled(enablementStatus);
        }
    }


}
