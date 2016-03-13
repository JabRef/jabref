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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;

public class FieldFormatterCleanupsPanel extends JPanel {

    private static final String DESCRIPTION = Localization.lang("Description") + ": ";
    private final JCheckBox enabled;

    private FieldFormatterCleanups fieldFormatterCleanups;

    private JList actionsList;

    private JTextFieldWithUnfocusedText keyField;

    private JComboBox<String> formatters;

    private JButton addButton;

    private JLabel descriptionText;

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
                        "pref, 2dlu, pref, 2dlu, pref, 4dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref,"));
        builder.add(enabled).xyw(1, 1, 5);
        builder.add(getSelectorPanel()).xyw(3, 3, 5);

        List<FieldFormatterCleanup> actionsToDisplay = new ArrayList<>(configuredActions.size());
        for (FieldFormatterCleanup action : configuredActions) {
            actionsToDisplay.add(action);
        }

        descriptionText = new JLabel(DESCRIPTION);
        builder.add(descriptionText).xyw(3, 5, 5);

        actionsList = new JList(new SaveActionsListModel(actionsToDisplay));
        actionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        actionsList.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                super.mouseMoved(e);
                SaveActionsListModel m = (SaveActionsListModel) actionsList.getModel();
                int index = actionsList.locationToIndex(e.getPoint());
                if (index > -1) {
                    actionsList.setToolTipText(m.getElementAt(index).getDescription());
                }
            }
        });
        builder.add(actionsList).xyw(3, 7, 5);

        deleteButton = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getSmallIcon());
        deleteButton.addActionListener(new DeleteButtonListener());
        builder.add(deleteButton).xy(3, 9);

        resetButton = new JButton(Localization.lang("Reset"));
        resetButton.addActionListener(e -> ((SaveActionsListModel) actionsList.getModel()).reset(defaultFormatters));
        builder.add(resetButton).xy(5, 9);

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.WEST);

        // make sure the layout is set according to the checkbox
        enabled.addActionListener(new EnablementStatusListener(fieldFormatterCleanups.isEnabled()));
        enabled.setSelected(fieldFormatterCleanups.isEnabled());
    }

    private void updateDescription() {
        FieldFormatterCleanup formatterCleanup = getFieldFormatterCleanup();
        if(formatterCleanup != null) {
            descriptionText.setText(DESCRIPTION + formatterCleanup.getDescription());
        } else {
            Formatter selectedFormatter = getFieldFormatter();
            if(selectedFormatter != null) {
                // Create dummy FieldFormatterCleanup just for displaying the description
                FieldFormatterCleanup displayFormatterCleanup = new FieldFormatterCleanup(
                        Localization.lang("the given field"), selectedFormatter);
                descriptionText.setText(DESCRIPTION + displayFormatterCleanup.getDescription());
            } else {
                descriptionText.setText(DESCRIPTION);
            }
        }
    }

    private JPanel getSelectorPanel() {
        FormBuilder builder = FormBuilder.create().layout(new FormLayout("left:pref, 4dlu, left:pref, 4dlu, pref:grow",
                "pref, 2dlu,"));

        keyField = new JTextFieldWithUnfocusedText(Localization.lang("Enter field name (e.g., title, author)"));
        keyField.setColumns(25);
        keyField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateDescription();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateDescription();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateDescription();
            }
        });
        builder.add(keyField).xy(1, 1);

        List<String> formatterNames = fieldFormatterCleanups.getAvailableFormatters().stream().map(
                formatter -> formatter.getKey()).collect(Collectors.toList());
        List<String> formatterDescriptions = fieldFormatterCleanups.getAvailableFormatters().stream().map(
                formatter -> formatter.getDescription()).collect(Collectors.toList());
        formatters = new JComboBox(formatterNames.toArray());
        formatters.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                if (-1 < index && index < formatterDescriptions.size() && value != null) {
                    setToolTipText(String.format(formatterDescriptions.get(index), keyField.getText()));
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        formatters.addItemListener(e -> updateDescription());
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
            FieldFormatterCleanup newAction = getFieldFormatterCleanup();
            if (newAction == null) {
                return;
            }

            ((SaveActionsListModel) actionsList.getModel()).addSaveAction(newAction);
            keyField.setText("");
        }
    }

    private FieldFormatterCleanup getFieldFormatterCleanup() {
        Formatter selectedFormatter = getFieldFormatter();

        String fieldKey = keyField.getText();
        if ((fieldKey == null) || fieldKey.isEmpty()) {
            return null;
        }

        FieldFormatterCleanup newAction = new FieldFormatterCleanup(fieldKey, selectedFormatter);
        return newAction;
    }

    private Formatter getFieldFormatter() {
        Formatter selectedFormatter = null;
        String selectedFormatterKey = formatters.getSelectedItem().toString();
        for (Formatter formatter : fieldFormatterCleanups.getAvailableFormatters()) {
            if (formatter.getKey().equals(selectedFormatterKey)) {
                selectedFormatter = formatter;
                break;
            }
        }
        return selectedFormatter;
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
