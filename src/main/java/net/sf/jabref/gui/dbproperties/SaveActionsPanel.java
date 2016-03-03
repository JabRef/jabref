package net.sf.jabref.gui.dbproperties;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.SaveActions;
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

public class SaveActionsPanel extends JPanel {

    private final JCheckBox enabled;

    private SaveActions saveActions;

    private JList actionsList;

    private JTextFieldWithUnfocusedText keyField;

    private JComboBox<String> formatters;

    private JButton addButton;

    private JButton deleteButton;

    public SaveActionsPanel() {

        enabled = new JCheckBox(Localization.lang("Enable save actions"));
    }

    public void setValues(MetaData metaData) {
        Objects.requireNonNull(metaData);

        List<String> saveActionsMetaList = metaData.getData(SaveActions.META_KEY);

        initializeSaveActions(saveActionsMetaList);

        // first clear existing content
        this.removeAll();

        List<FieldFormatterCleanup> configuredActions = saveActions.getConfiguredActions();


        FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, pref:grow",
                "pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref,"));
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        builder.add(enabled).xy(3, 1);
        builder.add(getSelectorPanel()).xy(3, 3);

        List<FieldFormatterCleanup> actionsToDisplay = new ArrayList<>(configuredActions.size());
        actionsToDisplay.addAll(configuredActions);

        actionsList = new JList(new SaveActionsListModel<>(actionsToDisplay));
        actionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        builder.add(actionsList).xyw(3, 5, 2);

        deleteButton = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getSmallIcon());
        deleteButton.addActionListener(new DeleteButtonListener());
        builder.add(deleteButton).xy(3, 7);

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.WEST);

        // make sure the layout is set according to the checkbox
        enabled.addActionListener(new EnablementStatusListener(saveActions.isEnabled()));
        enabled.setSelected(saveActions.isEnabled());
    }

    private void initializeSaveActions(List<String> saveActionsMetaList) {

        if ((saveActionsMetaList != null) && (saveActionsMetaList.size() >= 2)) {
            boolean enablementStatus = "enabled".equals(saveActionsMetaList.get(0));
            String formatterString = saveActionsMetaList.get(1);
            saveActions = new SaveActions(enablementStatus, formatterString);
        } else {
            // apply default actions
            saveActions = SaveActions.DEFAULT_ACTIONS;
        }

    }

    private JPanel getSelectorPanel() {
        FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, pref:grow",
                "pref, 2dlu,"));

        keyField = new JTextFieldWithUnfocusedText(Localization.lang("Enter field name (e.g., title, author)"));
        keyField.setColumns(25);
        builder.add(keyField).xy(1, 1);

        List<String> formatterNames = saveActions.getAvailableFormatters().stream().map(formatter -> formatter.getKey()).collect(Collectors.toList());
        formatters = new JComboBox(formatterNames.toArray());
        builder.add(formatters).xy(3, 1);

        addButton = new JButton(IconTheme.JabRefIcon.ADD_NOBOX.getSmallIcon());
        addButton.addActionListener(new AddButtonListener());
        builder.add(addButton).xy(5, 1);

        return builder.getPanel();
    }

    public void storeSettings(MetaData metaData) {
        Objects.requireNonNull(metaData);

        List<String> actions = new ArrayList<>();

        if (enabled.isSelected()) {
            actions.add("enabled");
        } else {
            actions.add("disabled");
        }

        List<FieldFormatterCleanup> newActions = ((SaveActionsListModel) actionsList.getModel()).getAllActions();

        // if all actions have been removed, remove the save actions from the MetaData
        if (newActions.isEmpty()) {
            metaData.remove(SaveActions.META_KEY);
            return;
        }

        String formatterString = SaveActions.getMetaDataString(newActions);
        actions.add(formatterString);

        metaData.putData(SaveActions.META_KEY, actions);
    }

    public boolean hasChanged() {
        List<FieldFormatterCleanup> newActions = ((SaveActionsListModel) actionsList.getModel()).getAllActions();
        String formatterString = SaveActions.getMetaDataString(newActions);

        return !saveActions.equals(new SaveActions(enabled.isSelected(), formatterString));
    }

    public boolean isDefaultSaveActions() {
        List<FieldFormatterCleanup> newActions = ((SaveActionsListModel) actionsList.getModel()).getAllActions();
        String formatterString = SaveActions.getMetaDataString(newActions);

        return SaveActions.DEFAULT_ACTIONS.equals(new SaveActions(enabled.isSelected(), formatterString));
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
        }
    }


}
