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
package net.sf.jabref.gui.dbproperties;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.MetaData;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.exporter.FieldFormatterCleanups;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;

public class FieldFormatterCleanupsPanel extends JPanel {

    private static final String DESCRIPTION = Localization.lang("Description") + ": ";
    private final JCheckBox enabled;
    private FieldFormatterCleanups fieldFormatterCleanups;
    private JList<?> actionsList;
    private JComboBox<?> formattersCombobox;
    private JComboBox<String> selectFieldCombobox;
    private JButton addButton;
    private JLabel descriptionText;
    private JButton deleteButton;
    private JButton resetButton;

    private final FieldFormatterCleanups defaultFormatters;


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
        //The copy is necessary becaue the original List is unmodifiable
        List<FieldFormatterCleanup> actionsToDisplay = new ArrayList<>(configuredActions);
        buildLayout(actionsToDisplay);

    }

    private void buildLayout(List<FieldFormatterCleanup> actionsToDisplay) {
        FormBuilder builder = FormBuilder.create()
                .layout(new FormLayout("left:pref, 13dlu, left:pref:grow, 4dlu, pref, 4dlu, pref",
                        "pref, 2dlu, pref, 2dlu, pref, 4dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref, 2dlu, pref,"));
        builder.add(enabled).xyw(1, 1, 7);
        builder.add(getSelectorPanel()).xyw(3, 3, 5);
        descriptionText = new JLabel(DESCRIPTION);
        builder.add(descriptionText).xyw(3, 5, 5);

        actionsList = new JList<>(new SaveActionsListModel(actionsToDisplay));
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

        resetButton = new JButton(Localization.lang("Reset"));
        resetButton.addActionListener(e -> ((SaveActionsListModel) actionsList.getModel()).reset(defaultFormatters));

        builder.add(resetButton).xy(3, 9);

        this.setLayout(new BorderLayout());
        this.add(builder.getPanel(), BorderLayout.WEST);

        // make sure the layout is set according to the checkbox
        enabled.addActionListener(new EnablementStatusListener(fieldFormatterCleanups.isEnabled()));
        enabled.setSelected(fieldFormatterCleanups.isEnabled());
    }

    private void updateDescription() {
        FieldFormatterCleanup formatterCleanup = getFieldFormatterCleanup();
        if (formatterCleanup != null) {
            descriptionText.setText(DESCRIPTION + formatterCleanup.getDescription());
        } else {
            Formatter selectedFormatter = getFieldFormatter();
            if (selectedFormatter != null) {
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
        FormBuilder builder = FormBuilder.create().layout(new FormLayout(
                "left:pref, 4dlu, left:pref, 4dlu, left:pref, 4dlu, pref:grow", "pref, 2dlu, pref:grow, 2dlu"));

        List<String> fieldNames = new ArrayList<>(InternalBibtexFields.getAllFieldNames());
        fieldNames.add(BibEntry.KEY_FIELD);
        Collections.sort(fieldNames);
        String[] allPlusKey = fieldNames.toArray(new String[fieldNames.size()]);
        selectFieldCombobox = new JComboBox<>(allPlusKey);
        selectFieldCombobox.setEditable(true);
        builder.add(selectFieldCombobox).xy(1, 1);

        List<String> formatterNames = fieldFormatterCleanups.getAvailableFormatters().stream()
                .map(formatter -> formatter.getKey()).collect(Collectors.toList());
        List<String> formatterDescriptions = fieldFormatterCleanups.getAvailableFormatters().stream()
                .map(formatter -> formatter.getDescription()).collect(Collectors.toList());
        formattersCombobox = new JComboBox<>(formatterNames.toArray());
        formattersCombobox.setRenderer(new DefaultListCellRenderer() {

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                if ((-1 < index) && (index < formatterDescriptions.size()) && (value != null)) {
                    setToolTipText(String.format(formatterDescriptions.get(index),
                            selectFieldCombobox.getSelectedItem().toString()));
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        formattersCombobox.addItemListener(e -> updateDescription());
        builder.add(formattersCombobox).xy(3, 1);

        addButton = new JButton(IconTheme.JabRefIcon.ADD_NOBOX.getSmallIcon());
        addButton.addActionListener(e -> {
            FieldFormatterCleanup newAction = getFieldFormatterCleanup();
            if (newAction == null) {
                return;
            }

            ((SaveActionsListModel) actionsList.getModel()).addSaveAction(newAction);

        });
        builder.add(addButton).xy(5, 1);

        deleteButton = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getSmallIcon());
        deleteButton.addActionListener(
                e -> ((SaveActionsListModel) actionsList.getModel()).removeAtIndex(actionsList.getSelectedIndex()));

        builder.add(deleteButton).xy(7, 1);

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

    private FieldFormatterCleanup getFieldFormatterCleanup() {
        Formatter selectedFormatter = getFieldFormatter();

        String fieldKey = selectFieldCombobox.getSelectedItem().toString();
        return new FieldFormatterCleanup(fieldKey, selectedFormatter);

    }

    private Formatter getFieldFormatter() {
        Formatter selectedFormatter = null;
        String selectedFormatterKey = formattersCombobox.getSelectedItem().toString();
        for (Formatter formatter : fieldFormatterCleanups.getAvailableFormatters()) {
            if (formatter.getKey().equals(selectedFormatterKey)) {
                selectedFormatter = formatter;
                break;
            }
        }
        return selectedFormatter;
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
            selectFieldCombobox.setEnabled(status);
            formattersCombobox.setEnabled(status);
            addButton.setEnabled(status);
            deleteButton.setEnabled(status);
            resetButton.setEnabled(status);
        }
    }

}
